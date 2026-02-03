package devkor.com.teamcback.domain.place.service;

import static devkor.com.teamcback.global.response.ResultCode.DUPLICATED_PLACE_IMAGE;
import static devkor.com.teamcback.global.response.ResultCode.NOT_FOUND_FILE;
import static devkor.com.teamcback.global.response.ResultCode.NOT_FOUND_PLACE;

import devkor.com.teamcback.domain.common.util.FileUtil;
import devkor.com.teamcback.domain.place.dto.response.DeletePlaceImageRes;
import devkor.com.teamcback.domain.place.dto.response.ModifyPlaceImageRes;
import devkor.com.teamcback.domain.place.dto.response.SavePlaceImageRes;
import devkor.com.teamcback.domain.place.dto.response.SearchPlaceImageRes;
import devkor.com.teamcback.domain.place.entity.Place;
import devkor.com.teamcback.domain.place.repository.PlaceRepository;
import devkor.com.teamcback.global.exception.exception.GlobalException;
import devkor.com.teamcback.infra.s3.FilePath;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class AdminPlaceImageService {
    private final PlaceRepository placeRepository;
    private final FileUtil fileUtil;

    // 장소 사진 저장
    @Transactional
    public SavePlaceImageRes savePlaceImage(Long placeId, MultipartFile image) {
        Place place = findPlace(placeId);

        // 이미 이미지가 있는지 확인
        checkExistedImage(place);

        // UUID 생성 및 파일 업로드
        String fileUuid = fileUtil.createFileUuid();
        fileUtil.upload(image, fileUuid, FilePath.PLACE, 1L);

        // Place에 fileUuid 저장
        place.setFileUuid(fileUuid);

        String imageUrl = fileUtil.getOriginalFile(fileUuid);

        return new SavePlaceImageRes(place, imageUrl);
    }

    // 장소 사진 수정
    @Transactional
    public ModifyPlaceImageRes modifyPlaceImage(Long placeId, MultipartFile image) {
        Place place = findPlace(placeId);

        // fileUuid 확인
        if (place.getFileUuid() == null) {
            place.setFileUuid(fileUtil.createFileUuid());
        } else {
            // 기존 파일 삭제
            fileUtil.deleteFile(place.getFileUuid());
        }

        // 새 파일 업로드
        fileUtil.upload(image, place.getFileUuid(), FilePath.PLACE, 1L);

        String imageUrl = fileUtil.getOriginalFile(place.getFileUuid());

        return new ModifyPlaceImageRes(place, imageUrl);
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

        // fileUuid 초기화
        place.setFileUuid(null);

        return new DeletePlaceImageRes();
    }

    // 장소 사진 검색
    @Transactional(readOnly = true)
    public SearchPlaceImageRes searchPlaceImage(Long placeId) {
        Place place = findPlace(placeId);

        if (place.getFileUuid() == null) {
            return new SearchPlaceImageRes(place);
        }

        String fileName = fileUtil.getOriginalFile(place.getFileUuid());
        if (fileName == null) {
            return new SearchPlaceImageRes(place);
        }

        return new SearchPlaceImageRes(place, fileName);
    }

    private Place findPlace(Long placeId) {
        return placeRepository.findById(placeId)
            .orElseThrow(() -> new GlobalException(NOT_FOUND_PLACE));
    }

    private void checkExistedImage(Place place) {
        if (place.getFileUuid() != null) {
            throw new GlobalException(DUPLICATED_PLACE_IMAGE);
        }
    }
}
