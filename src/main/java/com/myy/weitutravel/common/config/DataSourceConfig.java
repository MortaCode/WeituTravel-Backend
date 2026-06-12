package com.myy.weitutravel.common.config;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

import static org.springframework.ai.vectorstore.pgvector.PgVectorStore.PgDistanceType.COSINE_DISTANCE;
import static org.springframework.ai.vectorstore.pgvector.PgVectorStore.PgIndexType.HNSW;

@Configuration
public class DataSourceConfig {

    // MySQL 数据源（主数据源）
    @Primary
    @Bean(name = "mysqlDataSource")
    //@ConfigurationProperties(prefix = "spring.datasource")
    public DataSource mysqlDataSource() {
        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setJdbcUrl("jdbc:mysql://localhost:3306/travel?useSSL=false&serverTimezone=Asia/Shanghai&characterEncoding=utf8&allowPublicKeyRetrieval=true");
        dataSource.setUsername("root");
        dataSource.setPassword("187369");
        dataSource.setDriverClassName("com.mysql.cj.jdbc.Driver");
        return dataSource;
        //return DataSourceBuilder.create().type(HikariDataSource.class).build();
    }

    @Primary
    @Bean(name = "mysqlJdbcTemplate")
    public JdbcTemplate mysqlJdbcTemplate() {
        return new JdbcTemplate(mysqlDataSource());
    }

//    // PostgreSQL 数据源（向量数据源）
//    @Bean(name = "postgresqlDataSource")
//    //@ConfigurationProperties(prefix = "spring.vector.datasource")
//    public DataSource postgresqlDataSource() {
//        HikariDataSource dataSource = new HikariDataSource();
//        dataSource.setJdbcUrl("jdbc:postgresql://pgm-uf65on0550i89mys9o.pg.rds.aliyuncs.com:5432/MyAgent");
//        dataSource.setUsername("MyAgent");
//        dataSource.setPassword("Ju187369");
//        dataSource.setDriverClassName("org.postgresql.Driver");
//        return dataSource;
//    }
//
//    @Bean(name = "postgresqlJdbcTemplate")
//    public JdbcTemplate postgresqlJdbcTemplate() {
//        return new JdbcTemplate(postgresqlDataSource());
//    }
//
//    // PGVector Store - 使用 PostgreSQL 数据源
//    @Bean
//    public VectorStore vectorStore(EmbeddingModel dashscopeEmbeddingModel) {
//        return PgVectorStore.builder(postgresqlJdbcTemplate(), dashscopeEmbeddingModel)
//                .dimensions(1024)                    // 不要盲目设置
//                .distanceType(COSINE_DISTANCE)       // Optional: defaults to COSINE_DISTANCE
//                .indexType(HNSW)                     // Optional: defaults to HNSW
//                .initializeSchema(true)              // Optional: defaults to false
//                .schemaName("public")                // Optional: defaults to "public"
//                .vectorTableName("vector_store")     // Optional: defaults to "vector_store"
//                .maxDocumentBatchSize(10000)         // Optional: defaults to 10000
//                .build();
//    }
}