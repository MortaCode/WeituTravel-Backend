//package com.myy.weitutravel.chat.service.retriever;
//
//import com.myy.weitutravel.common.config.HybridRetrievalConfig;
//import jakarta.annotation.PostConstruct;
//import lombok.extern.slf4j.Slf4j;
//import org.apache.lucene.analysis.standard.StandardAnalyzer;
//import org.apache.lucene.document.*;
//import org.apache.lucene.index.*;
//import org.apache.lucene.queryparser.classic.QueryParser;
//import org.apache.lucene.search.*;
//import org.apache.lucene.store.ByteBuffersDirectory;
//import org.apache.lucene.store.Directory;
//import org.springframework.ai.document.Document;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.stereotype.Component;
//import org.springframework.util.StringUtils;
//
//import java.util.*;
//import java.util.concurrent.ConcurrentHashMap;
//
//@Slf4j
//@Component
//public class Bm25Retriever {
//
////    内存索引：使用 ByteBuffersDirectory 在内存中构建 Lucene 索引，不持久化到磁盘。
////    文档缓存：通过 ConcurrentHashMap<String, Document> 缓存原始文档（org.springframework.ai.document.Document），索引重建时从缓存读取。
////    索引重建：每次新增/批量添加文档时，都会清空现有索引并基于缓存全量重建（见 rebuildIndex()）。
////    分析器：使用 Lucene 的 StandardAnalyzer 进行文本分词。
//
//    private IndexReader indexReader;
//    private IndexSearcher indexSearcher;
//    private StandardAnalyzer analyzer;
//    private Directory directory;
//
//    @Autowired
//    private HybridRetrievalConfig config;
//
//    //存储文档ID到Document对象的映射
//    private final Map<String, Document> documentCache = new ConcurrentHashMap<>();
//
//    @PostConstruct
//    public void init() {
//        try {
//            analyzer = new StandardAnalyzer();
//            directory = new ByteBuffersDirectory();
//            rebuildIndex();
//        } catch (Exception e) {
//            log.error("初始化BM25索引失败", e);
//            throw new RuntimeException("初始化BM25索引失败", e);
//        }
//    }
//
//    /**
//     * 重建索引
//     */
//    public void rebuildIndex() {
//        try {
//            IndexWriterConfig config = new IndexWriterConfig(analyzer);
//            IndexWriter writer = new IndexWriter(directory, config);
//
//            // 清空现有索引
//            writer.deleteAll();
//
//            // 重新添加所有文档（从缓存中）
//            for (Document doc : documentCache.values()) {
//                writer.addDocument(createLuceneDocument(doc));
//            }
//
//            writer.commit();
//            writer.close();
//
//            // 重新打开IndexReader和IndexSearcher
//            if (indexReader != null) {
//                indexReader.close();
//            }
//            indexReader = DirectoryReader.open(directory);
//            indexSearcher = new IndexSearcher(indexReader);
//
//            log.info("BM25索引重建，文件梳理 = {} ", documentCache.size());
//        } catch (Exception e) {
//            log.error("重建BM25索引失败", e);
//            throw new RuntimeException("重建BM25索引失败", e);
//        }
//    }
//
//    /**
//     * 添加文档到BM25索引
//     */
//    public void addDocument(Document document) {
//        try {
//            documentCache.put(document.getId(), document);
//            rebuildIndex();
//        } catch (Exception e) {
//            log.error("添加文档到BM25索引失败", e);
//        }
//    }
//
//    /**
//     * 批量添加文档
//     */
//    public void addDocuments(List<Document> documents) {
//        for (Document doc : documents) {
//            documentCache.put(doc.getId(), doc);
//        }
//        rebuildIndex();
//    }
//
//    /**
//     * BM25检索，返回带BM25分数的Document
//     */
//    public List<Bm25SearchResult> search(String query, int topK) {
//        if (!StringUtils.hasText(query)) {
//            return Collections.emptyList();
//        }
//
//        try {
//            // 转义查询中的特殊字符
//            String escapedQuery = QueryParser.escape(query);
//            QueryParser parser = new QueryParser("content", analyzer);
//            Query luceneQuery = parser.parse(escapedQuery);
//
//            // Lucene 9.x 中 TopDocs 不再有 getMaxScore 方法
//            TopDocs topDocs = indexSearcher.search(luceneQuery, topK);
//
//            List<Bm25SearchResult> results = new ArrayList<>();
//
//            // 获取最大分数的方法1：如果没有显式的分数，使用1.0作为默认最大值
//            float maxScore = 1.0f;
//
//            // 尝试获取最高分（如果scoreDocs不为空）
//            if (topDocs.scoreDocs.length > 0) {
//                // 获取第一个文档的分数作为参考
//                float firstScore = topDocs.scoreDocs[0].score;
//                if (firstScore > 0) {
//                    maxScore = firstScore;
//                }
//            }
//
//            for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
//                org.apache.lucene.document.Document luceneDoc = indexSearcher.doc(scoreDoc.doc);
//                String docId = luceneDoc.get("id");
//                Document originalDoc = documentCache.get(docId);
//
//                if (originalDoc != null) {
//                    // 归一化BM25分数到0-1之间
//                    float normalizedScore = maxScore > 0 ? Math.min(scoreDoc.score / maxScore, 1.0f) : 0.0f;
//                    results.add(new Bm25SearchResult(originalDoc, normalizedScore, scoreDoc.score));
//                }
//            }
//
//            log.debug("BM25检索到 {} 个结果，query = {}", results.size(), query);
//            return results;
//
//        } catch (Exception e) {
//            log.error("BM25检索失败: query = {}", query, e);
//            return Collections.emptyList();
//        }
//    }
//
//    /**
//     * 使用自定义相似度进行检索
//     */
//    public List<Bm25SearchResult> searchWithExplain(String query, int topK) {
//        if (!StringUtils.hasText(query)) {
//            return Collections.emptyList();
//        }
//
//        try {
//            String escapedQuery = QueryParser.escape(query);
//            QueryParser parser = new QueryParser("content", analyzer);
//            Query luceneQuery = parser.parse(escapedQuery);
//
//            // 使用 explain 获取详细的分数信息
//            TopDocs topDocs = indexSearcher.search(luceneQuery, topK);
//
//            List<Bm25SearchResult> results = new ArrayList<>();
//
//            // 收集所有分数用于归一化
//            List<Float> scores = new ArrayList<>();
//            for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
//                scores.add(scoreDoc.score);
//            }
//
//            // 使用 min-max 归一化
//            float minScore = scores.stream().min(Float::compare).orElse(0.0f);
//            float maxScore = scores.stream().max(Float::compare).orElse(1.0f);
//            float range = maxScore - minScore;
//
//            for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
//                org.apache.lucene.document.Document luceneDoc = indexSearcher.doc(scoreDoc.doc);
//                String docId = luceneDoc.get("id");
//                Document originalDoc = documentCache.get(docId);
//
//                if (originalDoc != null) {
//                    // Min-Max 归一化
//                    float normalizedScore;
//                    if (range > 0) {
//                        normalizedScore = (scoreDoc.score - minScore) / range;
//                    } else {
//                        normalizedScore = 1.0f;
//                    }
//                    results.add(new Bm25SearchResult(originalDoc, normalizedScore, scoreDoc.score));
//                }
//            }
//
//            // 按归一化分数排序
//            results.sort((a, b) -> Double.compare(b.getNormalizedScore(), a.getNormalizedScore()));
//
//            log.debug("BM25检索到 {} 个结果 (使用explain模式)", results.size());
//            return results;
//
//        } catch (Exception e) {
//            log.error("BM25检索失败 (使用explain模式)  query = {}", query, e);
//            return Collections.emptyList();
//        }
//    }
//
//    private org.apache.lucene.document.Document createLuceneDocument(Document document) {
//        org.apache.lucene.document.Document luceneDoc = new org.apache.lucene.document.Document();
//        luceneDoc.add(new StringField("id", document.getId(), Field.Store.YES));
//        luceneDoc.add(new TextField("content", document.getText(), Field.Store.YES));
//
//        // 添加元数据字段以支持过滤
//        Map<String, Object> metadata = document.getMetadata();
//        if (metadata != null) {
//            if (metadata.containsKey("headerPath")) {
//                Object headerPathObj = metadata.get("headerPath");
//                if (headerPathObj instanceof List) {
//                    @SuppressWarnings("unchecked")
//                    List<String> headerPath = (List<String>) headerPathObj;
//                    if (headerPath != null && !headerPath.isEmpty()) {
//                        luceneDoc.add(new StringField("headerPath", String.join("/", headerPath), Field.Store.YES));
//                    }
//                }
//            }
//
//            // 添加标题字段
//            if (metadata.containsKey("title")) {
//                String title = metadata.get("title").toString();
//                luceneDoc.add(new TextField("title", title, Field.Store.YES));
//            }
//        }
//
//        return luceneDoc;
//    }
//
//    /**
//     * 关闭资源
//     */
//    public void close() {
//        try {
//            if (indexReader != null) {
//                indexReader.close();
//            }
//            if (directory != null) {
//                directory.close();
//            }
//            if (analyzer != null) {
//                analyzer.close();
//            }
//        } catch (Exception e) {
//            log.error("Failed to close BM25 resources", e);
//        }
//    }
//
//    /**
//     * BM25搜索结果包装类
//     */
//    @lombok.Data
//    @lombok.AllArgsConstructor
//    public static class Bm25SearchResult {
//        private Document document;
//        private double normalizedScore;
//        private float rawScore;
//    }
//}