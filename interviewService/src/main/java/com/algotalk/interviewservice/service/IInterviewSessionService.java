package com.algotalk.interviewservice.service;

import com.algotalk.interviewservice.dto.command.SessionCreateCommand;
import com.algotalk.interviewservice.dto.response.SessionCreateResponseDTO;

public interface IInterviewSessionService {

    // 면접 세션 생성
    SessionCreateResponseDTO createSession(SessionCreateCommand pCommand) throws Exception;
}
