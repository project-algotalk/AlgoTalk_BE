package com.algotalk.userservice.repository;

import com.algotalk.userservice.dto.command.CsCategoryCommand;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface ICsCategoryMapper {
    // 카테고리 조회
    List<CsCategoryCommand> getCsCategories() throws Exception;
}
