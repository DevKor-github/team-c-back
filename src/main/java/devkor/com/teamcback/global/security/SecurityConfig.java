package devkor.com.teamcback.global.security;

import devkor.com.teamcback.global.exception.handler.CustomAccessDeniedHandler;
import devkor.com.teamcback.global.exception.handler.CustomAuthenticationEntryPoint;
import devkor.com.teamcback.global.exception.handler.ExceptionHandlerFilter;
import devkor.com.teamcback.global.jwt.JwtAuthorizationFilter;
import devkor.com.teamcback.global.jwt.JwtUtil;
import devkor.com.teamcback.global.redis.RedisUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.security.servlet.PathRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
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
    private final LogoutHandler logoutHandler;
    private final LogoutSuccessHandler logoutSuccessHandler;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

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
    public CustomAccessDeniedHandler customAccessDeniedHandler() {
        return new CustomAccessDeniedHandler();
    }

    @Bean
    public CustomAuthenticationEntryPoint customAuthenticationEntryPoint() {
        return new CustomAuthenticationEntryPoint();
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
                .requestMatchers(HttpMethod.POST, "/api/search/**").authenticated()
                .requestMatchers("/api/users/login/**").permitAll() // 로그인은 허용
                .requestMatchers("/api/users/**").authenticated()
                .requestMatchers("/api/admin/**").hasRole("ADMIN") // 관리자인 경우에만 허용
                .requestMatchers("/api/categories/**").authenticated()
                .requestMatchers("/api/bookmarks/**").authenticated()
                .anyRequest().permitAll()
        ).exceptionHandling(ex -> ex
            .accessDeniedHandler(customAccessDeniedHandler()) // 인가 실패 시
            .authenticationEntryPoint(customAuthenticationEntryPoint()) // 인증 실패 시
        );

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
