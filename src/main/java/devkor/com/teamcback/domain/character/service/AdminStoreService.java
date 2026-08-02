package devkor.com.teamcback.domain.character.service;

import static devkor.com.teamcback.global.response.ResultCode.ALREADY_OWNED_CHARACTER;
import static devkor.com.teamcback.global.response.ResultCode.CHARACTER_IN_USE;
import static devkor.com.teamcback.global.response.ResultCode.INVALID_INPUT;
import static devkor.com.teamcback.global.response.ResultCode.NOT_FOUND_CHARACTER;
import static devkor.com.teamcback.global.response.ResultCode.NOT_FOUND_USER;

import devkor.com.teamcback.domain.character.dto.request.CreateCharacterReq;
import devkor.com.teamcback.domain.character.dto.request.ModifyCharacterReq;
import devkor.com.teamcback.domain.character.dto.response.CreateCharacterRes;
import devkor.com.teamcback.domain.character.dto.response.DeleteCharacterRes;
import devkor.com.teamcback.domain.character.dto.response.GetAdminCharacterListRes;
import devkor.com.teamcback.domain.character.dto.response.GetAdminCharacterRes;
import devkor.com.teamcback.domain.character.dto.response.GrantCharacterRes;
import devkor.com.teamcback.domain.character.dto.response.ModifyCharacterRes;
import devkor.com.teamcback.domain.character.entity.KoCharacter;
import devkor.com.teamcback.domain.character.entity.UserCharacter;
import devkor.com.teamcback.domain.character.repository.CharacterRepository;
import devkor.com.teamcback.domain.character.repository.UserCharacterRepository;
import devkor.com.teamcback.domain.user.entity.Level;
import devkor.com.teamcback.domain.user.entity.User;
import devkor.com.teamcback.domain.user.repository.UserRepository;
import devkor.com.teamcback.global.exception.exception.GlobalException;
import devkor.com.teamcback.infra.s3.FilePath;
import devkor.com.teamcback.infra.s3.S3Util;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class AdminStoreService {
    private final CharacterRepository characterRepository;
    private final UserCharacterRepository userCharacterRepository;
    private final UserRepository userRepository;
    private final S3Util s3Util;

    /**
     * 캐릭터 목록 조회 (비활성 포함)
     */
    @Transactional(readOnly = true)
    public GetAdminCharacterListRes getCharacterList() {
        List<GetAdminCharacterRes> characterList = characterRepository.findAllByOrderByDisplayOrderAsc().stream()
            .map(GetAdminCharacterRes::new)
            .toList();

        return new GetAdminCharacterListRes(characterList);
    }

    /**
     * 캐릭터 생성
     */
    @Transactional
    public CreateCharacterRes createCharacter(CreateCharacterReq req) {
        validatePrice(req.getPrice());
        validateRequiredLevel(req.getRequiredLevel());

        String imageUrl = uploadImage(req.getImage());
        if(imageUrl == null) throw new GlobalException(INVALID_INPUT); // 이미지 필수

        KoCharacter character = characterRepository.save(new KoCharacter(req, imageUrl));

        return new CreateCharacterRes(character.getCharacterId());
    }

    /**
     * 캐릭터 수정 (이미지 미첨부 시 기존 이미지 유지)
     */
    @Transactional
    public ModifyCharacterRes modifyCharacter(Long characterId, ModifyCharacterReq req) {
        validatePrice(req.getPrice());
        validateRequiredLevel(req.getRequiredLevel());

        KoCharacter character = findCharacter(characterId);

        String imageUrl = uploadImage(req.getImage());
        if(imageUrl == null) {
            imageUrl = character.getImageUrl();
        } else if(character.getImageUrl() != null) {
            s3Util.deleteFile(character.getImageUrl(), FilePath.CHARACTER);
        }

        character.update(req, imageUrl);

        return new ModifyCharacterRes();
    }

    /**
     * 캐릭터 삭제 (구매한 사용자가 있으면 불가 - isActive=false로 숨김 처리 안내)
     */
    @Transactional
    public DeleteCharacterRes deleteCharacter(Long characterId) {
        KoCharacter character = findCharacter(characterId);

        if(userCharacterRepository.existsByCharacter(character)) {
            throw new GlobalException(CHARACTER_IN_USE);
        }

        if(character.getImageUrl() != null) {
            s3Util.deleteFile(character.getImageUrl(), FilePath.CHARACTER);
        }
        characterRepository.delete(character);

        return new DeleteCharacterRes();
    }

    /**
     * 캐릭터 수동 지급 (이벤트 보상 등, 포인트 차감 없음)
     */
    @Transactional
    public GrantCharacterRes grantCharacter(Long characterId, Long userId) {
        KoCharacter character = findCharacter(characterId);
        User user = userRepository.findById(userId).orElseThrow(() -> new GlobalException(NOT_FOUND_USER));

        if(userCharacterRepository.existsByUserAndCharacter(user, character)) {
            throw new GlobalException(ALREADY_OWNED_CHARACTER);
        }

        UserCharacter userCharacter = userCharacterRepository.save(new UserCharacter(user, character));

        return new GrantCharacterRes(userCharacter.getUserCharacterId());
    }

    private void validatePrice(Integer price) {
        if(price == null || price < 0) throw new GlobalException(INVALID_INPUT);
    }

    private void validateRequiredLevel(Integer requiredLevel) {
        int maxLevel = Level.values()[Level.values().length - 1].getLevelNumber();
        if(requiredLevel == null || requiredLevel < 1 || requiredLevel > maxLevel) {
            throw new GlobalException(INVALID_INPUT);
        }
    }

    private String uploadImage(MultipartFile image) {
        if(image == null || image.isEmpty()) return null;
        return s3Util.uploadFile(image, FilePath.CHARACTER);
    }

    private KoCharacter findCharacter(Long characterId) {
        return characterRepository.findById(characterId).orElseThrow(() -> new GlobalException(NOT_FOUND_CHARACTER));
    }
}
