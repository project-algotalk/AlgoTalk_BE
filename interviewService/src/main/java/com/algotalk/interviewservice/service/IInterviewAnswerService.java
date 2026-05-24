package com.algotalk.interviewservice.service;

import com.algotalk.interviewservice.dto.command.InterviewAnswerCommand;

public interface IInterviewAnswerService {
    void saveAnswer(InterviewAnswerCommand pCommand) throws Exception;
}