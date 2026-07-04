# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**E-Commerce Store ("ShopEasy")** — a Spring Boot web application demo with:
- **Java 17** with Maven build system
- **Spring Boot 4.1.0** with Spring MVC (WebMvc)
- **Thymeleaf** for server-side HTML templating
- **Bootstrap 5.3.2** for frontend UI
- **Lombok** for reducing boilerplate code
- **JUnit 5** for testing

## Project Structure

```
src/
├── main/java/com/claude/claudePractice/
│   ├── ClaudePracticeApplication.java  # Main entry point (@SpringBootApplication)
│   ├── HomeController.java             # MVC Controller (routes: /, /products)
│   ├── Product.java                    # Model class (name, description, price)
│   └── MyClass.java                    # Demo utility class (greet, add methods)
├── main/resources/
│   ├── application.properties          # App config (spring.application.name)
│   ├── templates/
│   │   ├── index.html                  # Home page with hero, featured products, offers
│   │   └── products.html               # Products listing page
│   └── static/css/
│       └── style.css                   # Custom CSS styles
└── test/java/com/claude/claudePractice/
    └── ClaudePracticeApplicationTests.java  # Context load test
```

## Common Commands

**Build the project:**
```bash
./mvnw clean compile
```

**Run tests:**
```bash
./mvnw test
# Run a specific test:
./mvnw test -Dtest=ClaudePracticeApplicationTests
```

**Run the application:**
```bash
./mvnw spring-boot:run
```

**Package the application:**
```bash
./mvnw package
```

**Clean compiled files:**
```bash
./mvnw clean
```

## Architecture Notes

- The main class `ClaudePracticeApplication` instantiates `MyClass` and calls its methods before starting the Spring Boot context
- `HomeController` is a `@Controller` with `@GetMapping` routes for `/` (home page with 3 featured products) and `/products` (product listing)
- `Product` is a simple POJO with name, description, and price fields
- Templates use Thymeleaf with Bootstrap 5 for responsive layouts
- All classes are in the `com.claude.claudePractice` package
- Tests use `@SpringBootTest` annotation for Spring context testing
