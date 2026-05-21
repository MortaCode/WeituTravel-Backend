package com.myy.weitutravel.chat.service.handler;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

@Slf4j
@SpringBootTest
class MarkdownReaderHandlerTest {

    @Resource
    MarkdownReaderHandler markdownReaderHandler;

    @Test
    void loadMarkdownTest(){
        List<Document> list =  markdownReaderHandler.loadMarkdown();
        int i=1;
        for (Document document : list){
            log.info("文件={}，内容={}",i, document);
            i++;
        }
    }

}