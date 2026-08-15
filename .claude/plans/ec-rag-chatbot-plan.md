---
name: ec-rag-chatbot-plan
description: Implementation plan for a RAG-based chatbot on EC policy using Spring AI
metadata:
  type: project
---

# RAG Chatbot for Ecommerce site Policy — Implementation Plan

## 1. Context

Build a **standalone Spring Boot application** serving a **RAG (Retrieval-Augmented Generation) chatbot** answering user queries about **Ecommerce site (EC) policy** documents.

**Key technologies:**
- Java 17 + Spring Boot 3.2.x
- Spring AI for embeddings + chat
- PostgreSQL + `pgvector` for vector storage
- Apache Tika for document parsing
- Anthropic Claude API for LLM responses

**Scope:** This will be a **new microservice**, separate from the existing ShopEasy e-commerce app, to avoid coupling complexity.

---

## 2. Project Structure

```
ec-rag-chatbot/
├── pom.xml
├── src/main/java/com/shopeasy/ecchat/
│   ├── EcRagChatbotApplication.java
│   ├── config/
│   │   ├── VectorStoreConfig.java        # PgVector config
│   │   ├── EmbeddingConfig.java          # Embedding model (OpenAI)
│   │   └── ChatConfig.java               # Chat client (Anthropic)
│   ├── document/
│   │   ├── DocumentIngestor.java         # Orchestrates parsing + chunking + embedding
│   │   ├── TikaDocumentParser.java       # Parses PDF/Word/TXT/HTML via Apache Tika
│   │   └── TextChunker.java              # Splits text into chunks (token-based)
│   ├── model/
│   │   ├── DocumentEntity.java           # JPA entity for source docs
│   │   └── DocumentChunkEntity.java      # JPA entity with embedding vector
│   ├── repository/
│   │   └── DocumentChunkRepository.java  # Spring Data with similarity search
│   ├── retrieval/
│   │   └── ContextRetriever.java         # Top-k similarity search
│   ├── chat/
│   │   ├── ChatController.java           # REST endpoint for chat
│   │   ├── ChatService.java              # Combines retrieval + LLM call
│   │   └──dto/
│   │       ├── ChatRequest.java
│   │       ├── ChatResponse.java
│   │       └── IngestRequest.java
│   └── util/
│       └── TokenUtils.java               # Estimate token count (tiktoken or regex-based)
└── src/main/resources/
    └── application.yml
└── src/test/java/.../EcRagChatbotApplicationTests.java
```

---

## 3. Maven Dependencies

### Core Stack
```xml
<dependencies>
  <!-- Spring Boot Starter -->
  <dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
  </dependency>

  <!-- Spring Data JPA -->
  <dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa</artifactId>
  </dependency>

  <!-- PostgreSQL Driver -->
  <dependency>
    <groupId>org.postgresql</groupId>
    <artifactId>postgresql</artifactId>
    <scope>runtime</scope>
  </dependency>
</dependencies>
```

### Spring AI
```xml
<!-- Spring AI BOM -->
<dependencyManagement>
  <dependencies>
    <dependency>
      <groupId>org.springframework.ai</groupId>
      <artifactId>spring-ai-bom</artifactId>
      <version>1.0.0-SNAPSHOT</version>
      <type>pom</type>
      <scope>import</scope>
    </dependency>
  </dependencies>
</dependencyManagement>

<!-- Embeddings (OpenAI-compatible, works with local Nomic or Sentence Transformers) -->
<dependency>
  <groupId>org.springframework.ai</groupId>
  <artifactId>spring-ai-openai-spring-boot-starter</artifactId>
</dependency>

<!-- Chat (Anthropic) -->
<dependency>
  <groupId>org.springframework.ai</groupId>
  <artifactId>spring-ai-anthropic-spring-boot-starter</artifactId>
</dependency>

<!-- JDBC Vector Store (for similarity search via pgvector) -->
<dependency>
  <groupId>org.springframework.ai</groupId>
  <artifactId>spring-ai-vector-store</artifactId>
</dependency>
```

### Apache Tika
```xml
<dependency>
  <groupId>org.apache.tika</groupId>
  <artifactId>tika-core</artifactId>
  <version>2.9.2</version>
</dependency>
<dependency>
  <groupId>org.apache.tika</groupId>
  <artifactId>tika-parsers</artifactId>
  <version>2.9.2</version>
</dependency>
```

### Testing
```xml
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-test</artifactId>
  <scope>test</scope>
</dependency>
```

---

## 4. Data Model

### `DocumentEntity` (source documents metadata)
```java
@Entity
@Table(name = "ec_documents")
public class DocumentEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String title;
    private String fileName;
    private String url;           // or local path
    private LocalDateTime uploadedAt;
}
```

### `DocumentChunkEntity` (chunked content + embedding)
```java
@Entity
@Table(name = "ec_document_chunks")
public class DocumentChunkEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = LAZY)
    private DocumentEntity document;

    @Lob
    private String content;

    @Column(name = "embedding", columnDefinition = "VECTOR(1536)") // Depends on chosen model
    private float[] embedding;
}
```

---

## 5. Document Ingestion Pipeline

### Flow:
1. Accept document via admin endpoint (`POST /api/admin/ingest`)
2. Parse using Apache Tika (PDF, DOCX, HTML, etc.)
3. Chunk using sliding window approach:
   - **Chunk size:** ~500 tokens
   - **Overlap:** ~100 tokens (~20% overlap)
4. Generate embeddings using OpenAI/Sentence Transformer
5. Store chunks in PostgreSQL with `pgvector`

### Components:
- **`TikaDocumentParser`**: Uses `org.apache.tika.parser.ParseContext` and `AutoDetectParser`
- **`TextChunker`**: Splits text into fixed-size chunks with overlap
- **`DocumentIngestor`**: Coordinates parsing → chunking → embedding → persistence

---

## 6. Embedding Generation Approach

Use **Spring AI’s `EmbeddingModel` abstraction** with OpenAI embeddings:

```yaml
spring:
  ai:
    openai:
      api-key: ${OPENAI_API_KEY}
      embedding:
        model: text-embedding-3-small
        dimensions: 1536
      base-url: https://api.openai.com
```

Alternative: Local sentence-transformer model (e.g., all-minilm-l6-v2) via `spring-ai-onnx` starter if avoiding cloud APIs.

---

## 7. Retriever Endpoint Design

**Endpoint:** `GET /api/search?q=<query>&k=5`

**Logic:**
1. Embed query using same embedding model
2. Use ` PgVector` similarity search (`DocumentChunkRepository`)
3. Return top-k chunks with cosine similarity scores

```java
public List<DocumentChunkEntity> findSimilarChunks(float[] queryEmbedding, int k) {
    return jdbcTemplate.query(
        "SELECT * FROM ec_document_chunks " +
        "ORDER BY 1 - (embedding <=> ?) LIMIT ?",
        (rs, rowNum) -> mapRow(rs),
        arrayToSqlArray(queryEmbedding), k
    );
}
```

---

## 8. Chat Endpoint Design

**Endpoint:** `POST /api/chat`

**Request Body:**
```json
{
  "query": "What is the role of the Ecommerce site in Bangladesh?"
}
```

**Response Body:**
```json
{
  "answer": "The Ecommerce site of Bangladesh ...",
  "sources": [
    {
      "documentTitle": "EC Act 1950",
      "chunkId": 123,
      "text": "...",
      "score": 0.87
    }
  ]
}
```

**Flow:**
1. Accept user query
2. Retrieve top-k relevant chunks using retriever
3. Format prompt:
   ```
   Answer the following question using only the provided context:
   
   Context:
   {chunk1}
   {chunk2}
   ...

   Question:
   {user_query}

   If the answer isn't found in the context, say “I couldn’t find relevant info.”
   ```
4. Call Anthropic Claude API via Spring AI
5. Return formatted response with sources

---

## 9. Integration Strategy

This will be a **completely separate Spring Boot application** (`ec-rag-chatbot`) from ShopEasy. They will share nothing at build/runtime unless explicitly connected later via REST/gateway.

Rationale:
- Keeps concerns isolated
- Allows independent scaling/deployment
- Avoids bloating ShopEasy’s e-commerce focus

Future integration options:
- Shared gateway layer
- Authentication sharing via OAuth2/JWT
- Admin portal reuse (ShopEasy admin UI can call both backends)

---

## 10. Testing Plan

| Layer         | Approach |
|---------------|----------|
| Unit Tests    | Mock `EmbeddingModel`, test `TextChunker`, mock repository searches |
| Integration    | Run PostgreSQL with `pgvector` extension; ingest real sample docs and verify search/chat flows |
| E2E Tests     | Spin up full app; test `/api/ingest`, `/api/search`, `/api/chat` with real EC documents |
| Manual QA     | Human-in-the-loop validation of factual accuracy and source attribution |

Use `@SpringBootTest` with testcontainers for DB, and mock cloud APIs where needed.

---

## 11. Assumptions & Open Questions

1. Where will EC policy documents come from? (e.g., local upload, public URLs like ecBangladesh.gov.in)
2. Will this start as read-only Q&A, or include moderation/admin features?
3. Should responses cite specific sections or just provide summaries?
4. Any preferred embedding model (OpenAI vs. local)?

Will clarify before implementation begins.
