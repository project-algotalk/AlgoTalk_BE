package com.algotalk.interviewservice.service;

import com.algotalk.interviewservice.dto.command.SessionCreateCommand;
import com.algotalk.interviewservice.dto.response.SessionCreateResponseDTO;

public interface IInterviewSessionService {

    // 면접 세션 생성 (llm)
    SessionCreateResponseDTO createSession(SessionCreateCommand pCommand);

    // 면접 세션 생성 (직접 질문 입력)
    SessionCreateResponseDTO createManualSession(SessionCreateCommand pCommand);
}
