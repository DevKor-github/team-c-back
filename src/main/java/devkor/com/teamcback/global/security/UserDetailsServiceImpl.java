package devkor.com.teamcback.global.security;

import devkor.com.teamcback.domain.user.entity.User;
import devkor.com.teamcback.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {
    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String userId) throws UsernameNotFoundException {
        User user = userRepository.findById(Long.parseLong(userId)).orElseThrow(
            () -> new UsernameNotFoundException(userId) // TODO: 예외 처리 후 수정
        );

        return new UserDetailsImpl(user);
    }
}
