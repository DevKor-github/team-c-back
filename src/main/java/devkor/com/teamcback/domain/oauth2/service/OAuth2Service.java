package devkor.com.teamcback.domain.oauth2.service;

import devkor.com.teamcback.domain.oauth2.dto.OAuth2UserInfo;
import devkor.com.teamcback.domain.user.entity.Role;
import devkor.com.teamcback.domain.user.entity.User;
import devkor.com.teamcback.domain.user.repository.UserRepository;
import devkor.com.teamcback.global.security.UserDetailsImpl;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

@RequiredArgsConstructor
@Service
public class OAuth2Service extends DefaultOAuth2UserService {

    private final UserRepository userRepository;

    private static final String DEFAULT_NAME = "호랑이";

    @Transactional
    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        // 1. 유저 정보(attributes) 가져오기
        Map<String, Object> oAuth2UserAttributes = super.loadUser(userRequest).getAttributes();

        // 2. resistrationId 가져오기 (third-party id)
        String registrationId = userRequest.getClientRegistration().getRegistrationId();

        // 3. userNameAttributeName 가져오기
        String userNameAttributeName = userRequest.getClientRegistration().getProviderDetails()
            .getUserInfoEndpoint().getUserNameAttributeName();

        // 4. 유저 정보 dto 생성
        OAuth2UserInfo oAuth2UserInfo = OAuth2UserInfo.of(registrationId, oAuth2UserAttributes);

        // 5. 회원가입 및 로그인
        User user = getOrSave(oAuth2UserInfo);

        // 6. OAuth2User로 반환
        return UserDetailsImpl.builder().user(user).attributes(oAuth2UserAttributes).attributeKey(userNameAttributeName).build();
    }

    private User getOrSave(OAuth2UserInfo oAuth2UserInfo) {
        User user = userRepository.findByEmail(oAuth2UserInfo.getEmail());
        if(user == null) user = save(oAuth2UserInfo);
        return user;
    }

    private User save(OAuth2UserInfo oAuth2UserInfo) {
        User user = new User(makeRandomName(), oAuth2UserInfo.getEmail(), Role.USER, oAuth2UserInfo.getProvider(), 0L);
        return userRepository.save(user);
    }

    private String makeRandomName() {
        String randomName;
        do {
            randomName = DEFAULT_NAME + UUID.randomUUID().toString().substring(0, 6);
        } while (userRepository.existsByUsername(randomName));
        return randomName;
    }
}