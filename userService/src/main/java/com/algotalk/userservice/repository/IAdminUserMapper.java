package com.algotalk.userservice.repository;

import com.algotalk.userservice.dto.command.AdminUserCommand;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface IAdminUserMapper {

    List<AdminUserCommand> getUsers(AdminUserCommand command) throws Exception;

    long countUsers(AdminUserCommand command) throws Exception;

    AdminUserCommand getUserByLoginId(
            @Param("loginId") String loginId
    ) throws Exception;
}
