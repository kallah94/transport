# Gayale Transport Backend

Backend API pour la gestion du transport de gravier, développé avec Spring Boot et MongoDB.

## À propos du projet

Le projet "Gayale Transport - Gestion de Transport de Gravier" est une application complète destinée à gérer les opérations logistiques d'une entreprise de transport de matériaux de construction, spécifiquement du gravier. Le backend permet le suivi des tickets de pesée, la gestion des projets et des bons de commande, ainsi que la génération de statistiques et rapports.

## Technologies utilisées

- **Framework**: Spring Boot 3.2.0
- **Base de données**: MongoDB
- **Authentification**: JWT (JSON Web Tokens)
- **Documentation API**: SpringDoc OpenAPI 3 (Swagger)
- **Validation**: Jakarta Bean Validation
- **Génération de QR Code**: ZXing
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

4. **Gestion des tickets de pesée**
    - Enregistrement des poids à vide et chargé
    - Calcul automatique du poids net
    - Génération de codes QR pour les tickets

5. **Statistiques et rapports**
    - Statistiques par période (jour, semaine, mois)
    - Statistiques par entité (projet, BC, transporteur, véhicule)

## Modèle de données

### Utilisateurs (User)
- Informations d'identification, nom complet, email, rôle

### Projets (Project)
- Nom, client, destination, dates, statut, tonnage total livré

### Bons de commande (PurchaseOrder)
- Numéro de commande, fournisseur, transporteur, quantités

### Tickets de pesée (WeightTicket)
- Numéro de ticket, date, poids, véhicule, chauffeur, produit, client, etc.

## Tests

Exécution des tests :
```bash
mvn test
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

## Frontend compatible

Ce backend est conçu pour fonctionner avec une application frontend développée en Angular/Electron. Les modèles et endpoints ont été spécifiquement conçus pour être compatibles avec ce frontend.

## Contributions

1. Fork du projet
2. Création d'une branche (`git checkout -b feature/nouvelle-fonctionnalite`)
3. Commit de vos changements (`git commit -m 'Ajout de nouvelle fonctionnalité'`)
4. Push vers la branche (`git push origin feature/nouvelle-fonctionnalite`)
5. Création d'une Pull Request

## Licence

Ce projet est sous licence propriétaire. © Gayale Transport.

## Contact

Pour toute question ou information complémentaire, veuillez contacter l'équipe support à support@gayaletransport.com