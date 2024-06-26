package devkor.com.teamcback.global.security;

import devkor.com.teamcback.domain.oauth2.handler.OAuth2LoginSuccessHandler;
import devkor.com.teamcback.domain.oauth2.service.OAuth2Service;
import devkor.com.teamcback.global.exception.ExceptionHandlerFilter;
import devkor.com.teamcback.global.jwt.JwtAuthorizationFilter;
import devkor.com.teamcback.global.jwt.JwtUtil;
import devkor.com.teamcback.global.redis.RedisUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.security.servlet.PathRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    private final JwtUtil jwtUtil;
    private final RedisUtil redisUtil;
    private final UserDetailsService userDetailsService;
    private final OAuth2Service oAuth2Service;
    private final OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler;
    private final LogoutHandler logoutHandler;
    private final LogoutSuccessHandler logoutSuccessHandler;

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    public JwtAuthorizationFilter jwtAuthorizationFilter() {
        return new JwtAuthorizationFilter(jwtUtil, redisUtil, userDetailsService);
    }

    @Bean
    public ExceptionHandlerFilter exceptionHandlerFilter() {
        return new ExceptionHandlerFilter();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        // CSRF 설정
        http.csrf(AbstractHttpConfigurer::disable);

        // 기본 설정인 Session 방식은 사용하지 않고 JWT 방식을 사용하기 위한 설정
        http.sessionManagement((sessionManagement) ->
            sessionManagement.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
        );

        http.authorizeHttpRequests((authorizeHttpRequests) ->
            authorizeHttpRequests
                .requestMatchers(PathRequest.toStaticResources().atCommonLocations()).permitAll() // resources 접근 허용 설정
                .requestMatchers("/api/search/**").permitAll()
                .requestMatchers("/api/routes").permitAll()
                .requestMatchers("/api/routes/**").permitAll()
                .requestMatchers("/swagger-ui/**").permitAll()
                .requestMatchers("/swagger-ui.html").permitAll()
                .requestMatchers("/api-docs/**").permitAll()
                .anyRequest().authenticated() // 그 외 모든 요청 인증처리
        );

        http.oauth2Login(
            (oauth2) ->
                oauth2
                    .userInfoEndpoint(
                        userInfoEndpointConfig -> userInfoEndpointConfig.userService(oAuth2Service))
                    .successHandler(oAuth2LoginSuccessHandler));

        http.logout(
            logout -> {
                logout.logoutUrl("/api/users/logout");
                logout.addLogoutHandler(logoutHandler);
                logout.logoutSuccessHandler(logoutSuccessHandler);
            });

        // 필터 관리
        http.addFilterBefore(jwtAuthorizationFilter(), UsernamePasswordAuthenticationFilter.class);
        http.addFilterBefore(exceptionHandlerFilter(), JwtAuthorizationFilter.class);

        return http.build();
    }

}
