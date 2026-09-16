package com.algotalk.userservice.service;

import org.springframework.web.multipart.MultipartFile;

public interface IS3Service {
    String uploadProfileImg(Long userId, MultipartFile file) throws Exception;
    void deleteProfileImg(String fileUrl) throws Exception;
    String getProfileImgUrl(String fileUrl) throws Exception;
}
