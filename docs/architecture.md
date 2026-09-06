# Architecture

## Document Service

```mermaid
flowchart LR
    Client --> API[Document Controller]
    API --> Upload[DocumentUploadService]
    Upload --> FS[FileStorage]
    Upload --> DB[(PostgreSQL)]
    DB --> Outbox[(outbox_events)]
    Outbox --> Publisher[OutboxPublisherService]
    Publisher --> Kafka[(Kafka topic: knowledge.documents.v1)]
```

## Upload sequence

```mermaid
sequenceDiagram
    participant C as Client
    participant A as Document Controller
    participant U as Upload Service
    participant S as File Storage
    participant D as PostgreSQL
    participant P as Outbox Publisher
    participant K as Kafka

    C->>A: multipart file upload
    A->>U: validate and store
    U->>S: write file
    U->>D: save document + outbox row
    U-->>C: 202 Accepted
    P->>D: claim unpublished rows
    P->>K: publish DocumentUploadedV1
    P->>D: set published_at
```

## RAG service

```mermaid
sequenceDiagram
    participant C as Client (UI)
    participant R as RagChatController
    participant M as ChatMemoryProvider
    participant E as EmbeddingModel (stub/azure)
    participant V as PgVectorChunkRetriever
    participant L as ChatLlm (stub/azure)

    C->>R: POST /api/v1/rag/chat {conversationId?, question}
    R->>M: resolve conversation + load history, append USER msg
    R->>E: embed(question)
    R->>V: cosine top-K over indexing_service.document_chunks (read-only)
    R-->>C: META, then SOURCES
    R->>L: stream(context = system + chunks + history + question)
    L-->>C: DELTA* (token fragments)
    R->>M: append ASSISTANT msg (also on error, partial)
    R-->>C: DONE {embeddingMs, retrievalMs, llmMs, totalMs}
```

- `POST /api/v1/rag/chat` streams NDJSON; `POST /api/v1/rag/search` returns ranked
  sources without calling the LLM (retrieval is demonstrable and testable alone).
- Embedding and LLM are seams selected by `knowledge-platform.{embedding,llm}.provider`
  (`stub` default, `azure` placeholder). The stub LLM is deterministic and quotes the
  retrieved chunks with their source ids.
- rag-service reads `indexing_service.document_chunks` **read-only** for retrieval; it
  never writes or migrates that table. Clean-architecture alternative (an
  indexing-service `/search` API or published read model) is left as a TODO.

## Notes

- The upload transaction never publishes Kafka directly
- `published_at` marks successful publication
- The design accepts at-least-once delivery and duplicates
- `FileStorage` is intentionally abstract so S3 can replace local storage later
