//package com.myy.weitutravel.chat.service.retriever;
//
//import com.myy.weitutravel.chat.service.handler.MarkdownReaderHandler;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.ai.document.Document;
//import org.springframework.ai.vectorstore.SearchRequest;
//import org.springframework.ai.vectorstore.VectorStore;
//import org.springframework.stereotype.Service;
//
//import java.util.List;
//import java.util.stream.Collectors;
//
//@Slf4j
//@Service
//@RequiredArgsConstructor
//public class HierarchicalRetrieverSingleService {
//
//    private final VectorStore vectorStore;
//    private final MarkdownReaderHandler markdownReaderHandler;
//
//    // 检索相关文档
//    public String retrieveRelevantContext(String query, int topK) {
//        try {
//            //构建请求
//            SearchRequest searchRequest = SearchRequest.builder()
//                    .query(query)
//                    .topK(topK)
//                    .build();
//
//            //执行相似度检索请求
//            List<Document> documents = vectorStore.similaritySearch(searchRequest);
//
//            if (documents.isEmpty()) {
//                log.debug("未找到相关文档");
//                return "";
//            }
//
//            String context = documents.stream()
//                    .map(doc -> doc.getText())
//                    .collect(Collectors.joining("\n\n"));
//
//            log.info("检索到 {} 个相关文档", documents.size());
//            return context;
//
//        } catch (Exception e) {
//            log.error("知识库检索失败", e);
//            return "";
//        }
//    }
//
//    // 添加文档到知识库
//    public void addDocuments() {
//        List<Document> documents = markdownReaderHandler.loadMarkdown();
//        vectorStore.add(documents);
//        log.info("添加 {} 个文档到知识库", documents.size());
//    }
//}
