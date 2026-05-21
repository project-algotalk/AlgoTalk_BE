package com.algotalk.interviewservice.service;

import com.algotalk.interviewservice.dto.command.SessionCreateCommand;
import com.algotalk.interviewservice.dto.feign.AiQuestionItemDTO;
import com.algotalk.interviewservice.dto.response.SessionCreateResponseDTO;

import java.util.List;

public interface ISessionSaveService {

    // 면접 세션 및 질문 DB 저장
    SessionCreateResponseDTO saveSession(SessionCreateCommand pCommand,
                                         List<AiQuestionItemDTO> aiQuestions) throws Exception;
}