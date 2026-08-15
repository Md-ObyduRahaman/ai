---
name: ec-rag-chatbot-plan
description: Implementation plan for a RAG-based chatbot on EC policy using Spring AI, pgvector, and Tika document parsing, with MySQL as primary datasource and PostgreSQL as secondary vector store
metadata:
  type: project
---

# RAG Chatbot for Ecommerce site Policy — Implementation Plan (Updated)

## 1. Context

Integrate a **RAG (Retrieval-Augmented Generation) chatbot** into the **existing ShopEasy Spring Boot application** that answers user queries about **Ecommerce site (EC) policy** documents.

**Key technologies:**
- Java 17 + Spring Boot 3.2.x (existing)
- Spring AI for embeddings + chat
- **MySQL** as primary relational database (existing ShopEasy database)
- **PostgreSQL + pgvector** as secondary vector store (semantic search)
- Apache Tika for document parsing
- Anthropic Claude API for LLM responses

**Scope:** This will be a **new feature module** within the existing ShopEasy e-commerce app.

---

## 2. Project Structure

```
ec-rag-chatbot/
├── pom.xml
├── src/main/java/com/shopeasy/ecchat/
│   ├── EcRagChatbotApplication.java          # Main application class
│   ├── config/
│   │   ├── PrimaryDataSourceConfig.java      # MySQL primary config
│   │   ├── VectorStoreConfig.java            # PostgreSQL pgvector config (secondary)
│   │   ├── EmbeddingConfig.java              # OpenAI embedding model
│   │   └── ChatConfig.java                   # Claude chat configuration
│   ├── document/
│   │   ├── DocumentIngestor.java             # Orchestrates parsing + chunking + embedding
│   │   ├── TikaDocumentParser.java           # Parses PDF/Word/TXT/HTML via Apache Tika
│   │   └── TextChunker.java                  # Splits text into chunks
│   ├── model/
│   │   ├── DocumentEntity.java               # JPA entity for source docs (MySQL)
│   │   └── DocumentChunkEntity.java          # Entity for vector storage (PostgreSQL)
│   ├── repository/
│   │   ├── DocumentRepository.java             # MySQL repository (JPA)
│   │   └── DocumentChunkRepository.java      # PostgreSQL repository (JDBC Vector Store)
│   ├── retrieval/
│   │   └── ContextRetriever.java             # Top-k similarity search in PostgreSQL
│   ├── chat/
│   │   ├── ChatController.java               # REST endpoint for chat
│   │   ├── ChatService.java                  # Combines retrieval + LLM call
│   │   └──dto/
│   │       ├── ChatRequest.java
│   │       ├── ChatResponse.java
│   │       └── IngestRequest.java
│   └── util/
│       └── TokenUtils.java                   # Estimate token count
└── src/main/resources/
    ├── application.yml                         # Primary DB config
    └── application-postgres.yml              # PostgreSQL vector store config
└── src/test/java/.../EcRagChatbotApplicationTests.java
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
*(Same as before - spring-ai-openai-spring-boot-starter, spring-ai-anthropic-spring-boot-starter, spring-ai-vector-store)*

### Apache Tika Dependencies
*(Same as before - tika-core, tika-parsers)*

### Testing Dependencies
*(Same as before - spring-boot-starter-test)*

---

## 4. Database Configuration Strategy

### 4.1 Primary Database (MySQL) - Main Application Data
- Stores **DocumentEntity** and other persistent objects
- Configured in `application.yml` as primary datasource
- Uses HikariCP connection pooling
- Schema generated via JPA/Hibernate

### 4.2 Secondary Database (PostgreSQL) - Vector Storage
- Stores **DocumentChunkEntity** with vector embeddings
- Configured via `application-postgres.yml` as secondary datasource
- Uses `pgvector` extension for similarity search
- JDBC Vector Store integration for Spring AI

**Why this separation?**
- MySQL: Reliable transactional storage for document metadata
- PostgreSQL: Optimized vector similarity search with pgvector
- Clear separation of concerns between relational data and vector search

---

## 5. Data Model

### `DocumentEntity` (MySQL - Primary Store)
```java
@Entity
@Table(name = "ec_documents")
public class DocumentEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String title;
    private String fileName;
    private String url;
    private LocalDateTime uploadedAt;
}
```

### `DocumentChunkEntity` (PostgreSQL - Vector Store)
```java
@Entity
@Table(name = "ec_document_chunks")
public class DocumentChunkEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "document_id", nullable = false)
    private DocumentEntity document;

    @Lob
    @Column(nullable = false)
    private String content;

    @Column(name = "embedding", columnDefinition = "VECTOR(1536)")
    private float[] embedding;
}
```

---

## 6. Configuration Files

### `application.yml` (MySQL Primary)
```yaml
server:
  port: 8081

spring:
  datasource:
    # Primary MySQL datasource
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
        model: claude-3-opus-20240229
        max-tokens: 1000
        temperature: 0.7
```

### `application-postgres.yml` (PostgreSQL Secondary)
```yaml
spring:
  datasource:
    # Secondary PostgreSQL datasource for vectors
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

### `VectorStoreConfig.java` (Uses PostgreSQL)
```java
@Configuration
public class VectorStoreConfig {

    @Bean
    public JdbcVectorStore vectorStore(@Qualifier("secondaryDataSource") DataSource dataSource) {
        return JdbcVectorStore.builder()
            .dataSource(dataSource)
            .tableName("ec_document_chunks")
            .embeddingDimension(1536)
            .build();
    }

    @Bean(name = "secondaryDataSource")
    @Primary
    public DataSource secondaryDataSource() {
        return new org.springframework.boot.autoconfigure.jdbc.DataSourceBuilder().build();
    }
}
```

---

## 7. Implementation Steps

### Step 1: Project Setup
Create the Maven project structure as shown above.

### Step 2: Maven Dependencies
Add dependencies for MySQL (primary), PostgreSQL (secondary), Spring AI, and Tika.

### Step 3: Database Configuration
1. **Primary MySQL Configuration** (`application.yml`):
   - Configure primary datasource for `DocumentEntity` storage
   - Set up JPA with MySQL dialect

2. **Secondary PostgreSQL Configuration** (`application-postgres.yml`):
   - Configure secondary datasource for vector storage
   - Set up JDBC Vector Store connection

### Step 4: Document Ingestion Pipeline
1. Parse documents using Tika
2. Chunk text with overlap
3. Generate embeddings using OpenAI
4. Store chunks in PostgreSQL using pgvector

### Step 5: Vector Search Integration
- Implement `DocumentChunkRepository` with native pgvector query
- Create `ContextRetriever` service for similarity search

### Step 6: Chat Endpoint Design
- Retrieve top-k chunks from PostgreSQL
- Construct prompt with context + user query
- Call Claude API for response generation
- Return response with source citations

### Step 7: API Endpoints
- `POST /api/admin/ingest` - Upload and process documents
- `POST /api/chat` - Chat with natural language queries
- `GET /api/search` - retrieve similar document chunks

### Step 8: Testing
- Unit tests for chunking and embedding
- Integration tests with embedded MySQL/PostgreSQL instances
- E2E tests for full pipeline

---

## 8. Data Flow Diagram

```
[Document Upload] 
        │
        ▼
[Tika Parser] → [TextChunker] → [EmbeddingGenerator]
        │                             │
        ▼                             ▼
[DocumentEntity] (stored in MySQL) [DocumentChunkEntity] (stored in PostgreSQL)
        │                             │
        └─────[DocumentRepository]─────┘
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

## 9. Assumptions & Open Questions

1. **Database Connectivity**: We assume both MySQL 8.0+ and PostgreSQL 12+ are available locally for development
2. **Vector Dimensions**: We assume 1536-dimensional embeddings from `text-embedding-3-small`
3. **Separate Databases**: Primary (MySQL) and secondary (PostgreSQL) databases will be separate instances
4. **Authentication**: Environmental variables for DB passwords must be configured
5. **Query Limits**: We assume reasonable query lengths for both vector search and LLM prompts

---

## 10. UI Implementation (Thymeleaf + jQuery + REST API)

### Step 9: Web UI Setup

### Project Structure Addition
```
└── src/main/resources/
    ├── application.yml
    ├── application-postgres.yml
    └── templates/
        ├── fragments.html                    # Shared Thymeleaf fragments
        │   ├── navbar.html                 # Navigation bar
        │   └── footer.html                 # Page footer
        ├── index.html                      # Home page with chat widget
        ├── chat.html                       # Chat interface page
        ├── documents.html                  # Document management page
        ├── login.html                      # User login page
        └── register.html                   # User registration page
    └── static/
        ├── css/
        │   ├── style.css                   # Custom styles
        │   └── chat.css                      # Chat-specific styles
        ├── js/
        │   ├── main.js                       # Core jQuery + REST API calls
        │   └── chat.js                       # Chat-specific JavaScript
        └── assets/
            └── img/                          # Logo, favicon, etc.
```

### 10.1 Maven Dependencies Additions
```xml
<!-- Thymeleaf Templates -->
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-thymeleaf</artifactId>
</dependency>

<!-- jQuery REST API calls (WebJars) -->
<dependency>
  <groupId>org.webjars</groupId>
  <artifactId>jquery</artifactId>
  <version>3.7.1</version>
</dependency>

<!-- Bootstrap for styling -->
<dependency>
  <groupId>org.webjars</groupId>
  <artifactId>bootstrap</artifactId>
  <version>5.3.2</version>
</dependency>
```

### 10.2 Thymeleaf Templates

#### Main Layout (`templates/fragments.html`)
```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title th:fragment="title">EC Policy Chatbot</title>
    <link href="/webjars/bootstrap/css/bootstrap.min.css" rel="stylesheet">
    <link href="/css/style.css" rel="stylesheet">
</head>
<body>
    <header th:fragment="navbar">
        <nav class="navbar navbar-expand-lg navbar-dark bg-primary">
            <div class="container">
                <a class="navbar-brand" th:href="@{/}">EC Policy Chat</a>
                <button class="navbar-toggler" type="button" data-bs-toggle="collapse" data-bs-target="#navbarNav">
                    <span class="navbar-toggler-icon"></span>
                </button>
                <div class="collapse navbar-collapse" id="navbarNav">
                    <ul class="navbar-nav ms-auto">
                        <li class="nav-item"><a class="nav-link" th:href="@{/}">Home</a></li>
                        <li class="nav-item"><a class="nav-link" th:href="@{/chat}">Chat</a></li>
                        <li class="nav-item"><a class="nav-link" th:href="@{/documents}">Documents</a></li>
                        <li class="nav-item"><a class="nav-link" th:href="@{/login}">Login</a></li>
                    </ul>
                </div>
            </div>
        </nav>
    </header>

    <footer th:fragment="footer">
        <footer class="bg-dark text-light py-4 mt-5">
            <div class="container">
                <div class="row">
                    <div class="col-md-6">
                        <h5>EC Policy Chatbot</h5>
                        <p>Assisting with Election Commission policy queries using AI-powered semantic search.</p>
                    </div>
                    <div class="col-md-6 text-md-end">
                        <p>&copy; <span id="year"></span> EC Policy Chatbot. All rights reserved.</p>
                    </div>
                </div>
            </div>
        </footer>
    </footer>

    <script src="/webjars/jquery/jquery.min.js"></script>
    <script src="/webjars/bootstrap/js/bootstrap.bundle.min.js"></script>
    <script src="/js/main.js"></script>
    <script>$('#year').text(new Date().getFullYear());</script>
</body>
</html>
```

#### Home Page (`templates/index.html`)
```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org"
      th:replace="fragments :: title(&quot;Home&quot;)">
<body>
    <header th:replace="fragments :: navbar"></header>

    <div class="container py-5">
        <div class="text-center mb-5">
            <h1 class="display-4">Welcome to EC Policy Chatbot</h1>
            <p class="lead">Ask questions about Election Commission policies and documents.</p>
            <a th:href="@{/chat}" class="btn btn-primary btn-lg">Start Chatting</a>
        </div>

        <div class="row">
            <div class="col-md-6">
                <div class="card">
                    <div class="card-header">How It Works</div>
                    <div class="card-body">
                        <ol>
                            <li>Upload EC policy documents (PDF, Word, HTML)</li>
                            <li>Documents are parsed and chunked for search</li>
                            <li>Embeddings enable semantic search in PostgreSQL</li>
                            <li>Chat with Claude AI using retrieved context</li>
                        </ol>
                    </div>
                </div>
            </div>
            <div class="col-md-6">
                <div class="card">
                    <div class="card-header">Features</div>
                    <div class="card-body">
                        <ul>
                            <li>Natural language Q&A about EC policies</li>
                            <li>Source citations for all answers</li>
                            <li>Full document search capability</li>
                            <li>User authentication for document management</li>
                        </ul>
                    </div>
                </div>
            </div>
        </div>
    </div>

    <footer th:replace="fragments :: footer"></footer>
</body>
</html>
```

#### Chat Interface (`templates/chat.html`)
```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org"
      th:replace="fragments :: title(&quot;Chat&quot;)">
<body>
    <header th:replace="fragments :: navbar"></header>

    <div class="container py-4">
        <div class="row">
            <div class="col-lg-8 mx-auto">
                <div class="card">
                    <div class="card-header">
                        <h5 class="mb-0">EC Policy Chat</h5>
                    </div>
                    <div class="card-body">
                        <div id="chat-container" class="mb-4" style="height: 500px; overflow-y: auto;">
                            <div id="chat-history" class="d-flex flex-column">
                                <!-- Messages will be appended here -->
                            </div>
                            <div id="loading" class="text-center d-none">
                                <div class="spinner-border text-primary" role="status">
                                    <span class="visually-hidden">Loading...</span>
                                </div>
                            </div>
                        </div>

                        <form id="chat-form" class="input-group">
                            <input type="text" id="question-input" class="form-control" 
                                   placeholder="Ask about EC policies..." autocomplete="off">
                            <button type="submit" class="btn btn-primary">Send</button>
                        </form>

                        <div id="sources" class="mt-3"></div>
                    </div>
                </div>
            </div>
        </div>
    </div>

    <footer th:replace="fragments :: footer"></footer>

    <script src="/webjars/jquery/jquery.min.js"></script>
    <script src="/webjars/bootstrap/js/bootstrap.bundle.min.js"></script>
    <script src="/js/main.js"></script>
    <script src="/js/chat.js"></script>
</body>
</html>
```

### 10.3 jQuery + REST API JavaScript (`static/js/chat.js`)
```javascript
$(document).ready(function() {
    const chatHistory = $('#chat-history');

    // Format date/time helper
    function formatDateTime() {
        const now = new Date();
        return now.toLocaleTimeString([], {hour: '2-digit', minute:'2-digit'});
    }

    // Add message to chat history
    function addMessage(content, isUser = true, sources = []) {
        const messageClass = isUser ? 'justify-content-end' : 'justify-content-start';
        const bubbleClass = isUser ? 'bg-primary text-white' : 'bg-light text-dark';

        const messageDiv = $(`
            <div class="d-flex mb-3 ${messageClass}">
                <div class="p-3 rounded ${bubbleClass} message-bubble">
                    ${content}
                    <small class="d-block mt-1"><small class="text-muted">${formatDateTime()}</small></small>
                </div>
            </div>
        `);

        chatHistory.append(messageDiv);
        chatHistory.scrollTop(chatHistory[0].scrollHeight);

        // Add sources if provided
        if (sources.length > 0) {
            const sourcesDiv = $('<div class="mb-3"><h6 class="text-muted small">Sources:</h6></div>');
            sources.forEach(source => {
                const sourceItem = $(`
                    <div class="border-start border-primary ps-3 mb-2">
                        <strong>${source.documentTitle}</strong>
                        <p class="mb-1 small">${source.text.substring(0, 200)}...</p>
                        <small class="text-muted">Score: ${(source.score * 100).toFixed(1)}%</small>
                    </div>
                `);
                sourcesDiv.append(sourceItem);
            });
            chatHistory.append(sourcesDiv);
            chatHistory.scrollTop(chatHistory[0].scrollHeight);
        }

        return messageDiv;
    }

    // Handle chat form submission
    $('#chat-form').on('submit', function(e) {
        e.preventDefault();

        const question = $('#question-input').val().trim();
        if (!question) return;

        // Add user message
        addMessage(question, true);
        $('#question-input').val('');
        $('#loading').removeClass('d-none');

        // Send to backend
        $.ajax({
            url: '/api/chat',
            method: 'POST',
            contentType: 'application/json',
            data: JSON.stringify({ query: question }),
            success: function(response) {
                addMessage(response.answer, false, response.sources);
            },
            error: function(xhr) {
                addMessage('Sorry, an error occurred. Please try again.', false);
            },
            complete: function() {
                $('#loading').addClass('d-none');
            }
        });
    });

    // Load conversation history on page load
    $.ajax({
        url: '/api/chat/history',
        method: 'GET',
        success: function(history) {
            history.forEach(msg => {
                addMessage(msg.content, msg.isUser, msg.sources);
            });
        }
    });
});
```

### 10.4 Controller Updates for Thymeleaf Views

#### ChatController additions
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
        // Return conversation history for the logged-in user
        // This would require a ConversationHistory entity and repository
        return new ArrayList<>();
    }
}
```

#### Document Management Controller
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
        // Handle document upload via REST API
        return "redirect:/documents";
    }
}
```

---

## 11. Testing Strategy

### 11.1 Unit Testing UI Components
- Thymeleaf template rendering tests
- JavaScript unit tests using Jest (if adding)
- REST API controller tests

### 11.2 Integration Testing UI
- Full end-to-end tests using Selenium or Cypress
- Testchat flow, document upload, and session management

### 11.3 Performance Testing
- Load testing the jQuery REST API calls
- Database query performance verification
- Pagination testing for document listing

---

## 12. Assumptions & Open Questions

1. **UI Design**: Do you have a preferred color scheme or brand guidelines?
2. **Authentication Method**: Auth via Spring Security with sessions or JWT tokens?
3. **Document Upload Limit**: What is the maximum file size for document uploads?
4. **Chat History Persistence**: Should chat history be saved per user? How long to retain?
5. **Rate Limiting**: Should there be rate limits on chat queries or document uploads?

---

## 13. Final Implementation Checklist

- [ ] Project setup with all dependencies
- [ ] Dual datasource configuration (MySQL + PostgreSQL)
- [ ] Entity definitions for DocumentEntity (MySQL) and DocumentChunkEntity (PostgreSQL)
- [ ] Database schema creation and pgvector extension setup
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