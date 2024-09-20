package devkor.com.teamcback.global.security;

import static devkor.com.teamcback.global.response.ResultCode.NOT_FOUND_USER;

import devkor.com.teamcback.domain.user.entity.User;
import devkor.com.teamcback.domain.user.repository.UserRepository;
import devkor.com.teamcback.global.exception.GlobalException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {
    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String userId) throws UsernameNotFoundException {
        log.info("String userId: {}", userId);
        log.info("Long userId: {}", Long.parseLong(userId));
        User user = userRepository.findByUserId(Long.parseLong(userId));
        if(user == null) {
            throw new GlobalException(NOT_FOUND_USER);
        }
        return UserDetailsImpl.builder().user(user).build();
    }
}
