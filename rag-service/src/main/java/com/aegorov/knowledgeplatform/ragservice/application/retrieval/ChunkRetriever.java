package com.aegorov.knowledgeplatform.ragservice.application.retrieval;

import java.util.List;

/**
 * Read-only similarity search over indexed document chunks.
 * <p>
 * The chunk + embedding data is OWNED by indexing-service
 * ({@code indexing_service.document_chunks}). rag-service only READS it, through
 * its own retrieval infrastructure, and never writes or migrates that table.
 * A cleaner long-term boundary would have indexing-service expose a search API
 * or publish a read model; that is a deliberate future TODO for this demo.
 */
public interface ChunkRetriever {

    List<RetrievedChunk> search(float[] queryEmbedding, int topK);
}
