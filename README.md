# Harvest Hub - User Service

[![Java](https://img.shields.io/badge/Java-17-orange.svg?logo=openjdk)](https://openjdk.java.net/) [![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.5+-6DB33F.svg?logo=spring)](https://spring.io/projects/spring-boot) [![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-336791.svg?logo=postgresql)](https://www.postgresql.org/) [![AWS Cognito](https://img.shields.io/badge/AWS_Cognito-Cognito-FF9900.svg?logo=amazon-aws)](https://aws.amazon.com/cognito/) [![Maven](https://img.shields.io/badge/Maven-4-C71A36.svg?logo=apache-maven)](https://maven.apache.org/)

This repository contains the source code for the User Service, a core backend microservice for the **Harvest Hub** eCommerce platform.

## 1. Overview

The User Service is responsible for all user management, authentication integration, and application-specific user data. It acts as a secure facade for **AWS Cognito**, handling user profiles, role management, and administration, while also managing user address data in a local **PostgreSQL** database.

This service is built with **Java 17**, **Spring Boot 3.5+**, and **Maven**.

## 2. Core Features

### User-Facing Features
- **Get My Profile:** Fetches the authenticated user's full profile from AWS Cognito.
- **Address Management (CRUD):**
  - Add one or more shipping addresses.
  - View all personal shipping addresses.
  - Update an existing address.
  - **Soft Delete** an address (marks it as inactive for data integrity).

### Admin-Facing Features (`SuperAdmin` Role)
- **User Listing:** Get a paginated list of all users in the system.
- **User Creation:** Create new administrative users (`Supplier`, `DataSteward`) and send email invitations.
- **Role Management:** Set and synchronize a user's roles (e.g., promote a `Customer` to a `Supplier`). Multi-role capable.
- **Status Management:** Enable or disable user accounts.
- **Security:** Built-in safeguards prevent admins from disabling or modifying the roles of other `SuperAdmin` users.

## 3. Technology Stack

- **Framework:** [Spring Boot](https://spring.io/projects/spring-boot)
- **Language:** [Java 17](https://www.oracle.com/java/)
- **Build Tool:** [Maven](https://maven.apache.org/)
- **Authentication:** [Spring Security (OAuth2 Resource Server)](https://spring.io/projects/spring-security)
- **Identity Provider:** [AWS Cognito](https://aws.amazon.com/cognito/)
- **Database:** [PostgreSQL](https://www.postgresql.org/)
- **Data Access:** [Spring Data JPA / Hibernate](https://spring.io/projects/spring-data-jpa)
- **API Documentation:** [(Postman)](https://www.postman.com/)

## 4. Setup and Configuration

### Prerequisites
- Java JDK 17 or higher
- Apache Maven 4
- PostgreSQL Server
- An AWS Account with a configured Cognito User Pool
- AWS CLI installed and configured with credentials

### Configuration
1.  **Clone the repository:**
    ```bash
    git clone <your-repo-url>
    cd user-service
    ```
2.  **Create the Database:**
    - Log in to your PostgreSQL instance.
    - Create a new database named `user_db`.
    ```sql
    CREATE DATABASE user_db;
    ```
3.  **Configure Local AWS Credentials:**
    - Run `aws configure` in your terminal and provide the Access Key and Secret Key for an IAM user with `AmazonCognitoPowerUser` permissions.
    ```bash
    aws configure
    AWS Access Key ID [None]: YOUR_ACCESS_KEY
    AWS Secret Access Key [None]: YOUR_SECRET_KEY
    Default region name [None]: ap-southeast-2
    Default output format [None]: json
    ```
4.  **Update `application.properties`:**
    - Open `src/main/resources/application.properties`.
    - Verify that the database credentials are correct for your local setup.
    - Update the `spring.security.oauth2.resourceserver.jwt.jwk-set-uri` and `aws.cognito.userPoolId` properties with your Cognito User Pool details.



You can run the service from your IDE (like IntelliJ or VS Code) by running the `UserServiceApplication.java` class.

Alternatively, you can build and run it using Maven:

```bash
# Build the project and create the JAR file
mvn clean install
```

 ```bash
# Run the application
java -jar target/user-service-0.0.1-SNAPSHOT.jar
```
## 6. API Documentation

- **Postman:** [Harvest Hub](https://www.postman.com/xd5555-3122/workspace/harvest-hub-apis)

## 7. AWS Configs

- **AWS:** [Documentation](https://github.com/AshanHimantha/Harvest-Hub-user-service/blob/master/AWS_Configs.md)

