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

## Main Endpoints

- `/api/v1/users` - User management
- `/api/v1/teams` - Team management
- `/api/v1/leagues` - League management
- `/api/v1/auth` - Authentication and registration

For detailed information about each endpoint, refer to the Swagger UI documentation.