package com.myy.weitutravel.chat.service.handler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.markdown.MarkdownDocumentReader;
import org.springframework.ai.reader.markdown.config.MarkdownDocumentReaderConfig;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class MarkdownReaderHandler {


    private final ResourcePatternResolver resourcePatternResolver;
    private final RecursiveSplitHandler recursiveSplitHandler;

    public MarkdownReaderHandler(ResourcePatternResolver resourcePatternResolver, RecursiveSplitHandler recursiveSplitHandler) {
        this.resourcePatternResolver = resourcePatternResolver;
        this.recursiveSplitHandler = recursiveSplitHandler;
    }

    public List<Document> loadMarkdown() {
        List<Document> list = new ArrayList<>();
        try {
            Resource[] resources = resourcePatternResolver.getResources("classpath:document/*.md");
            for (Resource resource : resources){
                String filename = resource.getFilename();
                MarkdownDocumentReaderConfig config = MarkdownDocumentReaderConfig.builder()
                        .withHorizontalRuleCreateDocument(true)
                        .withIncludeCodeBlock(true)
                        .withIncludeBlockquote(true)
                        .withAdditionalMetadata("filename", filename)
                        .build();
                MarkdownDocumentReader reader = new MarkdownDocumentReader(resource, config);
                //超过目标Token大小，递归分块
                List<Document> splitDocument = recursiveSplitHandler.recursiveSplit(reader.get());
                list.addAll(splitDocument);
            }
        } catch (IOException e) {
            log.error("Markdown加载失败={}", e);
            throw new RuntimeException(e);
        }
        return list;
    }
}
