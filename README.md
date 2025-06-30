# Gayel Transport Backend

Backend API pour la gestion du transport de gravier, développé avec Spring Boot et MongoDB.

## À propos du projet

Le projet "Gayel Transport - Gestion de Transport de Gravier" est une application complète destinée à gérer les opérations logistiques d'une entreprise de transport de matériaux de construction, spécifiquement du gravier. Le backend permet le suivi des tickets de pesée, la gestion des projets et des bons de commande, la gestion des entreprises de transport, ainsi que la génération de statistiques et rapports.

## Technologies utilisées

- **Framework**: Spring Boot 3.2.0
- **Base de données**: MongoDB
- **Authentification**: JWT (JSON Web Tokens)
- **Documentation API**: SpringDoc OpenAPI 3 (Swagger)
- **Validation**: Jakarta Bean Validation
- **Génération de QR Code**: ZXing
- **Mapping DTO/Entité**: ModelMapper
- **Build Tool**: Maven

## Prérequis

- JDK 17 ou plus récent
- Maven 3.6+
- MongoDB 6.0+

## Installation et démarrage

### Configuration de la base de données MongoDB

1. Installez et démarrez MongoDB sur votre machine ou utilisez une instance cloud
2. Créez une base de données nommée `gayale_transport_dev` (pour le développement)

### Configuration de l'application

Les configurations se trouvent dans les fichiers suivants :

- `src/main/resources/application.yml` - Configuration principale
- `src/main/resources/application-dev.yml` - Configuration de développement
- `src/main/resources/application-prod.yml` - Configuration de production
- `src/main/resources/application-test.yml` - Configuration de test

Adaptez ces fichiers selon votre environnement.

### Exécution de l'application

1. Clonez le dépôt

  ```bash
  git clone https://github.com/votre-organisation/gayale-transport-backend.git
  cd gayale-transport-backend
  ```

2. Construisez l'application

  ```bash
  mvn clean package
  ```

3. Exécutez l'application

  ```bash
  java -jar target/gayale-transport-backend-0.0.1-SNAPSHOT.jar
  ```


Par défaut, l'application démarrera sur le port 8080 avec le contexte `/api`.

## Structure du projet

```
gayale-transport-backend/
├── src/
│   ├── main/
│   │   ├── java/com/gayaletransport/
│   │   │   ├── config/       # Configuration Spring Boot
│   │   │   ├── controller/   # Contrôleurs REST
│   │   │   ├── dto/          # Objets de transfert de données
│   │   │   ├── exception/    # Gestion des exceptions
│   │   │   ├── model/        # Entités de domaine
│   │   │   ├── repository/   # Accès aux données MongoDB
│   │   │   ├── security/     # Configuration de sécurité et JWT
│   │   │   ├── service/      # Logique métier
│   │   │   └── util/         # Classes utilitaires
│   │   └── resources/        # Fichiers de configuration
│   └── test/                 # Tests unitaires et d'intégration
```

## API Endpoints

L'API est documentée avec Swagger. Une fois l'application démarrée, accédez à :

```
http://localhost:8080/api/swagger-ui.html
```

### Principaux endpoints

- **Authentification**: `/api/auth/login`, `/api/auth/refresh-token`
- **Utilisateurs**: `/api/users`
- **Projets**: `/api/projects`
- **Bons de commande**: `/api/purchase-orders`
- **Tickets de pesée**: `/api/tickets`
- **Entreprises de transport**: `/api/transporter-enterprises`
- **Statistiques**: `/api/statistics`

## Sécurité

L'API utilise JWT pour l'authentification. Chaque requête (sauf login et refresh token) doit inclure un en-tête d'autorisation :

```
Authorization: Bearer <token>
```

Les rôles suivants sont définis :

- **ADMIN**: Accès complet à toutes les fonctionnalités
- **AGENT**: Création et modification de projets, bons de commande et tickets
- **GUEST**: Lecture seule

## Fonctionnalités principales

1. **Gestion des utilisateurs**

- Création, modification et suppression d'utilisateurs
- Attribution des rôles et droits d'accès
2. **Gestion des projets**

- Suivi des projets de livraison et leur avancement
- Calcul automatique du tonnage total livré
3. **Gestion des bons de commande**

- Suivi des contrats de livraison avec pourcentage de réalisation
- Alerte automatique pour les BC presque terminés
4. **Gestion des entreprises de transport**

- Enregistrement des entreprises de transport et de leurs flottes
- Suivi des représentants et des capacités de transport
- Gestion des informations de contact et d'immatriculation
5. **Gestion des tickets de pesée**

- Enregistrement des poids à vide et chargé
- Calcul automatique du poids net
- Génération de codes QR pour les tickets
6. **Statistiques et rapports**

- Statistiques par période (jour, semaine, mois)
- Statistiques par entité (projet, BC, transporteur, véhicule)

## Modèle de données

### Utilisateurs (User)

- Informations d'identification, nom complet, email, rôle

### Projets (Project)

- Nom, client, destination, dates, statut, tonnage total livré

### Bons de commande (PurchaseOrder)

- Numéro de commande, fournisseur, transporteur, quantités

### Entreprises de transport (TransporterEnterprise)

- Nom, adresse, représentant, nombre de camions, informations de contact

### Tickets de pesée (WeightTicket)

- Numéro de ticket, date, poids, véhicule, chauffeur, produit, client, etc.

## Module Entreprises de Transport

### Vue d'ensemble

Ce module permet la gestion complète des entreprises de transport partenaires, incluant leurs flottes de véhicules et leurs représentants.

### Structure de données

```json
{
  "id": "string",
  "name": "string (obligatoire, unique)",
  "address": "string (obligatoire)",
  "representative": {
    "id": "string",
    "username": "string",
    "fullName": "string", 
    "email": "string"
  },
  "numberOfTrucks": "integer (≥ 0)",
  "phone": "string (optionnel)",
  "email": "string (optionnel, format email)",
  "registrationNumber": "string (optionnel, unique)",
  "active": "boolean",
  "createdAt": "datetime",
  "updatedAt": "datetime"
}
```

### Endpoints disponibles

#### Gestion CRUD

- **POST** `/api/transporter-enterprises` - Créer une entreprise (ADMIN, AGENT)
- **GET** `/api/transporter-enterprises` - Liste paginée (tous rôles)
- **GET** `/api/transporter-enterprises/{id}` - Détails d'une entreprise (tous rôles)
- **PUT** `/api/transporter-enterprises/{id}` - Modifier une entreprise (ADMIN, AGENT)
- **DELETE** `/api/transporter-enterprises/{id}` - Supprimer définitivement (ADMIN)

#### Fonctionnalités avancées

- **GET** `/api/transporter-enterprises/active` - Entreprises actives uniquement
- **GET** `/api/transporter-enterprises/search?keyword=...` - Recherche textuelle
- **GET** `/api/transporter-enterprises/statistics` - Statistiques des transporteurs
- **PATCH** `/api/transporter-enterprises/{id}/deactivate` - Désactiver (ADMIN)
- **PATCH** `/api/transporter-enterprises/{id}/reactivate` - Réactiver (ADMIN)

### Paramètres de requête

- `page` : Numéro de page (défaut: 0)
- `size` : Taille de page (défaut: 20)
- `sortBy` : Champ de tri (défaut: name)
- `sortDir` : Direction (asc/desc, défaut: asc)
- `keyword` : Terme de recherche pour `/search`

### Exemples d'utilisation

#### Créer une entreprise de transport

```bash
POST /api/transporter-enterprises
Content-Type: application/json
Authorization: Bearer <token>

{
  "name": "Transport Dakar Express",
  "address": "Zone Industrielle, Dakar, Sénégal",
  "representativeId": "user123",
  "numberOfTrucks": 15,
  "phone": "+221701234567",
  "email": "contact@dakarexpress.sn",
  "registrationNumber": "REG789",
  "active": true
}
```

#### Rechercher des entreprises

```bash
GET /api/transporter-enterprises/search?keyword=dakar&page=0&size=10
Authorization: Bearer <token>
```

#### Obtenir les statistiques

```bash
GET /api/transporter-enterprises/statistics
Authorization: Bearer <token>
```

Réponse :

```json
{
  "totalTransporters": 25,
  "activeTransporters": 22,
  "totalTrucks": 150,
  "recentlyCreated": 3
}
```

### Validation des données

- **Nom** : Obligatoire, unique, 2-100 caractères
- **Adresse** : Obligatoire, maximum 255 caractères
- **Représentant** : Obligatoire, doit exister dans la collection User
- **Nombre de camions** : Entier positif ou zéro
- **Email** : Format email valide (si fourni)
- **Numéro d'immatriculation** : Unique (si fourni)

### Fonctionnalités

- **Recherche textuelle** : Dans nom, adresse et numéro d'immatriculation
- **Soft delete** : Désactivation au lieu de suppression définitive
- **Audit automatique** : Dates de création et modification
- **Statistiques** : Compteurs et métriques en temps réel
- **Validation métier** : Vérification des doublons et contraintes

## Tests

Exécution des tests :

```bash
mvn test
```

### Tests spécifiques aux entreprises de transport

```bash
mvn test -Dtest=TransporterEnterpriseServiceTest
```

## Déploiement en production

1. Configurez les variables d'environnement de production

  ```
  MONGODB_URI=mongodb://user:password@host:port/database
  JWT_SECRET=votre_clé_secrète_très_longue_et_aléatoire
  APP_URL=https://votre-domaine.com
  ```

2. Construisez avec le profil de production

  ```bash
  mvn clean package -Pprod
  ```

3. Déployez le fichier JAR

  ```bash
  java -jar -Dspring.profiles.active=prod target/gayale-transport-backend-0.0.1-SNAPSHOT.jar
  ```


## Optimisations et index MongoDB

### Index recommandés pour les entreprises de transport

```javascript
// Index unique sur le nom
db.transporter_enterprises.createIndex({"name": 1}, {"unique": true})

// Index sur le statut actif
db.transporter_enterprises.createIndex({"active": 1})

// Index de recherche textuelle
db.transporter_enterprises.createIndex({
  "name": "text",
  "address": "text", 
  "registrationNumber": "text"
})

// Index sur la date de création pour les statistiques
db.transporter_enterprises.createIndex({"created_at": -1})

// Index sur le représentant pour les requêtes
db.transporter_enterprises.createIndex({"representative.$id": 1})
```

## Frontend compatible

Ce backend est conçu pour fonctionner avec une application frontend développée en Angular/Electron. Les modèles et endpoints ont été spécifiquement conçus pour être compatibles avec ce frontend.

## Architecture et design patterns

### Patterns utilisés

- **Repository Pattern** : Séparation de la logique d'accès aux données
- **Service Layer** : Encapsulation de la logique métier
- **DTO Pattern** : Transfert de données optimisé entre couches
- **Builder Pattern** : Construction d'objets complexes (via ModelMapper)

### Principes SOLID

- **Single Responsibility** : Chaque classe a une responsabilité unique
- **Open/Closed** : Extensions possibles sans modification du code existant
- **Dependency Inversion** : Dépendances via interfaces et injection

## Monitoring et logs

### Logs applicatifs

Les logs sont configurés avec Logback et incluent :

- Requêtes HTTP entrantes
- Erreurs et exceptions
- Opérations de base de données
- Événements d'authentification

### Métriques

- Nombre de requêtes par endpoint
- Temps de réponse moyens
- Taux d'erreur par module
- Utilisation des ressources

## Contributions

1. Fork du projet
2. Création d'une branche (`git checkout -b feature/nouvelle-fonctionnalite`)
3. Commit de vos changements (`git commit -m 'Ajout de nouvelle fonctionnalité'`)
4. Push vers la branche (`git push origin feature/nouvelle-fonctionnalite`)
5. Création d'une Pull Request

### Conventions de code

- **Java** : Google Java Style Guide
- **Tests** : Coverage minimum de 80%
- **Documentation** : Javadoc pour toutes les méthodes publiques
- **Git** : Messages de commit en français, descriptifs

## Roadmap

### Version 2.0 (Prochaine)

- [ ] Module de facturation automatique
- [ ] Intégration avec APIs de géolocalisation
- [ ] Notifications temps réel via WebSocket
- [ ] Tableau de bord analytics avancé
- [ ] Export/Import Excel/CSV
- [ ] API mobile dédiée

### Version 1.1 (En cours)

- [x] Module entreprises de transport
- [x] Audit automatique des entités
- [x] Validation avancée des données
- [ ] Module de gestion des véhicules
- [ ] Planification des livraisons

## Licence

Ce projet est sous licence propriétaire. © Gayel Transport.

## Contact

Pour toute question ou information complémentaire, veuillez contacter l'équipe support à support@gayaletransport.com

---

### Changelog

#### Version 1.0.1 - 2025-06-01

- ✅ Ajout du module Entreprises de Transport
- ✅ Implémentation de ModelMapper pour les conversions
- ✅ Configuration de l'audit automatique MongoDB
- ✅ Gestion des exceptions centralisée
- ✅ Tests unitaires complets
- ✅ Documentation API Swagger enrichie

#### Version 1.0.0 - 2025-01-01

- ✅ Release initiale
- ✅ Modules User, Project, PurchaseOrder, WeightTicket
- ✅ Authentification JWT
- ✅ API REST complète