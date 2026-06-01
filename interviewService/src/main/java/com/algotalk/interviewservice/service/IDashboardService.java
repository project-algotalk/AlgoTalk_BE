package com.algotalk.interviewservice.service;

import com.algotalk.interviewservice.dto.response.DashboardResponseDTO;

public interface IDashboardService {
    DashboardResponseDTO getDashboard(Long userId, Integer page, Integer size);
}