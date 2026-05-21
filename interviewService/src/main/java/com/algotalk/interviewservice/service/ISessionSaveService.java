package com.algotalk.interviewservice.service;

import com.algotalk.interviewservice.dto.command.SessionCreateCommand;
import com.algotalk.interviewservice.dto.feign.AiQuestionItemDTO;
import com.algotalk.interviewservice.dto.response.SessionCreateResponseDTO;

import java.util.List;

public interface ISessionSaveService {

    // 면접 세션 저장(InterviewSessionService 내부에서만 사용하는 메서드)
    SessionCreateResponseDTO saveSession(SessionCreateCommand pCommand,
                                         List<AiQuestionItemDTO> aiQuestions) throws Exception;
}