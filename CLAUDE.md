# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**E-Commerce Store ("ShopEasy")** — a Spring Boot web application demo with:
- **Java 17** with Maven build system
- **Spring Boot 4.1.0** with Spring MVC (WebMvc)
- **Thymeleaf** for server-side HTML templating (with Spring Security extras)
- **Bootstrap 5.3.2** + **Bootstrap Icons** for frontend UI
- **Spring Security** for authentication and form-based login
- **Spring Data JPA** + **H2 in-memory database** for persistence
- **BCrypt** password encoding
- **JUnit 5** for testing

## Project Structure

```
src/
├── main/java/com/claude/claudePractice/
│   ├── ClaudePracticeApplication.java    # Main entry point (@SpringBootApplication)
│   ├── config/
│   │   └── SecurityConfig.java           # Spring Security config (BCrypt, form login, filter chain)
│   ├── controller/
│   │   ├── HomeController.java           # Routes: /, /products, /contact, /about
│   │   ├── AuthController.java           # Routes: /login, /register (GET + POST)
│   │   ├── CartController.java           # Routes: /cart, /cart/add, /cart/update, /cart/remove
│   │   └── BlogController.java           # Routes: /blog, /blog/{id}
│   ├── model/
│   │   ├── Product.java                  # POJO: name, description, price, imageUrl
│   │   ├── BlogPost.java                 # POJO: id, title, content, excerpt, author, imageUrl, publishedDate
│   │   ├── User.java                     # @Entity: JPA user (username, password, role)
│   │   ├── CartItem.java                 # POJO: cart item DTO (name, imageUrl, price, qty, total)
│   │   └── CartItemEntity.java           # @Entity: JPA cart item (username, productName, imageUrl, price, qty)
│   └── repository/
│       ├── UserRepository.java           # JPA: findByUsername
│       └── CartItemRepository.java       # JPA: findByUsername, findByUsernameAndProductName, deleteByUsername
├── main/resources/
│   ├── application.properties            # App config (H2, JPA, datasource)
│   ├── templates/
│   │   ├── fragments.html                # Shared header + footer fragments (navbar, cart badge, login/logout)
│   │   ├── index.html                    # Home page with hero, featured products, offers
│   │   ├── products.html                 # Products listing page
│   │   ├── cart.html                     # Shopping cart (view, update qty, remove, total)
│   │   ├── login.html                    # Login form
│   │   ├── register.html                 # Registration form
│   │   ├── about.html                    # About Us page
│   │   ├── contact.html                  # Contact Us page
│   │   ├── blog.html                     # Blog listing (cards with excerpts, sidebar)
│   │   └── blog-post.html                # Blog post detail (full content, author info)
│   └── static/css/
│       └── style.css                     # Custom CSS styles
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

## Request Flow

### Browsing & Navigation
```
Browser → GET /                    → HomeController.home()
                                    → model: brandName, heroTitle, heroSubtitle, featured products, cartCount
                                    → template: index.html
                                    → fragments.html (header + footer)

Browser → GET /products            → HomeController.products()
                                    → model: full product list, cartCount
                                    → template: products.html

Browser → GET /about               → HomeController.about() → template: about.html
Browser → GET /contact             → HomeController.contact() → template: contact.html
```

### Index Page Sections
```
Browser → GET / (index.html)
                                    → model: brandName, heroTitle, heroSubtitle, featured products, cartCount
                                    → template: index.html
                                    → sections: Hero → Featured Products → New Arrivals → Features → Footer
                                    → New Arrivals section: 3 products displayed in a responsive grid
```

### Blog Flow
```
Browser → GET /blog                → BlogController.blogList()
                                    → model: all posts (full list), recentPosts (latest 3)
                                    → template: blog.html
                                    → sidebar: About Our Blog + Recent Posts list
                                    → each card: image, date, author, title, excerpt, "Read More →"

Browser → GET /blog/{id}           → BlogController.blogPost(id)
                                    → finds post by ID from in-memory list
                                    → if not found: redirect to /blog
                                    → model: single post, recentPosts (latest 3)
                                    → template: blog-post.html
                                    → breadcrumb: Home > Blog > Post Title
                                    → sidebar: About Author, Recent Posts (excluding current), ShopEasy Promise
                                    → full content rendered with th:utext (HTML)
```

### Authentication Flow
```
Browser → GET /register            → AuthController.registerForm() → template: register.html
Browser → POST /register           → AuthController.register(username, password)
                                    → validates username is unique
                                    → encodes password with BCrypt, saves User (ROLE_USER)
                                    → redirects to /login

Browser → GET /login               → AuthController.login() → template: login.html
Browser → POST /login              → Spring Security form login (SecurityFilterChain)
                                    → on success: redirect to /
                                    → on failure: return to /login

Browser → POST /logout             → Spring Security logout → redirect to /
```

### Shopping Cart Flow (requires authentication for data, but pages are public)
```
Browser → GET /cart                → CartController.viewCart()
                                    → loads CartItemEntity list for current user
                                    → converts to CartItem DTOs, computes total
                                    → template: cart.html

Browser → POST /cart/add           → CartController.addToCart(name, imageUrl, price)
                                    → finds or creates CartItemEntity for this user + product
                                    → increments quantity if exists, else creates with qty=1
                                    → redirects to /cart

Browser → POST /cart/update        → CartController.updateCart(name, quantity)
                                    → sets quantity (deletes if qty ≤ 0)
                                    → redirects to /cart

Browser → POST /cart/remove        → CartController.removeFromCart(name)
                                    → deletes CartItemEntity for this user + product
                                    → redirects to /cart
```

### Layout & Navigation (fragments.html)
All pages share header and footer via Thymeleaf `th:replace`:
- **Header**: brand logo, nav links (Home, Products, **Blog**, About, Contact), cart icon with badge count, login/logout button
- **Cart badge**: shown when `cartCount > 0`, computed per-request from CartItemRepository
- **Auth controls**: Login button shown for anonymous users; username + Logout form shown for authenticated users (via `sec:authorize` tags)
- **Footer**: brand info, links, newsletter form, copyright with dynamic year

## Authentication & Security

- **Public pages**: `/`, `/products`, `/about`, `/contact`, `/blog`, `/blog/**`, `/cart`, `/cart/**`, `/login`, `/register`, static resources, H2 console
- **Protected pages**: any other URL (no authenticated-only pages currently exist; cart endpoints are POST but permitAll for GET)
- **Password storage**: BCrypt via `PasswordEncoder` bean
- **User details**: custom `UserDetailsService` backed by `UserRepository` (role stored as `ROLE_USER`, stripped prefix for Spring Security)
- **Database**: H2 in-memory (`jdbc:h2:mem:shopeasy`), console enabled at `/h2-console/`, JPA `create-drop` mode

## Database Schema

### User (users table)
| Column       | Type     | Constraints          |
|-------------|----------|----------------------|
| id          | Long     | PK, auto-generated   |
| username    | String   | UNIQUE, NOT NULL     |
| password    | String   | NOT NULL (BCrypt)    |
| role        | String   | NOT NULL (ROLE_USER) |

### CartItemEntity (cart_items table)
| Column      | Type   | Constraints          |
|-------------|--------|----------------------|
| id          | Long   | PK, auto-generated   |
| username    | String | NOT NULL             |
| productName | String | NOT NULL             |
| imageUrl    | String | NOT NULL             |
| price       | double | NOT NULL             |
| quantity    | int    | NOT NULL             |

## Architecture Notes

- `HomeController`, `AuthController`, `CartController`, and `BlogController` are `@Controller` classes using `@GetMapping`/`@PostMapping`
- `ClaudePracticeApplication` is a `@SpringBootApplication` that only calls `SpringApplication.run()` (no startup logic)
- `Product` is a plain POJO used for display data; `BlogPost` is a POJO with HTML content for blog articles (rendered with `th:utext`); `User` and `CartItemEntity` are JPA `@Entity` classes mapped to `users` and `cart_items` tables
- `CartItem` is a non-entity POJO DTO (computed `getTotal()` = price × quantity)
- Repositories extend `JpaRepository` for CRUD
- Cart badge count is computed per-endpoint by querying `CartItemRepository` for the current authenticated user
- Templates use Thymeleaf layout via `th:replace` with fragments from `fragments.html`
- All classes are in the `com.claude.claudePractice` package (with sub-packages for controller, model, config, repository)
- Tests use `@SpringBootTest` annotation for Spring context testing
