package com.algotalk.userservice.controller;

import com.algotalk.userservice.service.IS3Service;
import jakarta.mail.Multipart;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@RestController
@RequestMapping(value = "/test/s3")
@RequiredArgsConstructor
public class S3TestController {

    private final IS3Service s3Service;

    @PostMapping("/upload-profile-img")
    public ResponseEntity<String> testUploadProfileImg(
            @RequestParam("file")MultipartFile file
            ) throws Exception {
        // 테스트용 메서드 구현

        Long testUserId = 1L; // 테스트용 사용자 ID
        String url = s3Service.uploadProfileImg(testUserId, file);

        return ResponseEntity.ok(url);
    }

    @PostMapping("/delete-profile-img")
    public ResponseEntity<String> testUploadProfileImg(
            @RequestParam("fileUrl") String fileUrl
    ) throws Exception {
        // 테스트용 메서드 구현

        s3Service.deleteProfileImg(fileUrl);

        return ResponseEntity.ok().build();
    }
}
