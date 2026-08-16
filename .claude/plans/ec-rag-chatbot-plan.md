# RAG Chatbot for EC Policy — Implementation Plan

## 1. Context

Integrate a **RAG (Retrieval-Augmented Generation) chatbot** into the **existing ShopEasy Spring Boot application** that answers user queries about **EC (Ecommerce site) policy** documents.

**Key technologies:**
- Do not change existing ShopEasy package structure or database schema for primary data (MySQL)
- Java 17 + Spring Boot 4.x (existing)
- Spring AI for embeddings + chat
- **MySQL** as primary relational database (existing ShopEasy database)
- **PostgreSQL + pgvector** as secondary vector store (semantic search)
- Apache Tika for document parsing
- Anthropic Claude API for LLM responses

**Scope:** This will be a **new feature module** within the existing ShopEasy e-commerce app.

---

## 2. Module Structure

```
com.shopeasy.ecpolicy/
├── controller/
│   ├── ChatController.java          # REST API for chat queries
│   ├── ChatViewController.java      # Thymeleaf chat page
│   └── DocumentViewController.java  # Document upload/management UI
├── service/
│   ├── DocumentIngestionService.java
│   ├── TextChunkerService.java
│   ├── EmbeddingService.java
│   ├── ContextRetrieverService.java
│   └── ChatService.java
├── repository/
│   ├── DocumentRepository.java        # MySQL
│   └── DocumentChunkRepository.java   # PostgreSQL (pgvector)
├── model/
│   ├── DocumentEntity.java
│   └── DocumentChunkEntity.java
├── dto/
│   ├── ChatRequest.java
│   ├── ChatResponse.java
│   └── ChatHistoryResponse.java
└── config/
    ├── PrimaryDataSourceConfig.java   # MySQL
    └── VectorDataSourceConfig.java    # PostgreSQL / pgvector
```

---

## 3. Maven Dependencies

### Core Dependencies
```xml
<dependencies>
  <!-- Spring Boot Starter -->
  <dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
  </dependency>

  <!-- Spring Data JPA (MySQL) -->
  <dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa</artifactId>
  </dependency>

  <!-- MySQL Driver (Primary Database) -->
  <dependency>
    <groupId>com.mysql</groupId>
    <artifactId>mysql-connector-j</artifactId>
    <scope>runtime</scope>
  </dependency>

  <!-- PostgreSQL Driver (Secondary Vector Store) -->
  <dependency>
    <groupId>org.postgresql</groupId>
    <artifactId>postgresql</artifactId>
    <scope>runtime</scope>
  </dependency>
</dependencies>
```

### Spring AI Dependencies
```xml
<dependency>
  <groupId>org.springframework.ai</groupId>
  <artifactId>spring-ai-openai-spring-boot-starter</artifactId>
</dependency>
<dependency>
  <groupId>org.springframework.ai</groupId>
  <artifactId>spring-ai-anthropic-spring-boot-starter</artifactId>
</dependency>
<dependency>
  <groupId>org.springframework.ai</groupId>
  <artifactId>spring-ai-pgvector-store-spring-boot-starter</artifactId>
</dependency>
```

### Apache Tika Dependencies
```xml
<dependency>
  <groupId>org.apache.tika</groupId>
  <artifactId>tika-core</artifactId>
</dependency>
<dependency>
  <groupId>org.apache.tika</groupId>
  <artifactId>tika-parsers-standard-package</artifactId>
</dependency>
```

### Testing Dependencies
```xml
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-test</artifactId>
  <scope>test</scope>
</dependency>
```

---

## 4. Database Configuration Strategy

### 4.1 Primary Database (MySQL) — Main Application Data
- Stores `DocumentEntity` and other persistent objects
- Configured in `application.yml` as primary datasource
- Uses HikariCP connection pooling
- Schema generated via JPA/Hibernate

### 4.2 Secondary Database (PostgreSQL) — Vector Storage
- Stores `DocumentChunkEntity` with vector embeddings
- Configured via `application-postgres.yml` as secondary datasource
- Uses `pgvector` extension for similarity search
- JDBC Vector Store integration for Spring AI

**Why this separation?**
- MySQL: reliable transactional storage for document metadata
- PostgreSQL: optimized vector similarity search with pgvector
- Clear separation of concerns between relational data and vector search

---

## 5. Data Model

### `DocumentEntity` (MySQL — Primary)
```java
@Entity
@Table(name = "ec_documents")
public class DocumentEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String sourceFileName;

    @Column(nullable = false)
    private String status; // PENDING, PROCESSED, FAILED

    @Column(nullable = false)
    private LocalDateTime uploadedAt;

    @Column
    private Integer totalChunks;
}
```

### `DocumentChunkEntity` (PostgreSQL — Vector Store)
```java
@Entity
@Table(name = "ec_document_chunks")
public class DocumentChunkEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // References DocumentEntity.id from MySQL — not a JPA relation,
    // since this lives in a separate database.
    @Column(name = "document_id", nullable = false)
    private Long documentId;

    @Lob
    @Column(nullable = false)
    private String content;

    @Column(name = "embedding", columnDefinition = "VECTOR(1536)")
    private float[] embedding;
}
```

> **Note:** Since `DocumentEntity` (MySQL) and `DocumentChunkEntity` (PostgreSQL) live in different databases, there is no real foreign-key/JPA `@ManyToOne` relationship between them — the link is maintained at the application level via `documentId`.

---

## 6. Configuration Files

### `application.yml` (MySQL Primary)
```yaml
server:
  port: 8081

spring:
  datasource:
    url: jdbc:mysql://localhost:3306/ec_rag_chatbot_primary?useSSL=false&serverTimezone=UTC
    username: ec_user
    password: ${MYSQL_PASSWORD}
    driver-class-name: com.mysql.cj.jdbc.Driver

  jpa:
    hibernate:
      ddl-auto: update
    properties:
      hibernate:
        dialect: org.hibernate.dialect.MySQLDialect
        jdbc:
          lob:
            non_contextual_creation: true

  ai:
    openai:
      api-key: ${OPENAI_API_KEY}
      embedding:
        model: text-embedding-3-small
        dimensions: 1536
      base-url: https://api.openai.com

    anthropic:
      api-key: ${ANTHROPIC_API_KEY}
      chat:
        model: claude-sonnet-5
        max-tokens: 1000
        temperature: 0.7
```

### `application-postgres.yml` (PostgreSQL Secondary)
```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/ec_rag_chatbot_vectors?useSSL=false
    username: postgres
    password: ${POSTGRES_PASSWORD}
    driver-class-name: org.postgresql.Driver

  jpa:
    hibernate:
      ddl-auto: update
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
```

### `VectorDataSourceConfig.java` (Uses PostgreSQL)
```java
@Configuration
@EnableJpaRepositories(
    basePackages = "com.shopeasy.ecpolicy.repository.vector",
    entityManagerFactoryRef = "vectorEntityManagerFactory",
    transactionManagerRef = "vectorTransactionManager"
)
public class VectorDataSourceConfig {
    // Configure a second DataSource, EntityManagerFactory, and
    // TransactionManager pointing at application-postgres.yml,
    // separate from the primary MySQL beans.
}
```

---

## 7. Implementation Steps

1. **Project Setup** — create the module/package structure shown above
2. **Maven Dependencies** — add MySQL (primary), PostgreSQL (secondary), Spring AI, and Tika
3. **Database Configuration**
    - Primary MySQL config (`application.yml`) — JPA with MySQL dialect
    - Secondary PostgreSQL config (`application-postgres.yml`) — JDBC Vector Store connection
4. **Document Ingestion Pipeline**
    - Parse documents with Tika
    - Chunk text with overlap
    - Generate embeddings via OpenAI
    - Store chunks in PostgreSQL using pgvector
5. **Vector Search Integration**
    - Implement `DocumentChunkRepository` with native pgvector query
    - Create `ContextRetrieverService` for similarity search
6. **Chat Endpoint Design**
    - Retrieve top-k chunks from PostgreSQL
    - Construct prompt with context + user query
    - Call Claude API for response generation
    - Return response with source citations
7. **API Endpoints**
    - `POST /api/admin/ingest` — upload and process documents
    - `POST /api/chat` — chat with natural language queries
    - `GET /api/search` — retrieve similar document chunks
8. **Testing**
    - Unit tests for chunking and embedding
    - Integration tests with embedded MySQL/PostgreSQL instances
    - E2E tests for the full pipeline

---

## 8. Data Flow Diagram

```
[Document Upload]
        │
        ▼
[Tika Parser] → [TextChunker] → [EmbeddingGenerator]
        │                             │
        ▼                             ▼
[DocumentEntity]                [DocumentChunkEntity]
 (stored in MySQL)               (stored in PostgreSQL)
        │                             │
        └──────────[linked by documentId]──────────┘
                          │
                          ▼
              [Similarity Search in PostgreSQL]
                          │
                          ▼
              [Context Retrieval for LLM Prompt]
                          │
                          ▼
               [Claude API Call → Response]
```

---

## 9. UI Implementation (Thymeleaf + jQuery + REST API)

### Controller Additions

**ChatViewController**
```java
@Controller
@RequestMapping("/chat")
public class ChatViewController {

    @GetMapping
    public String chatPage() {
        return "chat";
    }

    @GetMapping("/history")
    @ResponseBody
    public List<ChatHistoryResponse> getHistory(@AuthenticationPrincipal UserDetails user) {
        // Returns conversation history for the logged-in user.
        // Requires a ConversationHistory entity + repository (MySQL).
        return new ArrayList<>();
    }
}
```

**DocumentViewController**
```java
@Controller
@RequestMapping("/documents")
public class DocumentViewController {

    @GetMapping
    public String documentsPage(Model model) {
        return "documents";
    }

    @PostMapping("/upload")
    public String uploadDocument(@RequestParam("file") MultipartFile file) {
        // Delegates to DocumentIngestionService, then redirects.
        return "redirect:/documents";
    }
}
```

### Frontend
- `chat.html` — Thymeleaf template with a chat window, message list, input box
- `documents.html` — Thymeleaf template with an upload form + document status table
- jQuery + `fetch`/`$.ajax` calls to `/api/chat` and `/api/admin/ingest`
- Reuse existing Bootstrap 5.3.2 styling from ShopEasy for visual consistency

---

## 10. Testing Strategy

### Unit Testing
- Chunking logic (chunk size, overlap boundaries)
- Embedding service (mocked API calls)
- Controller-level tests with `MockMvc`

### Integration Testing
- Full ingestion pipeline with embedded/test MySQL + PostgreSQL instances
- Chat flow: query → retrieval → LLM call → response
- Document upload and status transitions

### UI / E2E Testing
- Selenium or Cypress for chat flow, document upload, session handling
- Manual verification of Thymeleaf template rendering

### Performance Testing
- Load testing on `/api/chat` under concurrent requests
- Vector similarity query performance at scale
- Pagination testing for document listing

---

## 11. Open Questions (to confirm before/while building)

1. **UI design** — any preferred color scheme or brand guidelines, or reuse ShopEasy's existing Bootstrap theme as-is?
2. **Authentication** — chat/document endpoints secured via existing Spring Security session auth, or a separate JWT-based scheme?
3. **Document upload limit** — max file size for uploads?
4. **Chat history persistence** — save per user? For how long?
5. **Rate limiting** — should chat queries or document uploads be rate-limited?
6. **Local dev environment** — assuming MySQL 8.0+ and PostgreSQL 12+ (with pgvector extension) are both available locally
7. **Embedding dimensions** — assuming 1536-dim embeddings from `text-embedding-3-small`

---

## 12. Implementation Checklist

- [ ] Project setup with all dependencies
- [ ] Dual datasource configuration (MySQL + PostgreSQL)
- [ ] Entity definitions: `DocumentEntity` (MySQL) and `DocumentChunkEntity` (PostgreSQL)
- [ ] Database schema creation + pgvector extension setup
- [ ] Apache Tika document parser implementation
- [ ] Text chunking with overlap strategy
- [ ] Embedding generation integration
- [ ] Vector search with pgvector similarity queries
- [ ] REST API endpoints for chat, search, and document management
- [ ] Thymeleaf templates for UI
- [ ] jQuery REST API integration
- [ ] Chat interface with real-time responses
- [ ] Document upload and management UI
- [ ] Authentication and authorization
- [ ] Unit and integration tests
- [ ] Deployment scripts and documentation