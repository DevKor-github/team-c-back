package devkor.com.teamcback.domain.place.service;

import static devkor.com.teamcback.global.response.ResultCode.NOT_FOUND_FILE;
import static devkor.com.teamcback.global.response.ResultCode.NOT_FOUND_PLACE;

import devkor.com.teamcback.domain.common.entity.File;
import devkor.com.teamcback.domain.common.util.FileUtil;
import devkor.com.teamcback.domain.place.dto.response.*;
import devkor.com.teamcback.domain.place.entity.Place;
import devkor.com.teamcback.domain.place.repository.PlaceRepository;
import devkor.com.teamcback.global.exception.exception.GlobalException;
import devkor.com.teamcback.infra.s3.FilePath;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminPlaceImageService {
    private final PlaceRepository placeRepository;
    private final FileUtil fileUtil;

    // 장소 사진 1장 추가 저장
    @Transactional
    public SavePlaceImageRes savePlaceImage(Long placeId, MultipartFile image) {
        Place place = findPlace(placeId);

        String fileUuid = place.getFileUuid();
        long sortNum = 1L;

        // fileUuid 확인
        if (fileUuid == null) {
            // 새로 생성하여 저장
            fileUuid = fileUtil.createFileUuid();
            place.setFileUuid(fileUuid);
        }
        else {
            sortNum = fileUtil.getFiles(fileUuid).size() + 1;
        }

        // 파일 업로드
        fileUtil.upload(image, fileUuid, FilePath.PLACE, sortNum);

        String imageUrl = fileUtil.getOriginalFile(fileUuid);

        return new SavePlaceImageRes(place, imageUrl);
    }

    // 장소 사진 여러 장 새로 저장
    @Transactional
    public SavePlaceImageRes savePlaceImageList(Long placeId, List<MultipartFile> images) {
        Place place = findPlace(placeId);

        String fileUuid = place.getFileUuid();

        // fileUuid 확인
        if (fileUuid == null) {
            // 새로 생성하여 저장
            fileUuid = fileUtil.createFileUuid();
            place.setFileUuid(fileUuid);
        }

        // 파일 업로드
        fileUtil.upload(images, fileUuid, null, FilePath.PLACE);

        String imageUrl = fileUtil.getOriginalFile(fileUuid);

        return new SavePlaceImageRes(place, imageUrl);
    }

    // 장소 사진 삭제
    @Transactional
    public DeletePlaceImageRes deletePlaceImage(Long placeId) {
        Place place = findPlace(placeId);

        if (place.getFileUuid() == null) {
            throw new GlobalException(NOT_FOUND_FILE);
        }

        // 기존 파일 삭제
        fileUtil.deleteFile(place.getFileUuid());

        return new DeletePlaceImageRes();
    }

    // 장소 사진 1장 검색
    @Transactional(readOnly = true)
    public GetPlaceImageRes searchPlaceImage(Long placeId) {
        Place place = findPlace(placeId);

        String fileUuid = place.getFileUuid();

        // 사진 없을 때
        if (fileUuid == null) {
            return new GetPlaceImageRes(place);
        }

        String fileName = fileUtil.getOriginalFile(fileUuid);
        if (fileName == null) {
            return new GetPlaceImageRes(place);
        }

        // 사진 있을 때
        return new GetPlaceImageRes(place, fileName);
    }

    // 장소 사진 여러 장 검색
    @Transactional(readOnly = true)
    public SearchPlaceImageListRes searchPlaceImageList(Long placeId) {
        Place place = findPlace(placeId);

        String fileUuid = place.getFileUuid();

        List<SearchPlaceImageRes> images = new ArrayList<>();

        // 사진 없을 때
        if (fileUuid == null || fileUtil.getFiles(fileUuid).isEmpty()) {
            images.add(new SearchPlaceImageRes(place));
            return new SearchPlaceImageListRes(place, images);
        }

        // 사진 있을 때
        for(File file : fileUtil.getFiles(fileUuid)) {
            images.add(new SearchPlaceImageRes(file.getFileSavedName(), file.getSortNum()));
        }

        return new SearchPlaceImageListRes(place, images);
    }

    private Place findPlace(Long placeId) {
        return placeRepository.findById(placeId)
            .orElseThrow(() -> new GlobalException(NOT_FOUND_PLACE));
    }
}
