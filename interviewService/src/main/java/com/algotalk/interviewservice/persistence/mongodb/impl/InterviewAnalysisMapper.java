package com.algotalk.interviewservice.persistence.mongodb.impl;

import com.algotalk.interviewservice.domain.InterviewAnalysisDocument;
import com.algotalk.interviewservice.persistence.mongodb.IInterviewAnalysisMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class InterviewAnalysisMapper implements IInterviewAnalysisMapper {

    private final MongoTemplate mongodb;
    private final ObjectMapper objectMapper;  // ← Spring이 관리하는 ObjectMapper 주입

    private static final String COL_NM = "interview_analysis_report";

    @Override
    public int insertData(InterviewAnalysisDocument pDoc) {
        log.info("{}.insertData Start!", this.getClass().getName());

        // Spring ObjectMapper는 JavaTimeModule이 등록되어 있어서 LocalDateTime 처리 가능
        mongodb.getCollection(COL_NM)
                .insertOne(new Document(objectMapper.convertValue(pDoc, Map.class)));

        log.info("{}.insertData End!", this.getClass().getName());
        return 1;
    }

    @Override
    public Optional<InterviewAnalysisDocument> findBySessionQuestionId(Long sessionQuestionId) {
        log.info("{}.findBySessionQuestionId Start!", this.getClass().getName());

        Document query = new Document("sessionQuestionId", sessionQuestionId);
        Document projection = new Document("_id", 0);

        Document result = mongodb.getCollection(COL_NM)
                .find(query)
                .projection(projection)
                .first();

        if (result == null) {
            log.info("{}.findBySessionQuestionId End! result is null", this.getClass().getName());
            return Optional.empty();
        }

        InterviewAnalysisDocument rDoc = objectMapper
                .convertValue(result, InterviewAnalysisDocument.class);

        log.info("{}.findBySessionQuestionId End!", this.getClass().getName());
        return Optional.of(rDoc);
    }

    @Override
    public List<InterviewAnalysisDocument> findBySessionId(Long sessionId) {
        log.info("{}.findBySessionId Start!", this.getClass().getName());

        Document query = new Document("sessionId", sessionId);
        Document projection = new Document("_id", 0);

        List<InterviewAnalysisDocument> rList = mongodb.getCollection(COL_NM)
                .find(query)
                .projection(projection)
                .map(doc -> objectMapper.convertValue(doc, InterviewAnalysisDocument.class))
                .into(new ArrayList<>());

        log.info("{}.findBySessionId End!", this.getClass().getName());
        return rList;
    }
}