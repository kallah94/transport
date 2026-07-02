# Déploiement personnalisable & multi‑tenant — Backend (API Spring Boot / MongoDB)

Ce document décrit comment faire évoluer l'API `com.gayale.transport` pour servir
**plusieurs clients** à partir d'un **seul code source**, selon deux modèles de
déploiement, et comment isoler les données dans le modèle partagé.

Décisions actées :
- **Deux canaux de livraison** :
  1. **Electron desktop** avec le **JAR `transport-api` embarqué** (+ MongoDB local) → modèle **dédié, mono‑tenant, hors‑ligne**.
  2. **Déploiement web** → modèle **partagé multi‑tenant** par sous‑domaine.
- Instance partagée → **isolation totale multi‑tenant** (chaque transporteur ne voit que ses données).
- Identification du client → **par sous‑domaine** (`clientA.app.com`).
- Branding → **fichier de configuration monté par déploiement** (pas de rebuild).
- Ordre → **Phase 1 : branding + déploiements dédiés**, puis **Phase 2 : multi‑tenant**.

---

## 1. Deux modèles, un seul code

| | Déploiement **dédié** (par client) | Déploiement **partagé** (SaaS) |
|---|---|---|
| Instances | 1 par client | 1 pour tous |
| Base Mongo | 1 base par client | 1 base, données taguées `tenantId` |
| Isolation des données | par l'infrastructure (bases séparées) | par le code (`tenantId` + filtrage) |
| Tenants en base | 1 (semé au démarrage) | N (auto‑inscription) |
| Canal | **Electron + JAR embarqué** | **Web** |
| Branding | fichier de config du déploiement | par tenant (fichier seedé → champ `Tenant`) |

Le pilote est une variable d'environnement :

```yaml
# application.yml
app:
  mode: ${APP_MODE:dedicated}     # dedicated | shared
  tenant:
    default-key: ${TENANT_KEY:default}   # utilisé en mode dedicated
```

En mode `dedicated`, le `tenantId` est constant (résolu une fois au démarrage) ;
le filtrage multi‑tenant devient un no‑op fonctionnel mais le code reste identique.

---

## 2. Phase 1 — Externalisation de la config (prérequis, faible risque)

Aucune logique métier ; on rend l'API déployable sans recompiler.

1. **Tout secret/URL en variables d'environnement** (déjà fait pour `JWT_SECRET`) :
   `MONGO_URI`, `JWT_SECRET`, `APP_MODE`, `TENANT_KEY`, `CORS_ALLOWED_ORIGINS`.
2. **Profils Spring** : `application.yml` (commun) + `application-dedicated.yml` /
   `application-shared.yml`. Lancement : `SPRING_PROFILES_ACTIVE=shared`.
3. **CORS dynamique** : autoriser le(s) sous‑domaine(s) via `CORS_ALLOWED_ORIGINS`
   (wildcard `*.app.com` géré par une `CorsConfigurationSource` basée sur regex).
4. **Endpoint branding** (lecture seule, public) pour servir le branding au front du
   déploiement partagé en Phase 2 :
   `GET /api/branding?host=clientA.app.com` → `{ key, title, logoUrl, theme{...} }`.
   En mode dédié, le front lit son propre fichier ; cet endpoint n'est requis qu'en partagé.

Livrable Phase 1 : la **même image** se déploie pour chaque client dédié, configurée
uniquement par variables d'env + sa propre base Mongo.

---

## 3. Phase 2 — Multi‑tenant à isolation totale

### 3.1 Modèle Tenant

```java
@Document(collection = "tenants")
@Data
public class Tenant {
    @Id private String id;
    @Indexed(unique = true) private String key;   // = sous‑domaine (clientA)
    private String title;                          // « Gayale Transport »
    private String logoUrl;
    private Theme theme;                           // primary/secondary/accent...
    private boolean active = true;
}
```

### 3.2 Le `tenantId` sur UNE seule classe

Presque toutes les entités étendent déjà `util/AuditableEntity`
(`Project, Truck, PurchaseOrder, WeightTicket, TransporterEnterprise, User,
TransporterRate, DriverRate, FuelConfig, PaymentStatement, DriverPaymentStatement`).
On ajoute le champ **une fois** :

```java
public abstract class AuditableEntity {
    @Indexed
    private String tenantId;   // <-- ajouté ici => hérité partout
    // ... champs d'audit existants
}
```

### 3.3 Contexte de tenant par requête (résolu au sous‑domaine)

```java
public final class TenantContext {
    private static final ThreadLocal<String> CURRENT = new ThreadLocal<>();
    public static void set(String t){ CURRENT.set(t); }
    public static String get(){ return CURRENT.get(); }
    public static void clear(){ CURRENT.remove(); }
}
```

Un **filtre** (avant la chaîne de sécurité) résout le tenant :
- `APP_MODE=dedicated` → `TenantContext.set(TENANT_KEY)`.
- `APP_MODE=shared` → extraire le sous‑domaine de l'en‑tête `Host`
  (`clientA.app.com` → `clientA`), valider qu'un `Tenant.active` existe, sinon 404.
- **Sécurité** : le JWT embarque aussi `tenantId` ; on **rejette** toute requête où le
  tenant du token ≠ tenant du sous‑domaine (anti‑escalade inter‑tenant).

```java
@Component @Order(Ordered.HIGHEST_PRECEDENCE)
public class TenantFilter extends OncePerRequestFilter {
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain) {
        try {
            String key = mode.isDedicated() ? defaultKey : subdomainOf(req.getServerName());
            TenantContext.set(tenantService.resolveId(key));
            chain.doFilter(req, res);
        } finally { TenantContext.clear(); }
    }
}
```

### 3.4 Écriture : tag automatique du `tenantId`

Listener Mongo qui pose le tenant courant avant insertion (comme l'audit) :

```java
@Component
public class TenantMongoListener extends AbstractMongoEventListener<AuditableEntity> {
    @Override public void onBeforeConvert(BeforeConvertEvent<AuditableEntity> e) {
        var ent = e.getSource();
        if (ent.getTenantId() == null) ent.setTenantId(TenantContext.get());
    }
}
```

### 3.5 Lecture : filtrage automatique (le point critique)

Trois options, par ordre de robustesse :

1. **Recommandé — intercepter `MongoTemplate`** : encapsuler les lectures pour
   ajouter `Criteria.where("tenantId").is(TenantContext.get())` à chaque `Query`.
   Centralise l'isolation, indépendant des repositories.
2. **Service de base `TenantAwareService<T>`** : toutes les méthodes `findAll/findById`
   passent par des requêtes scoping `tenantId`. Simple mais à appliquer partout.
3. **À éviter** : se reposer sur le filtrage manuel dans chaque service (oubli = fuite).

> Test de non‑régression **obligatoire** : un utilisateur du tenant A ne doit JAMAIS
> lire/modifier une entité du tenant B (voir §6).

### 3.6 Unicité & index par tenant

Les contraintes d'unicité deviennent **composites** :
- `Truck.vehicle` → unique `{tenantId, vehicle}`.
- `User.email` → unique `{tenantId, email}` (un même email peut exister chez 2 clients).
- `PurchaseOrder.orderNumber` → unique `{tenantId, orderNumber}`.
- Index de performance : préfixer par `tenantId` (`{tenantId, status}`, etc.).

### 3.7 Auto‑inscription (instance partagée)

Flux `POST /api/signup` (public, mode shared) :
1. Créer le `Tenant` (`key` = sous‑domaine demandé, vérifié libre).
2. Créer le `User` admin lié à ce `tenantId` (rôle ADMIN du tenant).
3. Semer les données par défaut du tenant (rôles, config carburant vide…).
4. Retourner l'URL `https://<key>.app.com`.

Les rôles existants (ADMIN/USER/GUEST) restent **internes à un tenant** ; prévoir un
rôle plateforme `SUPER_ADMIN` (hors tenant) pour l'exploitation.

---

## 4. Recettes de déploiement

### 4.0 Electron — JAR `transport-api` embarqué (canal dédié)

Le desktop est déjà câblé pour ça : `electron-builder` copie le JAR via
`build.extraResources` (`from: server`, filtres `*.jar`, `*.yml`, `*.properties`),
et le front lit l'URL du serveur local par IPC
(`window.electron.server.getStatus()` dans `api.service.ts`) — donc **pas d'`apiUrl`
à coder en dur**.

Le **process principal Electron** lance le JAR en mode dédié, sur un port libre :

```js
// electron main : démarrage du backend embarqué
const jar = path.join(process.resourcesPath, 'server', 'transport-api.jar');
serverProcess = spawn('java', [
  '-jar', jar,
  '--server.port=0',                       // port libre, lu ensuite dans les logs
  '--spring.profiles.active=dedicated',
  '--app.mode=dedicated',
  `--app.tenant.default-key=${CLIENT_KEY}`,// ex: clientA
  `--spring.data.mongodb.uri=${MONGO_URI}` // mongodb://localhost:27017/clientA
]);
// puis exposer l'URL résolue au renderer via getStatus()
```

Points d'attention propres au desktop :
- **MongoDB local** : soit MongoDB Community installé sur le poste (déjà fait en dev),
  soit `mongod` bundlé dans `extraResources` et démarré par Electron. À trancher
  (cf. §7) selon que le client a droit ou non d'installer Mongo.
- **JRE** : embarquer un JRE (jlink) dans `extraResources` pour ne pas dépendre d'un
  `java` système.
- **Mono‑tenant** : `app.mode=dedicated` ⇒ le filtrage multi‑tenant est un no‑op
  (`tenantId = CLIENT_KEY` constant). Le même code que le web, sans isolation à risque.
- **Hors‑ligne** : tout est local (front + JAR + Mongo) ; aucune dépendance réseau.

### 4.1 Web partagé (une instance, N tenants par sous‑domaine)

L'API se déploie comme aujourd'hui (PaaS type Render, ou un simple `java -jar` sur un
serveur), pilotée **uniquement par variables d'environnement** :

```
APP_MODE=shared
MONGO_URI=...                       # une base, données taguées tenantId
JWT_SECRET=...
CORS_ALLOWED_ORIGINS=https://*.app.com
```

Exigences côté hébergement :
- **Sous‑domaine wildcard** `*.app.com` pointant vers l'instance (certificat TLS wildcard).
- L'en‑tête **`Host`** doit arriver intact jusqu'à l'API : le `TenantFilter` en déduit le
  tenant. Sur un PaaS c'est le cas par défaut ; derrière un proxy, penser à transmettre `Host`.

---

## 5. Plan de migration (ordre conseillé)

1. Phase 1 : externaliser config + profils + endpoint `/api/branding` + CORS dynamique.
2. Ajouter `Tenant` + `tenantId` sur `AuditableEntity` (+ index composites).
3. `TenantContext` + `TenantFilter` + JWT enrichi du `tenantId`.
4. Tag écriture (`TenantMongoListener`).
5. Filtrage lecture via `MongoTemplate` (option 1).
6. Auto‑inscription + `SUPER_ADMIN`.
7. Script de **backfill** : poser `tenantId = default` sur les données du client historique.

---

## 6. Sécurité & tests (non négociable)

- Tests d'**isolation croisée** : pour chaque endpoint, token tenant A vs ressource
  tenant B → 404/403, jamais 200.
- Vérifier que `tenantId` n'est **jamais** accepté depuis le corps de requête (toujours
  dérivé du contexte serveur).
- Rejet si `Host` inconnu / tenant inactif.
- Ne jamais logguer de données d'un tenant dans le contexte d'un autre.
- `JWT_SECRET` et `MONGO_URI` uniquement par variables d'env ; jamais committés.

---

## 7. Hors périmètre / à décider plus t