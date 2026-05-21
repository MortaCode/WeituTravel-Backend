//package com.myy.weitutravel.chat.service.retriever;
//
//import com.myy.weitutravel.chat.service.retriever.Bm25Retriever;
//import com.myy.weitutravel.common.config.HybridRetrievalConfig;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.ai.document.Document;
//import org.springframework.ai.vectorstore.SearchRequest;
//import org.springframework.ai.vectorstore.VectorStore;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.stereotype.Service;
//
//import java.util.*;
//import java.util.stream.Collectors;
//
//@Slf4j
//@Service
//public class HybridSearchService {
//
//    @Autowired
//    private VectorStore vectorStore;
//
//    @Autowired
//    private Bm25Retriever bm25Retriever;
//
//    @Autowired
//    private HybridRetrievalConfig config;
//
//    /**
//     * 混合检索 - 主要接口
//     * @param query 查询文本
//     * @return 检索结果（已排序和过滤）
//     */
//    public List<Document> hybridSearch(String query) {
//        return hybridSearch(query, config.getFinalTopK());
//    }
//
//    /**
//     * 混合检索 - 带topK参数
//     */
//    public List<Document> hybridSearch(String query, int topK) {
//        if (query == null || query.trim().isEmpty()) {
//            return Collections.emptyList();
//        }
//
//        long startTime = System.currentTimeMillis();
//
//        // 1. 执行检索
//        List<VectorSearchResult> vectorResults = performVectorSearch(query);  //向量检索
//        List<Bm25Retriever.Bm25SearchResult> bm25Results = performBm25Search(query);//BM25检索
//
//        // 2. 分数融合
//        Map<String, HybridSearchResult> fusedResults = fuseScores(vectorResults, bm25Results);
//
//        // 3. 排序并过滤
//        List<Document> finalResults = fusedResults.values().stream()
//                .sorted((a, b) -> Double.compare(b.getFinalScore(), a.getFinalScore()))
//                .filter(result -> result.getFinalScore() >= config.getRelevanceThreshold())
//                .limit(topK)
//                .map(HybridSearchResult::getDocument)
//                .collect(Collectors.toList());
//
//        log.info("混合检索完成 - 查询: '{}', 原始结果: vector={}, bm25={}, 最终结果: {}, 耗时: {}ms",
//                query, vectorResults.size(), bm25Results.size(),
//                finalResults.size(), System.currentTimeMillis() - startTime);
//
//        return finalResults;
//    }
//
//    /**
//     * 向量检索
//     */
//    private List<VectorSearchResult> performVectorSearch(String query) {
//        try {
//            SearchRequest searchRequest = SearchRequest.builder()
//                    .query(query)
//                    .topK(config.getVectorTopK())
//                    .similarityThreshold(0.5)  // 最低相似度阈值
//                    .build();
//
//            List<Document> documents = vectorStore.similaritySearch(searchRequest);
//
//            // 计算归一化分数
//            double maxScore = documents.stream()
//                    .mapToDouble(doc -> {
//                        Double score = doc.getMetadata().get("similarity_score") instanceof Double ?
//                                (Double) doc.getMetadata().get("similarity_score") : 0.0;
//                        return score;
//                    })
//                    .max()
//                    .orElse(1.0);
//
//            return documents.stream()
//                    .map(doc -> {
//                        double rawScore = doc.getMetadata().get("similarity_score") instanceof Double ?
//                                (Double) doc.getMetadata().get("similarity_score") : 0.0;
//                        double normalizedScore = maxScore > 0 ? rawScore / maxScore : rawScore;
//                        return new VectorSearchResult(doc, normalizedScore, rawScore);
//                    })
//                    .collect(Collectors.toList());
//
//        } catch (Exception e) {
//            log.error("向量检索失败", e);
//            return Collections.emptyList();
//        }
//    }
//
//    /**
//     * BM25检索
//     */
//    private List<Bm25Retriever.Bm25SearchResult> performBm25Search(String query) {
//        return bm25Retriever.search(query, config.getBm25TopK());
//    }
//
//    /**
//     * 分数融合
//     * 主要功能：合并两个查询结果 + 加权分数
//     */
//    private Map<String, HybridSearchResult> fuseScores(
//            List<VectorSearchResult> vectorResults,
//            List<Bm25Retriever.Bm25SearchResult> bm25Results) {
//
//        Map<String, HybridSearchResult> resultMap = new HashMap<>();
//
//        // 处理向量检索结果
//        double vectorWeight = config.getVectorWeight();
//        for (VectorSearchResult vr : vectorResults) {
//            String docId = vr.getDocument().getId();
//            double weightedScore = vr.getNormalizedScore() * vectorWeight;
//            resultMap.put(docId, new HybridSearchResult(
//                    vr.getDocument(),
//                    weightedScore,
//                    vr.getNormalizedScore(),
//                    0.0,
//                    vr.getRawScore(),
//                    0.0f
//            ));
//        }
//
//        // 处理BM25检索结果（融合）
//        double bm25Weight = config.getBm25Weight();
//        for (Bm25Retriever.Bm25SearchResult br : bm25Results) {
//            String docId = br.getDocument().getId();
//            double weightedScore = br.getNormalizedScore() * bm25Weight;
//
//            if (resultMap.containsKey(docId)) {
//                // 文档同时被两种方法召回的，累加分数
//                HybridSearchResult existing = resultMap.get(docId);
//                existing.setFinalScore(existing.getFinalScore() + weightedScore);
//                existing.setBm25Score(br.getNormalizedScore());
//                existing.setBm25RawScore(br.getRawScore());
//            } else {
//                resultMap.put(docId, new HybridSearchResult(
//                        br.getDocument(),
//                        weightedScore,
//                        0.0,
//                        br.getNormalizedScore(),
//                        0.0,
//                        br.getRawScore()
//                ));
//            }
//        }
//
//        //应用RRF作为补充
//        applyRRF(resultMap, vectorResults, bm25Results);
//
//        return resultMap;
//    }
//
//    /**
//     * RRF[倒数排序融合]
//     * 计算公式：rrfScore += 1 / (K + locIndex + 1)
//     * 最终得分：olderScore*0.7 + rrfScore*0.3
//     */
//    private void applyRRF(
//            Map<String, HybridSearchResult> resultMap,
//            List<VectorSearchResult> vectorResults,
//            List<Bm25Retriever.Bm25SearchResult> bm25Results) {
//
//        final int K = 60;
//
//        // 构建排名映射
//        Map<String, Integer> vectorRanks = new HashMap<>();
//        for (int i = 0; i < vectorResults.size(); i++) {
//            vectorRanks.put(vectorResults.get(i).getDocument().getId(), i + 1);
//        }
//
//        Map<String, Integer> bm25Ranks = new HashMap<>();
//        for (int i = 0; i < bm25Results.size(); i++) {
//            bm25Ranks.put(bm25Results.get(i).getDocument().getId(), i + 1);
//        }
//
//        // 计算RRF分数
//        for (HybridSearchResult result : resultMap.values()) {
//            String docId = result.getDocument().getId();
//            double rrfScore = 0.0;
//
//            Integer vectorRank = vectorRanks.get(docId);
//            if (vectorRank != null) {
//                rrfScore += 1.0 / (K + vectorRank);
//            }
//
//            Integer bm25Rank = bm25Ranks.get(docId);
//            if (bm25Rank != null) {
//                rrfScore += 1.0 / (K + bm25Rank);
//            }
//
//            // 将RRF分数与加权分数结合
//            result.setFinalScore(result.getFinalScore() * 0.7 + rrfScore * 0.3);
//        }
//    }
//
//    /**
//     * 添加文档到检索系统
//     */
//    public void addDocument(Document document) {
//        vectorStore.add(List.of(document));
//        bm25Retriever.addDocument(document);
//        log.info("文档已添加到混合检索系统: {}", document.getId());
//    }
//
//    public void addDocuments(List<Document> documents) {
//        vectorStore.add(documents);
//        bm25Retriever.addDocuments(documents);
//        log.info("批量添加 {} 个文档到混合检索系统", documents.size());
//    }
//
//
//    @lombok.Data
//    @lombok.AllArgsConstructor
//    private static class VectorSearchResult {
//        private Document document;
//        private double normalizedScore;
//        private double rawScore;
//    }
//
//    @lombok.Data
//    @lombok.AllArgsConstructor
//    private static class HybridSearchResult {
//        private Document document;
//        private double finalScore;
//        private double vectorScore;
//        private double bm25Score;
//        private double vectorRawScore;
//        private float bm25RawScore;
//    }
//}
