package com.algotalk.interviewservice.service;

import com.algotalk.interviewservice.dto.command.SessionCreateCommand;
import com.algotalk.interviewservice.dto.command.SessionResultCommand;
import com.algotalk.interviewservice.dto.response.SessionCreateResponseDTO;
import com.algotalk.interviewservice.dto.response.SessionResultResponseDTO;

public interface IInterviewSessionService {

    // 면접 세션 생성 (llm)
    SessionCreateResponseDTO createSession(SessionCreateCommand pCommand);

    // 면접 세션 생성 (직접 질문 입력)
    SessionCreateResponseDTO createManualSession(SessionCreateCommand pCommand);

    // 면접 세션 결과 조회
    SessionResultResponseDTO getSessionResult(SessionResultCommand pCommand);

    void completeSession(Long userId, Long sessionId);
}
