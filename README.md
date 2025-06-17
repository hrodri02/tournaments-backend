# Tournaments Backend

This is a Spring Boot application that provides a RESTful API for managing tournaments, teams, leagues, and users.

## Getting Started

### Prerequisites

- Java 23
- Maven
- PostgreSQL

### Installation

1. Clone the repository
2. Configure your PostgreSQL database in `application.properties`
3. Run the application using Maven:

```bash
./mvnw spring-boot:run
```

## API Documentation

The API documentation is available through OpenAPI (Swagger UI). You can access it at:

```
http://localhost:8080/swagger-ui.html
```

The OpenAPI JSON specification is available at:

```
http://localhost:8080/v3/api-docs
```

## Mock Data

The application includes mock data for testing purposes. When the application starts with `spring.jpa.hibernate.ddl-auto=create-drop` (default in development), the following mock data is automatically loaded:

### Users
- Admin user: admin@example.com / password
- Regular user: user@example.com / password

### Teams
- Several sample teams are created with random players

### Leagues
- Sample leagues with teams assigned to them

You can view and interact with this mock data through the API endpoints or the Swagger UI interface.

## Authentication

The API uses JWT for authentication. To authenticate:

1. Use the `/api/v1/auth/login` endpoint with valid credentials
2. Include the returned JWT token in the Authorization header for subsequent requests:
   ```
   Authorization: Bearer <your_token>
   ```

### Swagger UI Authentication

To test secured endpoints in Swagger UI:

1. First, obtain a JWT token by using the `/api/v1/auth/login` endpoint with one of the mock users:
   - Admin user: `admin@example.com` / `password`
   - Regular user: `user@example.com` / `password`

2. The token will be returned in the Authorization header of the response.

3. In the Swagger UI interface, click the "Authorize" button (lock icon) at the top right.

4. In the authorization popup, enter your token in the format:
   ```
   Bearer <your_token>
   ```

5. Click "Authorize" and close the popup. All subsequent API requests will include your token.

#### Sample Token for Testing

For quick testing, you can use this pre-generated token (valid for admin@example.com):
```
Bearer eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJhZG1pbkBleGFtcGxlLmNvbSIsImF1dGgiOiJBRE1JTiIsImlhdCI6MTcxODU5ODQwMCwiZXhwIjoxNzE5NDYyNDAwfQ.DjkXg4QYgST4Qe0IV9L0i4fx5-kBzQXJwR9IOYPfLqc
```

Note: This token is for testing purposes only and will expire on June 26, 2025. For production use, always generate a fresh token using the login endpoint.

## Main Endpoints

- `/api/v1/users` - User management
- `/api/v1/teams` - Team management
- `/api/v1/leagues` - League management
- `/api/v1/auth` - Authentication and registration

For detailed information about each endpoint, refer to the Swagger UI documentation.

## Java 23 Compatibility Notes

This project uses Java 23, which may display certain JVM warnings. The following JVM arguments have been configured in the Maven plugins to address these warnings:

- `-XX:+EnableDynamicAgentLoading`: Suppresses warnings about dynamic agent loading, which can occur with certain testing frameworks.
- `-Xshare:off`: Disables Class Data Sharing (CDS) to prevent warnings about "Sharing is only supported for boot loader classes".

These arguments are configured in both the Maven Surefire plugin (for tests) and the Spring Boot Maven plugin (for running the application).
