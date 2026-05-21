//package com.myy.weitutravel.chat.service.retriever;
//
//import com.myy.weitutravel.chat.service.handler.MarkdownReaderHandler;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.ai.document.Document;
//import org.springframework.stereotype.Service;
//
//import java.util.List;
//import java.util.stream.Collectors;
//
//@Slf4j
//@Service
//@RequiredArgsConstructor
//public class HierarchicalRetrieverService {
//
//    private final HybridSearchService hybridSearchService;
//    private final MarkdownReaderHandler markdownReaderHandler;
//
//    /**
//     * 检索相关文档（使用混合检索）
//     */
//    public String retrieveRelevantContext(String query, int topK) {
//        try {
//            List<Document> documents = hybridSearchService.hybridSearch(query, topK);
//
//            if (documents.isEmpty()) {
//                log.debug("未找到相关文档");
//                return "";
//            }
//
//            // 构建上下文，包含文档来源和分数信息
//            String context = documents.stream()
//                    .map(doc -> {
//                        StringBuilder sb = new StringBuilder();
//                        sb.append("[来源: ").append(doc.getId()).append("]\n");
//
//                        // 如果有标题信息，添加进去
//                        if (doc.getMetadata().containsKey("title")) {
//                            sb.append("标题: ").append(doc.getMetadata().get("title")).append("\n");
//                        }
//
//                        sb.append(doc.getText());
//                        return sb.toString();
//                    })
//                    .collect(Collectors.joining("\n\n---\n\n"));
//
//            log.info("混合检索到 {} 个相关文档", documents.size());
//            return context;
//
//        } catch (Exception e) {
//            log.error("知识库检索失败", e);
//            return "";
//        }
//    }
//
//    /**
//     * 检索并返回完整文档对象
//     */
//    public List<Document> retrieveDocuments(String query, int topK) {
//        return hybridSearchService.hybridSearch(query, topK);
//    }
//
//    /**
//     * 添加文档到知识库
//     */
//    public void addDocuments() {
//        List<Document> documents = markdownReaderHandler.loadMarkdown();
//        hybridSearchService.addDocuments(documents);
//        log.info("添加 {} 个文档到混合检索知识库", documents.size());
//    }
//
//    /**
//     * 手动添加单个文档
//     */
//    public void addDocument(Document document) {
//        hybridSearchService.addDocument(document);
//    }
//}
