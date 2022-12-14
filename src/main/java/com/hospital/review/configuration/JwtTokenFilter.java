package com.hospital.review.configuration;

import com.hospital.review.domain.User;
import com.hospital.review.service.UserService;
import com.hospital.review.utils.JwtTokenUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

@RequiredArgsConstructor
@Slf4j
public class JwtTokenFilter extends OncePerRequestFilter { // 한번요청할때마다 검증
    private final UserService userService;
    private final String secretKey;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        // header에서 token 꺼내기
        final String authorizationHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        log.info("authorizationHeader:{}", authorizationHeader);

        // header이 존재하지 않을 경우 block
        if(authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) { // 토큰의 형식 : 시작할 때 맨앞에 Bearer 붙여서 보내야 한다.
            log.error("authorizationHeader이 잘못되었습니다.");
            filterChain.doFilter(request, response);
            return;
        }

        // Bearer 제거 - token만 가져오기
        String token = authorizationHeader.split(" ")[1];

        // Token Expired 만료 확인
        if(JwtTokenUtil.isExpired(token, secretKey)){
            log.error("token이 만료되었습니다.");
            filterChain.doFilter(request, response);
            return;
        }

        // userName token에서 꺼내오기
        String userName = JwtTokenUtil.getUserName(token, secretKey);
        log.info("jwtFilter - userName:{}",userName);

        // User가져오기
        User user = userService.getUserByUserName(userName);
        log.info("Token Filter userName:{}", userName);

        // 문열어주기
        // UserName넣기
        UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
                user.getUserName(), null, List.of(new SimpleGrantedAuthority("USER"))
        );
        authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authenticationToken);

        filterChain.doFilter(request, response);
    }
}
