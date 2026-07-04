# Claude Practice - Spring Boot Application

A Spring Boot web application practice project demonstrating Java application development with Spring Framework.

## Technologies Used

- **Java 17** - Programming language
- **Spring Boot 4.1.0** - Application framework
- **Spring MVC** - Web framework for building web applications
- **Spring Data JPA** - Database access and ORM
- **Spring Security** - Authentication and authorization
- **Thymeleaf** - Template engine for server-side HTML rendering
- **H2 Database** - In-memory database
- **Maven** - Build tool and dependency management
- **Lombok** - Code generation library to reduce boilerplate
- **JUnit 5** - Testing framework

## Getting Started

### Prerequisites

- JDK 17 or later
- Maven 3.8+ (or use Maven wrapper `./mvnw`)

### Building the Project

```bash
./mvnw clean compile
```

### Running Tests

```bash
./mvnw test
```

Run a specific test:
```bash
./mvnw test -Dtest=ClaudePracticeApplicationTests
```

### Running the Application

```bash
./mvnw spring-boot:run
```

Or after building:
```bash
./mvnw package
java -jar target/claudePractice-0.0.1-SNAPSHOT.jar
```

## Project Structure

```
src/
├── main/java/com/claude/claudePractice/
│   ├── ClaudePracticeApplication.java  # Main application class
│   ├── MyClass.java                      # Example utility class
│   ├── HomeController.java               # Thymeleaf page controllers
│   ├── AuthController.java               # Login & registration controller
│   ├── SecurityConfig.java               # Spring Security configuration
│   ├── Product.java                      # Product model class
│   ├── User.java                         # User entity (JPA)
│   └── UserRepository.java               # User data access (JPA)
├── main/resources/
│   ├── templates/
│   │   ├── index.html                    # E-commerce home page
│   │   ├── products.html                 # Products listing page
│   │   ├── contact.html                  # Contact form page
│   │   ├── login.html                    # Login page
│   │   └── register.html                 # Registration page
│   ├── static/
│   │   ├── css/style.css                 # Custom styles
│   │   └── images/                       # Product SVG images
│   │       ├── headphones.svg
│   │       ├── watch.svg
│   │       ├── speaker.svg
│   │       ├── laptop.svg
│   │       ├── camera.svg
│   │       └── tablet.svg
│   └── application.properties            # App & DB config
└── test/java/com/claude/claudePractice/
    └── ClaudePracticeApplicationTests.java  # Unit tests
```

## Features

### Java Classes
- **MyClass**: A simple utility class demonstrating:
  - Instance method `greet(String name)` - returns a greeting message
  - Instance method `add(int a, int b)` - performs addition
  - Default constructor
- **Product**: Model class with name, description, price, and imageUrl fields
- **HomeController**: Spring MVC controller handling routes for `/`, `/products`, `/contact`
- **AuthController**: Handles `/login` and `/register` routes
- **SecurityConfig**: Spring Security setup with form login, BCrypt passwords, and public page access
- **User**: JPA entity with id, username, password, role stored in H2
- **UserRepository**: JPA repository with `findByUsername` lookup

### Thymeleaf Templates
- **index.html**: E-commerce home page featuring:
  - Responsive navigation header with Login/Logout
  - Hero section with call-to-action
  - Dynamic featured products grid (6 products with SVG images)
  - Special offers section
  - Footer with newsletter subscription
- **products.html**: Products listing page with grid layout and product images
- **contact.html**: Contact form with address, phone, email, and business hours
- **login.html**: Login form with error/success messages, link to register
- **register.html**: Registration form with duplicate username validation

### Web Pages
- **Home Page** (`/`): Main landing page with featured products
- **Products Page** (`/products`): Grid view of all products
- **Contact Page** (`/contact`): Contact form and business info
- **Login Page** (`/login`): User authentication
- **Register Page** (`/register`): New user registration

### Authentication & Database
- In-memory H2 database with JPA persistence
- User registration with BCrypt password hashing
- Form-based login with Spring Security
- Session-aware nav showing username and logout when signed in
- H2 Console available at `/h2-console` in development

### Product Images
- Custom SVG product illustrations for each item
- Laptop, Camera, and Tablet added alongside original products
- Images served statically from `/images/`

### Styling
- Bootstrap 5 for responsive design
- Custom CSS with modern styling
- Mobile-friendly navigation

## Project Flow

### Request Lifecycle

```
Browser Request
       │
       ▼
  ┌─────────────────┐
  │  Spring Security │──→ Login page if protected route & not authenticated
  │  (Filter Chain)  │
  └────────┬─────────┘
           │ (authenticated or public route)
           ▼
  ┌─────────────────┐
  │  HomeController  │──→ Returns model data (products, page title, etc.)
  │  AuthController  │──→ Handles login/register form submissions
  └────────┬─────────┘
           │ (model attributes)
           ▼
  ┌─────────────────┐
  │  Thymeleaf       │──→ Renders HTML with dynamic data
  │  Template Engine │
  └────────┬─────────┘
           │ (HTML response)
           ▼
  Browser renders page (Bootstrap 5 UI)
```

### Authentication Flow

```
                         ┌──────────────┐
                         │   H2 Database │
                         │   (users tbl) │
                         └──────┬───────┘
                                │
  ┌──────────┐    ┌─────────────┴──────────────┐    ┌──────────┐
  │ Register │───▶│  AuthController            │───▶│  Login   │
  │  Page    │    │  (BCrypt encode + save)    │    │  Page    │
  └──────────┘    └─────────────┬──────────────┘    └────┬─────┘
                                │                        │
                                │              ┌─────────▼──────────┐
                                │              │ Spring Security    │
                                │              │ UserDetailsService │
                                │              │ (load by username) │
                                │              └─────────┬──────────┘
                                │                        │
                                │              ┌─────────▼──────────┐
                                │              │ PasswordEncoder    │
                                │              │ (BCrypt match)     │
                                │              └─────────┬──────────┘
                                │                        │
                                │              ┌─────────▼──────────┐
                                │              │ Session Created    │
                                │              │ (redirect to "/")  │
                                │              └────────────────────┘
```

### Page Routing

| URL | Controller | Template | Auth Required |
|-----|-----------|----------|:---:|
| `/` | `HomeController.home()` | `index.html` | No |
| `/products` | `HomeController.products()` | `products.html` | No |
| `/contact` | `HomeController.contact()` | `contact.html` | No |
| `/cart` | None (static) | — | No |
| `/login` | `AuthController.login()` | `login.html` | No |
| `/register` | `AuthController.registerForm()` | `register.html` | No |
| `/logout` | Spring Security built-in | — | Yes |

### Data Model

```
┌───────────────┐          ┌───────────────┐
│    User       │          │   Product     │
├───────────────┤          ├───────────────┤
│ id (PK)       │          │ name          │
│ username (UQ) │          │ description   │
│ password      │          │ price         │
│ role          │          │ imageUrl      │
└───────────────┘          └───────────────┘
```

- **User** data is persisted in H2 via JPA (`UserRepository`)
- **Product** objects are created in-memory by `HomeController` (no database storage)
- H2 Console is accessible at `/h2-console` (JDBC URL: `jdbc:h2:mem:shopeasy`)

## Development

### IDE Setup

This project can be imported into IntelliJ IDEA or any IDE with Maven support. The `.idea` directory contains IntelliJ IDEA configuration files.

### Code Style

The project uses Lombok annotations to reduce boilerplate code. Ensure your IDE has Lombok plugin installed for proper code assistance.

## References

- [Spring Boot Documentation](https://docs.spring.io/spring-boot/docs/4.1.0/reference/htmlsingle/)
- [Spring Web MVC Documentation](https://docs.spring.io/spring-boot/docs/4.1.0/reference/htmlsingle/#web)
- [Maven Getting Started Guide](https://maven.apache.org/guides/getting-started/)

## License

This project is a practice exercise and has no specific license defined.