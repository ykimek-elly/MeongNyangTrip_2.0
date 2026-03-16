package com.team.meongnyang.ai.config;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;


@Configuration
public class RagConfig {
//
//  @Bean
//  public VectorStore vectorStore(JdbcTemplate jdbcTemplate,
//                                 EmbeddingModel embeddingModel) {
//
//    return PgVectorStore.builder(jdbcTemplate, embeddingModel)
//            .dimensions(3072)
//            .indexType(PgVectorStore.PgIndexType.HNSW)
//            .build();
//  }
}
