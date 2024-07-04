package devkor.com.teamcback.infra.s3;

import static devkor.com.teamcback.global.response.ResultCode.INVALID_IMAGE_FILE;
import static devkor.com.teamcback.global.response.ResultCode.MAXIMUM_UPLOAD_FILE_SIZE;
import static devkor.com.teamcback.global.response.ResultCode.NOT_FOUND_FILE;
import static devkor.com.teamcback.global.response.ResultCode.SYSTEM_ERROR;

import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.ObjectMetadata;
import devkor.com.teamcback.global.exception.GlobalException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@RequiredArgsConstructor
public class S3Util {

    private final AmazonS3Client amazonS3Client;
    private static final String IMAGE_JPG = "image/jpeg";
    private static final String IMAGE_PNG = "image/png";

    @Value("${cloud.aws.s3.bucket}")
    private String bucketName;

    private static ObjectMetadata setObjectMetadata(MultipartFile multipartFile) {
        // 업로드할 파일의 메타데이터를 설정하는 메소드
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(multipartFile.getSize());
        metadata.setContentType(multipartFile.getContentType());
        return metadata;
    }

    public String uploadFile(MultipartFile multipartFile, FilePath filePath) {
        // 업로드할 파일이 존재하지 않거나 비어있으면 null 반환
        if (multipartFile == null || multipartFile.isEmpty()) {
            return null;
        }

        // 이미지 파일인지 확인
        String fileType = multipartFile.getContentType();
        if(fileType == null || (!fileType.equals(IMAGE_JPG) && !fileType.equals(IMAGE_PNG))) {
            throw new GlobalException(INVALID_IMAGE_FILE);
        }

        // 각 파일의 크기 확인
        long fileSize = multipartFile.getSize();
        if (fileSize > 10 * 1024 * 1024) { // 10MB 이상인 경우 에러 발생
            throw new GlobalException(MAXIMUM_UPLOAD_FILE_SIZE);
        }

        // 업로드할 파일의 고유한 파일명 생성
        String fileName = createFileName(multipartFile.getOriginalFilename());
        // 파일명을 UTF-8로 디코딩
        fileName = URLDecoder.decode(fileName, StandardCharsets.UTF_8);
        // 업로드할 파일의 메타데이터 생성
        ObjectMetadata metadata = setObjectMetadata(multipartFile);

        try {
            // S3에 파일 업로드
            amazonS3Client.putObject(
                bucketName, filePath.getPath() + fileName, multipartFile.getInputStream(), metadata);
        } catch (Exception e) {
            // 업로드 중에 예외 발생 시 전역 예외(GlobalException) 발생
            log.info("error message: {}", e.getMessage());
            throw new GlobalException(SYSTEM_ERROR);
        }
        // 업로드한 파일의 URL 반환
        return getFileUrl(fileName, filePath);
    }

    public void deleteFile(String fileUrl, FilePath filePath) {
        // 주어진 파일 URL로부터 파일명을 추출
        String fileName = getFileNameFromFileUrl(fileUrl, filePath);
        // 파일명을 UTF-8로 디코딩
        fileName = URLDecoder.decode(fileName, StandardCharsets.UTF_8);
        // 파일명이 비어있거나 해당 파일이 존재하지 않으면 예외 발생
        if (fileName.isBlank()
            || !amazonS3Client.doesObjectExist(bucketName, filePath.getPath() + fileName)) {
            throw new GlobalException(NOT_FOUND_FILE);
        }
        // S3에서 파일 삭제
        amazonS3Client.deleteObject(bucketName, filePath.getPath() + fileName);
    }

    public boolean exists(String fileUrl, FilePath filePath) {
        // 파일 URL에서 파일 이름 추출
        String fileName = getFileNameFromFileUrl(fileUrl, filePath);

        // S3에 파일이 존재하는지 확인
        if (fileName.isBlank()
            || !amazonS3Client.doesObjectExist(bucketName, filePath.getPath() + fileName)) {
            return false; // 파일을 찾을 수 없음
        }
        return true;
    }

    private String getFileUrl(String fileName, FilePath filePath) {
        // AWS S3 클라이언트를 사용하여 주어진 버킷, 파일 경로 및 파일명에 해당하는 파일의 URL을 얻어옴
        return amazonS3Client.getUrl(bucketName, filePath.getPath() + fileName).toString();
    }

    private String getFileNameFromFileUrl(String fileUrl, FilePath filePath) {
        // 파일 URL에서 파일 경로 다음의 문자열부터 파일명의 끝까지 추출하여 반환
        return fileUrl.substring(fileUrl.lastIndexOf(filePath.getPath()) + filePath.getPath().length());
    }

    private String createFileName(String fileName) {
        // UUID를 사용하여 고유한 문자열을 생성하고, 주어진 파일명과 연결하여 반환
        return UUID.randomUUID().toString().concat(fileName);
    }
}
