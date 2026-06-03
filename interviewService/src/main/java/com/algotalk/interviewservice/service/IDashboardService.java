package com.algotalk.interviewservice.service;

import com.algotalk.common.pagination.Pagination;
import com.algotalk.interviewservice.dto.response.DashboardResponseDTO;

public interface IDashboardService {
    DashboardResponseDTO getDashboard(Long userId, Pagination pagination);
}