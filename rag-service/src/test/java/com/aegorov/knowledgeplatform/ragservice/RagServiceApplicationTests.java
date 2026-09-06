package com.aegorov.knowledgeplatform.ragservice;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Boots the full Spring context against a Testcontainers Postgres (pgvector
 * image) and confirms that the R1 Liquibase changelog created the
 * {@code rag_service} schema. This is the R1 smoke test: no domain code
 * exists yet.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class RagServiceApplicationTests {

    @Autowired
    private DataSource dataSource;

    @Test
    void contextLoads() {
        assertThat(dataSource).isNotNull();
    }

    @Test
    void liquibaseCreatesRagServiceSchema() {
        var jdbc = JdbcClient.create(dataSource);
        var schemaExists = jdbc.sql("""
                        SELECT EXISTS (
                            SELECT 1 FROM information_schema.schemata
                            WHERE schema_name = 'rag_service'
                        )
                        """)
                .query(Boolean.class)
                .single();
        assertThat(schemaExists).isTrue();
    }
}
