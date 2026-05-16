package com.algotalk.userservice.service;

import com.algotalk.userservice.dto.command.CsCategoryCommand;
import com.algotalk.userservice.dto.response.CsCategoryResponseDTO;

import java.util.List;

public interface ICsCategoryService {
    List<CsCategoryResponseDTO> getCsCategories() throws Exception;
}
