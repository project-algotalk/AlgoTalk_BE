package com.algotalk.userservice.repository;

import com.algotalk.userservice.dto.command.AdminUserCommand;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface IAdminUserMapper {

    AdminUserCommand getUserById(Long userId) throws Exception;
}