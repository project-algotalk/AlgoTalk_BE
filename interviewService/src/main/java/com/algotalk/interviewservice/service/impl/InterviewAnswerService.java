package com.algotalk.interviewservice.service.impl;

import com.algotalk.interviewservice.domain.InterviewAnalysisDocument;
import com.algotalk.interviewservice.dto.command.InterviewAnswerCommand;
import com.algotalk.interviewservice.persistence.mongodb.IInterviewAnalysisMapper;
import com.algotalk.interviewservice.service.IInterviewAnswerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class InterviewAnswerService implements IInterviewAnswerService {

    private final IInterviewAnalysisMapper interviewAnalysisMapper;

    @Override
    public void saveAnswer(InterviewAnswerCommand pCommand) throws Exception {
        log.info("{}.saveAnswer Start!", this.getClass().getName());

        InterviewAnalysisDocument rDoc = InterviewAnalysisDocument.from(pCommand);

        interviewAnalysisMapper.insertData(rDoc);

        log.info("{}.saveAnswer End!", this.getClass().getName());
    }
}