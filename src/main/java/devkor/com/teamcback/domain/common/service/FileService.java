package devkor.com.teamcback.domain.common.service;

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
public class FileService {
    private final S3Util s3Util;
    private final FileRepository fileRepository;

    /**
     * 파일 리스트 저장
     * @param fileList
     * @param fileUuid
     * @param sortNum
     */
    @Transactional
    public void upload(List<MultipartFile> fileList, String fileUuid, List<Long> sortNum, FilePath filePath) {

        // 입력 파일 없는 경우
        if(fileList == null || fileList.isEmpty()) {
            return;
        }

        // originalName, savedName, fileSize, extension, sortNum 필요 정보 추출
        for (int i = 0; i < fileList.size(); i++) {
            MultipartFile file = fileList.get(i);
            String fileOriginalName = file.getOriginalFilename();
            long fileSize = file.getSize();
            String extension = fileOriginalName.substring(fileOriginalName.lastIndexOf(".") + 1);

            // 원본 파일 S3 저장
            String fileSavedName = s3Util.uploadFile(file, filePath);

            File savedFile = new File(fileUuid, fileOriginalName, fileSavedName, fileSize, extension, sortNum.get(i));

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

        // originalName, savedName, fileSize, extension, sortNum 필요 정보 추출
        String fileOriginalName = uploadFile.getOriginalFilename();
        long fileSize = uploadFile.getSize();
        String extension = fileOriginalName.substring(fileOriginalName.lastIndexOf(".") + 1);

        // 원본 파일 S3 저장
        String fileSavedName = s3Util.uploadFile(uploadFile, filePath);

        File savedFile = new File(fileUuid, fileOriginalName, fileSavedName, fileSize, extension, sortNum);

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
        String thumbSavedName = s3Util.uploadFile(inputStream, savedFile.getFileUuid(), filePath);
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

    public String getOriginalFile(String fileUuid, Long sortNum) {
        if(sortNum == null || sortNum <= 0) {
            sortNum = 1L;
        }
        File file = fileRepository.findByFileUuidAndSortNum(fileUuid, sortNum);

        if(file == null) return null;

        return file.getFileSavedName();
    }

    public String getThumbnail(String fileUuid, Long sortNum) {
        if(sortNum == null || sortNum <= 0) {
            sortNum = 1L;
        }
        File file = fileRepository.findByFileUuidAndSortNum(fileUuid, sortNum);

        if(file == null) return null;

        return file.getThumbSavedName();
    }

    /**
     * fileUuid 생성
     * @return
     */
    public String createFileUuid() {
        return UUID.randomUUID().toString();
    }

}
