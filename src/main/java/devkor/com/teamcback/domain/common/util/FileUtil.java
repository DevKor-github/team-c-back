package devkor.com.teamcback.domain.common.util;

import devkor.com.teamcback.domain.common.entity.File;
import devkor.com.teamcback.domain.common.repository.FileRepository;
import devkor.com.teamcback.global.exception.exception.GlobalException;
import devkor.com.teamcback.infra.s3.FilePath;
import devkor.com.teamcback.infra.s3.S3Util;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.*;

import static devkor.com.teamcback.global.response.ResultCode.SYSTEM_ERROR;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileUtil {
    private final S3Util s3Util;
    private final FileRepository fileRepository;
    private static final String IMAGE_JPG = "image/jpeg";
    private static final String IMAGE_PNG = "image/png";

    /**
     * 파일 리스트 저장
     * @param fileList
     * @param fileUuid
     * @param sortNumList
     */
    @Transactional
    public void upload(List<MultipartFile> fileList, String fileUuid, List<Long> sortNumList, FilePath filePath) {

        // 입력 파일 없는 경우
        if(fileList == null || fileList.isEmpty()) {
            return;
        }

        // originalName, savedName, fileSize, extension, sortNum 필요 정보 추출
        for (int i = 0; i < fileList.size(); i++) {
            MultipartFile file = fileList.get(i);
            String fileOriginalName = file.getOriginalFilename();
            long fileSize = file.getSize();
            long sortNum = sortNumList == null ? i+1 : sortNumList.get(i);
            String extension = fileOriginalName.substring(fileOriginalName.lastIndexOf(".") + 1);

            // 원본 파일 S3 저장
            String fileSavedName = s3Util.uploadFile(file, filePath);

            // 기존 파일 엔티티 가져오기
            File savedFile = fileRepository.findByFileUuidAndSortNum(fileUuid, sortNum);
            if(savedFile == null) {
                savedFile = new File(fileUuid, sortNum);
            }

            // 업데이트
            savedFile.update(fileUuid, fileOriginalName, fileSavedName, fileSize, extension, sortNum);

            // 썸네일 생성
            uploadThumb(savedFile, file, 400, 400, filePath);

            // 파일 저장
            fileRepository.save(savedFile);
        }
    }

    /**
     * 파일 저장
     * @param uploadFile
     * @param fileUuid
     */
    @Transactional
    public void upload(MultipartFile uploadFile, String fileUuid, FilePath filePath, Long sortNum) {

        // 입력 파일 없는 경우
        if(uploadFile == null || uploadFile.isEmpty()) {
            return;
        }

        File savedFile = fileRepository.findByFileUuidAndSortNum(fileUuid, sortNum);
        if(savedFile == null) {
            savedFile = new File(fileUuid, sortNum);
        }

        // originalName, savedName, fileSize, extension, sortNum 필요 정보 추출
        String fileOriginalName = uploadFile.getOriginalFilename();
        long fileSize = uploadFile.getSize();
        String extension = fileOriginalName.substring(fileOriginalName.lastIndexOf(".") + 1);

        // 원본 파일 S3 저장
        String fileSavedName = s3Util.uploadFile(uploadFile, filePath);

        savedFile.update(fileUuid, fileOriginalName, fileSavedName, fileSize, extension, sortNum);

        // 썸네일 생성
        uploadThumb(savedFile, uploadFile, 400, 400, filePath);

        // 파일 저장
        fileRepository.save(savedFile);
    }

    /**
     * 썸네일 생성
     * @param file
     * @param width
     * @param height
     */
    @Transactional
    public void uploadThumb(File savedFile, MultipartFile file, Integer width, Integer height, FilePath filePath) {

        // 이미지 파일인지 확인
        String fileType = file.getContentType();
        if(fileType == null || (!fileType.equals(IMAGE_JPG) && !fileType.equals(IMAGE_PNG))) {
            return;
        }

        // 썸네일 생성
        ByteArrayOutputStream os = new ByteArrayOutputStream();

        try {
            Thumbnails.of(file.getInputStream())
                    .size(width, height)
                    .keepAspectRatio(true)
                    .toOutputStream(os);
        } catch (Exception e) {
            // 변환 중에 예외 발생 시 전역 예외(GlobalException) 발생
            log.info("error message: {}", e.getMessage());
            throw new GlobalException(SYSTEM_ERROR);
        }

        InputStream inputStream = new ByteArrayInputStream(os.toByteArray());

        // 저장
        String thumbSavedName = s3Util.uploadFile(inputStream, savedFile.getFileUuid(), filePath, file.getContentType());
        savedFile.setThumbSavedName(thumbSavedName);
    }

    /**
     * 파일 삭제
     * @param fileUuid
     */
    @Transactional
    public void deleteFile(String fileUuid) {
        // 기존 파일 테이블 삭제
        List<File> savedFileList = fileRepository.findAllByFileUuid(fileUuid);

        for (File savedFile : savedFileList) {
            if(savedFile.getFileSavedName() == null || savedFile.getFileSavedName().isEmpty()) continue;

            // s3 파일 삭제
            if(savedFile != null && s3Util.exists(savedFile.getFileSavedName(), FilePath.BUILDING_IMAGE)) {
                s3Util.deleteFile(savedFile.getFileSavedName(), FilePath.BUILDING_IMAGE); // 기존 사진 S3에서 삭제
            }

            // 썸네일 s3 파일 삭제
            if(savedFile != null && s3Util.exists(savedFile.getFileSavedName(), FilePath.BUILDING_IMAGE.getPath() + "thumb/")) {
                s3Util.deleteFile(savedFile.getFileSavedName(), FilePath.BUILDING_IMAGE.getPath() + "thumb/"); // 기존 사진 S3에서 삭제
            }
        }

        fileRepository.deleteAll(savedFileList);
    }

    public List<String> getOriginalFiles(String fileUuid) {
        List<String> fileList = fileRepository.findAllByFileUuid(fileUuid).stream().map(File::getFileSavedName).toList();

        return fileList;
    }

    public List<String> getThumbnailFiles(String fileUuid) {
        List<String> fileList = fileRepository.findAllByFileUuid(fileUuid).stream().map(File::getThumbSavedName).toList();

        return fileList;
    }

    public String getOriginalFile(String fileUuid) {

        List<String> originalFiles = getOriginalFiles(fileUuid);
        if(originalFiles.isEmpty()) return null;

        return originalFiles.get(0);
    }

    public String getThumbnail(String fileUuid) {

        List<String> thumbnailFiles = getThumbnailFiles(fileUuid);
        if(thumbnailFiles.isEmpty()) return null;

        return thumbnailFiles.get(0);
    }

    /**
     * fileUuid 생성
     * @return
     */
    public String createFileUuid() {
        return UUID.randomUUID().toString();
    }

}
