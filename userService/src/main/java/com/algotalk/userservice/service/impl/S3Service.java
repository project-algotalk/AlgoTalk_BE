package com.algotalk.userservice.service.impl;

import com.algotalk.common.exception.BusinessException;
import com.algotalk.userservice.service.IS3Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.net.URI;
import java.time.Duration;
import java.util.Set;
import java.util.UUID;

import static com.algotalk.userservice.exception.UserErrorCode.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class S3Service implements IS3Service {

    @Value("${image.max-size:5242880}")
    private Long maxImgFileSize; // 5MB

    private static final Set<String> ALLOWED_IMG_EXTENSIONS = Set.of(
            "jpg",
            "jpeg",
            "png",
            "bmp",
            "webp"
    );

    private static final Set<String> ALLOWED_IMG_CONTENT_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/bmp",
            "image/webp"
    );

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;

    @Value("${cloud.aws.s3.bucket}")
    private String bucket;

    @Value("${cloud.aws.region.static}")
    private String region;

    @Override
    public String uploadProfileImg(Long userId, MultipartFile file) throws Exception {
        if(file == null || file.isEmpty()) {
            throw new BusinessException(FILE_IS_EMPTY);
        }

        if (file.getSize() > maxImgFileSize) {
            throw new BusinessException(FILE_SIZE_EXCEEDED);
        }

        String originFileName = file.getOriginalFilename();

        if(originFileName == null
                || !originFileName.contains(".")
                || originFileName.endsWith(".")
        ) {
            throw new BusinessException(FILE_TYPE_NOT_ALLOWED);
        }

        // 확장자 검증
        String ext = originFileName
                .substring(originFileName.lastIndexOf(".") + 1)
                .toLowerCase();

        if (!ALLOWED_IMG_EXTENSIONS.contains(ext)) {
            throw new BusinessException(FILE_TYPE_NOT_ALLOWED);
        }

        // MIME Type 검증
        String contentType = file.getContentType();

        if (contentType == null || !ALLOWED_IMG_CONTENT_TYPES.contains(contentType)) {
            throw new BusinessException(FILE_TYPE_NOT_ALLOWED);
        }

        String key = "profile/"
                + userId
                + "/"
                + UUID.randomUUID()
                + "."
                + ext;

        try {
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .contentType(contentType)
                    .build();

            s3Client.putObject(
                    request
                    , RequestBody.fromInputStream(
                            file.getInputStream()
                            , file.getSize()
                    )
            );

            return "https://"
                    + bucket
                    + ".s3."
                    + region
                    + ".amazonaws.com/"
                    + key;
        } catch (Exception e) {
            log.error("S3 업로드 실패. userId: {}, error: {}", userId, e.getMessage(), e);
            throw new BusinessException(FILE_UPLOAD_FAIL);
        }
    }

    @Override
    public void deleteProfileImg(String fileUrl) throws Exception {
        if(fileUrl == null || fileUrl.isBlank()) {
            return;
        }

        String key;

        try {
            URI uri = URI.create(fileUrl);
            String path = uri.getPath();

            key = path.startsWith("/") ? path.substring(1) : path;
        } catch (Exception e) {
            throw new BusinessException(FILE_NOT_FOUND);
        }

        try {
            DeleteObjectRequest request = DeleteObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .build();
            s3Client.deleteObject(request);
        } catch (Exception e) {
            log.error("S3 삭제 실패. fileURL: {}, error: {}", fileUrl, e.getMessage(), e);
            throw new BusinessException(FILE_DELETE_FAIL);
        }
    }

    @Override
    public String getProfileImgUrl(String fileUrl) throws Exception {
        if (fileUrl == null || fileUrl.isBlank()) {
            return null;
        }

        String key;

        try {
            URI uri = URI.create(fileUrl);
            String path = uri.getPath();

            key = path.startsWith("/") ? path.substring(1) : path;
        } catch (Exception e) {
            log.error("S3 URL 파싱 실패. fileURL: {}, error: {}", fileUrl, e.getMessage(), e);
            throw new BusinessException(FILE_NOT_FOUND);
        }

        try {
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .build();

            GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                    .signatureDuration(Duration.ofHours(1))
                    .getObjectRequest(getObjectRequest)
                    .build();

            return s3Presigner
                    .presignGetObject(presignRequest)
                    .url()
                    .toString();
        } catch (Exception e) {
            log.error("S3 Presigned URL 생성 실패. fileURL: {}, error: {}", fileUrl, e.getMessage(), e);
            throw new BusinessException(PROFILE_IMG_URL_GENERATE_FAIL);
        }
    }
}
