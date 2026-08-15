---
name: ec-rag-chatbot

description: |
  Builds a Retrieval-Augmented Generation (RAG) chatbot for Ecomerce site (EC) policy documents using:
  - Spring AI for embeddings + chat
  - PostgreSQL + pgvector for vector storage
  - Apache Tika for document parsing
  - Anthropic Claude API for LLM responses
  - Complete document ingestion pipeline with chunking and similarity search

metadata:
  type: project
---

# EC RAG Chatbot Skill Implementation Guide

## Overview

This skill implements a standalone Spring Boot application that provides a RAG chatbot for answering questions about Ecomerce site (EC) policy documents. The system combines document ingestion, vector storage, and LLM-powered responses to deliver accurate, source-cited answers.

## Technology Stack

### Core Components:
- **Spring AI**: Embedding generation and LLM integration
- **PostgreSQL + PgVector**: Vector database for semantic search
- **Apache Tika**: Document parsing (PDF, DOCX, HTML, TXT, etc.)
- **Spring Boot**: Application framework
- **Spring Data JPA**: Database access
- **Anthropic Claude**: LLM for generating responses

### Key Features:
- Document ingestion with intelligent chunking
- Vector-based semantic search
- Context-aware AI responses
- Source citation and attribution
- Admin document management

## Implementation Steps

### Step 1: Project Setup

Create the Maven project structure:
```
ec-rag-chatbot/
├── pom.xml
├── src/main/java/com/shopeasy/ecchat/
│   ├── EcRagChatbotApplication.java
│   ├── config/
│   ├── document/
│   ├── model/
│   ├── repository/
│   ├── retrieval/
│   ├── chat/
│   └── util/
└── src/main/resources/
    └── application.yml
```

### Step 2: Maven Dependencies

Configure `pom.xml` with:

**Core Dependencies:**
- `spring-boot-starter-web` - Web framework
- `spring-boot-starter-data-jpa` - Database access
- `postgresql` - PostgreSQL driver

**Spring AI Dependencies:**
- `spring-ai-openai-spring-boot-starter` - Embedding model
- `spring-ai-anthropic-spring-boot-starter` - Chat model
- `spring-ai-vector-store` - Vector database integration

**Document Processing:**
- `tika-core` - Core document parsing
- `tika-parsers` - Format support (PDF, DOCX, HTML, etc.)

**Testing:**
- `spring-boot-starter-test` - Unit and integration tests

### Step 3: Application Configuration

Create `src/main/resources/application.yml`:

```yaml
server:
  port: 8081

spring:
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

  datasource:
    url: jdbc:postgresql://localhost:5432/ec_rag_chatbot
    username: postgres
    password: ${POSTGRES_PASSWORD}

  jpa:
    hibernate:
      ddl-auto: update
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
        jdbc:
          lob:
            non_contextual_creation: true
```

### Step 4: Database Models

#### Document Entity (Source Documents)
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
    private String fileName;

    private String url; // or local path

    @Column(name = "uploaded_at", nullable = false)
    private LocalDateTime uploadedAt;

    // Constructors, getters, setters
}
```

#### Document Chunk Entity (Vector Storage)
```java
@Entity
@Table(name = "ec_document_chunks")
public class DocumentChunkEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id", nullable = false)
    private DocumentEntity document;

    @Lob
    @Column(nullable = false)
    private String content;

    @Column(name = "embedding", columnDefinition = "VECTOR(1536)")
    private float[] embedding;

    @Column(name = "token_count", nullable = false)
    private int tokenCount;

    @Column(name = "chunk_index", nullable = false)
    private int chunkIndex;

    // Constructors, getters, setters
}
```

### Step 5: Configuration Classes

#### Vector Store Configuration
```java
@Configuration
public class VectorStoreConfig {

    @Bean
    public JdbcVectorStore vectorStore(DataSource dataSource) {
        return JdbcVectorStore.builder()
            .dataSource(dataSource)
            .tableName("ec_document_chunks")
            .embeddingDimension(1536)
            .build();
    }
}
```

#### Embedding Configuration
```java
@Configuration
public class EmbeddingConfig {

    @Bean
    public EmbeddingModel embeddingModel() {
        OpenAiEmbeddingOptions embeddingOptions = OpenAiEmbeddingOptions.builder()
            .model(OpenAiEmbeddingModel.EMBEDDING_3_SMALL)
            .dimensions(1536)
            .build();

        return OpenAiEmbeddingModel.builder()
            .apiKey(System.getenv("OPENAI_API_KEY"))
            .embeddingOptions(embeddingOptions)
            .build();
    }
}
```

#### Chat Configuration
```java
@Configuration
public class ChatConfig {

    @Bean
    public ChatModel chatModel() {
        AnthropicChatOptions chatOptions = AnthropicChatOptions.builder()
            .model("claude-3-opus-20240229")
            .maxTokens(1000)
            .temperature(0.7)
            .build();

        return AnthropicChatModel.builder()
            .apiKey(System.getenv("ANTHROPIC_API_KEY"))
            .chatOptions(chatOptions)
            .build();
    }
}
```

### Step 6: Document Processing Components

#### Apache Tika Document Parser
```java
@Component
public class TikaDocumentParser {

    private final AutoDetectParser parser = new AutoDetectParser();

    public String parse(InputStream inputStream, String contentType) throws IOException {
        ParseContext parseContext = new ParseContext();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        parser.parse(inputStream, outputStream, metadata, parseContext);
        return outputStream.toString(StandardCharsets.UTF_8);
    }
}
```

#### Text Chunker
```java
@Component
public class TextChunker {

    private static final int CHUNK_SIZE = 500; // tokens
    private static final int OVERLAP = 100; // tokens

    public List<String> chunkText(String text, int tokenSize) {
        List<String> chunks = new ArrayList<>();
        int totalTokens = estimateTokenCount(text);

        int startToken = 0;
        while (startToken < totalTokens) {
            int endToken = Math.min(startToken + CHUNK_SIZE, totalTokens);
            String chunk = extractTokenRange(text, startToken, endToken);

            if (!chunk.trim().isEmpty()) {
                chunks.add(chunk);
            }

            startToken += CHUNK_SIZE - OVERLAP;
        }

        return chunks;
    }

    private int estimateTokenCount(String text) {
        // Simple approximation: 1 token ≈ 0.75 words
        return (int) Math.ceil(text.split("\\s+").length * 0.75);
    }
}
```

#### Document Ingestor
```java
@Service
public class DocumentIngestor {

    private final TikaDocumentParser parser;
    private final TextChunker chunker;
    private final EmbeddingModel embeddingModel;
    private final DocumentRepository documentRepository;
    private final DocumentChunkRepository chunkRepository;

    @Autowired
    public DocumentIngestor(TikaDocumentParser parser,
                          TextChunker chunker,
                          EmbeddingModel embeddingModel,
                          DocumentRepository documentRepository,
                          DocumentChunkRepository chunkRepository) {
        this.parser = parser;
        this.chunker = chunker;
        this.embeddingModel = embeddingModel;
        this.documentRepository = documentRepository;
        this.chunkRepository = chunkRepository;
    }

    public void ingestDocument(MultipartFile file, String title) throws IOException {
        // Parse document content
        String content = parser.parse(file.getInputStream(), file.getContentType());

        // Create document metadata
        DocumentEntity document = new DocumentEntity();
        document.setTitle(title);
        document.setFileName(file.getOriginalFilename());
        document.setUploadedAt(LocalDateTime.now());
        document = documentRepository.save(document);

        // Chunk the text
        List<String> chunks = chunker.chunkText(content, 500);

        // Process each chunk
        for (int i = 0; i < chunks.size(); i++) {
            String chunkText = chunks.get(i);

            // Generate embedding
            float[] embedding = embeddingModel.embed(chunkText)
                .stream()
                .map(Float::floatValue)
                .toArray();

            // Save chunk with embedding
            DocumentChunkEntity chunk = new DocumentChunkEntity();
            chunk.setDocument(document);
            chunk.setContent(chunkText);
            chunk.setEmbedding(embedding);
            chunk.setTokenCount(estimateTokenCount(chunkText));
            chunk.setChunkIndex(i);

            chunkRepository.save(chunk);
        }
    }

    private int estimateTokenCount(String text) {
        return (int) Math.ceil(text.split("\\s+").length * 0.75);
    }
}
```

### Step 7: Repository Interfaces

#### Document Repository
```java
public interface DocumentRepository extends JpaRepository<DocumentEntity, Long> {
}
```

#### Document Chunk Repository with Vector Search
```java
public interface DocumentChunkRepository extends JpaRepository<DocumentChunkEntity, Long> {

    @Query(value = """
        SELECT * FROM ec_document_chunks 
        ORDER BY 1 - (embedding <=> :embedding) 
        LIMIT :k
    """, nativeQuery = true)
    List<DocumentChunkEntity> findSimilarChunks(@Param("embedding") float[] embedding, 
                                               @Param("k") int k);

    @Query(value = """
        SELECT embedding FROM ec_document_chunks 
        WHERE id = :chunkId
    """, nativeQuery = true)
    float[] findEmbeddingById(@Param("chunkId") Long chunkId);
}
```

### Step 8: Retrieval Service

```java
@Service
public class ContextRetriever {

    private final DocumentChunkRepository chunkRepository;
    private final EmbeddingModel embeddingModel;

    @Autowired
    public ContextRetriever(DocumentChunkRepository chunkRepository,
                          EmbeddingModel embeddingModel) {
        this.chunkRepository = chunkRepository;
        this.embeddingModel = embeddingModel;
    }

    public List<DocumentChunkEntity> retrieveSimilarChunks(String query, int k) {
        // Generate embedding for query
        float[] queryEmbedding = embeddingModel.embed(query)
            .stream()
            .map(Float::floatValue)
            .toArray();

        // Find similar chunks
        return chunkRepository.findSimilarChunks(queryEmbedding, k);
    }

    public List<DocumentChunkEntity> retrieveSimilarChunks(List<String> chunks, int k) {
        // Combine multiple chunks into a single query for better context
        String combinedQuery = String.join(" \n \n ", chunks);
        return retrieveSimilarChunks(combinedQuery, k);
    }
}
```

### Step 9: Chat Service

```java
@Service
public class ChatService {

    private final ContextRetriever retriever;
    private final ChatModel chatModel;

    @Autowired
    public ChatService(ContextRetriever retriever, ChatModel chatModel) {
        this.retriever = retriever;
        this.chatModel = chatModel;
    }

    public ChatResponse chat(ChatRequest request) {
        // Retrieve relevant chunks
        List<DocumentChunkEntity> relevantChunks = retriever.retrieveSimilarChunks(
            request.getQuery(), 5 // Retrieve top 5 chunks
        );

        // Prepare context from retrieved chunks
        String context = relevantChunks.stream()
            .map(DocumentChunkEntity::getContent)
            .collect(Collectors.joining("\n\n"));

        // Create prompt with context and query
        String systemPrompt = """
        Answer the following question using only the provided context.

        Context:
        %s

        Question:
        %s

        If the answer isn't found in the context, say "I couldn't find relevant info."
        """.formatted(context, request.getQuery());

        // Generate response using Claude
        ChatResponse anthropicResponse = chatModel.call(
            Prompt.of(systemPrompt)
        );

        // Build response with sources
        ChatResponse response = new ChatResponse();
        response.setAnswer(anthropicResponse.getResult().getOutput().getText());

        List<Source> sources = relevantChunks.stream()
            .map(chunk -> {
                Source source = new Source();
                source.setDocumentTitle(chunk.getDocument().getTitle());
                source.setChunkId(chunk.getId());
                source.setText(chunk.getContent());
                source.setScore(calculateSimilarityScore(chunk));
                return source;
            })
            .collect(Collectors.toList());

        response.setSources(sources);
        return response;
    }

    private double calculateSimilarityScore(DocumentChunkEntity chunk) {
        // Calculate a relevance score based on embedding similarity
        // This is a placeholder - implement actual similarity calculation
        return 0.85; // Placeholder score
    }
}
```

### Step 10: Controller Layer

#### Chat Controller
```java
@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class ChatController {

    private final ChatService chatService;

    @Autowired
    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping("/chat")
    public ChatResponse chat(@RequestBody ChatRequest request) {
        return chatService.chat(request);
    }

    @GetMapping("/search")
    public List<DocumentChunkEntity> search(@RequestParam String q,
                                          @RequestParam(defaultValue = "5") int k) {
        return chatService.retrieveSimilarChunks(q, k);
    }

    @PostMapping("/admin/ingest")
    public ResponseEntity<Void> ingestDocument(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "title", required = false) String title) {
        try {
            String documentTitle = Optional.ofNullable(title)
                .orElseGet(() -> Optional.ofNullable(file.getOriginalFilename())
                    .orElse("Untitled Document"));

            documentIngestor.ingestDocument(file, documentTitle);
            return ResponseEntity.ok().build();
        } catch (IOException e) {
            return ResponseEntity.badRequest().build();
        }
    }
}
```

#### DTO Classes
```java
public class ChatRequest {
    private String query;
    // Getters and setters
}

public class ChatResponse {
    private String answer;
    private List<Source> sources;
    // Getters and setters
}

public class Source {
    private String documentTitle;
    private Long chunkId;
    private String text;
    private double score;
    // Getters and setters
}
```

### Step 11: Main Application Class

```java
@SpringBootApplication
@EnableConfigurationProperties
public class EcRagChatbotApplication {

    public static void main(String[] args) {
        SpringApplication.run(EcRagChatbotApplication.class, args);
    }

    @Bean
    public CommandLineRunner demo(DocumentRepository documentRepository,
                                 DocumentChunkRepository chunkRepository) {
        return (args) -> {
            // Initialize with sample data if empty
            if (documentRepository.count() == 0) {
                // Add sample EC policy documents
                // This would be done via admin API in production
            }
        };
    }
}
```

### Step 12: Testing

#### Unit Tests
```java
@WebMvcTest(ChatController.class)
class ChatControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ChatService chatService;

    @Test
    void shouldChatWithRetrievedContext() throws Exception {
        ChatRequest request = new ChatRequest();
        request.setQuery("What is the role of EC?");

        ChatResponse mockResponse = new ChatResponse();
        mockResponse.setAnswer("The Ecomerce site is responsible for...");

        when(chatService.chat(any(ChatRequest.class)))
            .thenReturn(mockResponse);

        mockMvc.perform(post("/api/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"query\": \"What is the role of EC?\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.answer").exists());
    }
}
```

#### Integration Tests
```java
@SpringBootTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Sql(scripts = "classpath:test-data.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class ChatServiceIntegrationTest {

    @Autowired
    private ChatService chatService;

    @Test
    void shouldChatWithRetrievedContext() {
        ChatRequest request = new ChatRequest();
        request.setQuery("What are the requirements for voter registration?");

        ChatResponse response = chatService.chat(request);

        assertNotNull(response.getAnswer());
        assertFalse(response.getSources().isEmpty());
    }
}
```

## Deployment Instructions

### Prerequisites

1. **PostgreSQL with PgVector Extension**
   ```sql
   CREATE EXTENSION IF NOT EXISTS vector;

   CREATE DATABASE ec_rag_chatbot;
   ```

2. **Environment Variables**
   - `OPENAI_API_KEY`: Your OpenAI API key for embeddings
   - `ANTHROPIC_API_KEY`: Your Anthropic API key for chat
   - `POSTGRES_PASSWORD`: PostgreSQL database password

### Build and Run

```bash
# Build the application
./mvnw clean package

# Run with default PostgreSQL configuration
./mvnw spring-boot:run

# Or run with custom profile
./mvnw spring-boot:run -Dspring-boot.run.profiles=production
```

### Application Properties

For production deployment, create `src/main/resources/application-prod.yml`:

```yaml
server:
  port: 8443
  ssl:
    enabled: true

spring:
  datasource:
    url: jdbc:postgresql://prod-db.example.com:5432/ec_rag_chatbot
    username: ec_user
    password: ${POSTGRES_PASSWORD}

  ai:
    openai:
      api-key: ${OPENAI_API_KEY}
      base-url: https://api.openai.com

    anthropic:
      api-key: ${ANTHROPIC_API_KEY}
```

### Monitoring and Maintenance

#### Health Check Endpoint
```bash
curl http://localhost:8081/actuator/health
```

#### Database Status
```sql
-- Check table sizes
SELECT schemaname, tablename, pg_size_pretty(pg_total_relation_size(schemaname||'.'||tablename))
FROM pg_tables WHERE schemaname = 'public';

-- Check vector index usage
SELECT * FROM pg_stat_user_indexes WHERE indexrelname LIKE '%vector%';
```

## Security Considerations

1. **Document Upload Security**
   - Validate file types and sizes
   - Scan documents for malicious content
   - Implement file type restrictions

2. **API Security**
   - Use HTTPS in production
   - Implement API rate limiting
   - Add authentication/authorization for admin endpoints

3. **Data Privacy**
   - Store embeddings separately from raw text
   - Implement access controls for sensitive documents
   - Consider data retention policies

## Troubleshooting

### Common Issues and Solutions

1. **Embedding Generation Failed**
   ```
   Cause: Invalid OpenAI API key or quota exceeded
   Solution: Check API key and usage limits
   ```

2. **PostgreSQL Connection Failed**
   ```
   Cause: Database not accessible or PgVector extension missing
   Solution: Verify database setup and extension installation
   ```

3. **Document Parsing Failed**
   ```
   Cause: Unsupported file format or corrupted document
   Solution: Check file format support and file integrity
   ```

### Debug Commands

```bash
# Check application logs
./mvnw spring-boot:run | grep -i error

# Check database connectivity
psql -h localhost -U postgres -d ec_rag_chatbot -c "SELECT 1;"

# Verify PgVector extension
psql -h localhost -U postgres -d ec_rag_chatbot -c "CREATE EXTENSION IF NOT EXISTS vector;"
```

## Performance Optimization

### Vector Search Optimization

1. **Indexing**
   ```sql
   CREATE INDEX idx_document_chunks_embedding ON ec_document_chunks 
       USING ivfflat (embedding vector_cosine_ops) WITH (lists = 100);
   ```

2. **Query Optimization**
   - Pre-filter by document category when possible
   - Adjust search k (top-k) based on use case
   - Consider caching frequently requested queries

### Document Processing Optimization

1. **Parallel Processing**
   - Process multiple documents concurrently
   - Use streaming for large document uploads

2. **Memory Management**
   - Implement chunk-based processing for large files
   - Use efficient data structures for embeddings

## Future Enhancements

1. **Advanced Search Features**
   - Boolean search operators
   - Date-based filtering
   - Document category tagging

2. **User Experience Improvements**
   - Chat interface with conversation history
   - Document preview and download
   - Custom query templates

3. **Advanced Features**
   - Multi-language support
   - Semantic document clustering
   - Automated document categorization

## Conclusion

This implementation provides a complete RAG chatbot solution for Ecomerce site policy documents. The system combines powerful document processing, vector-based semantic search, and AI-powered responses to deliver accurate, source-cited answers to user queries.

The modular architecture allows for easy extension and customization based on specific use cases and requirements.