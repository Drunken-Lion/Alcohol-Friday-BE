package com.drunkenlion.alcoholfriday.domain.auth.application;

import com.drunkenlion.alcoholfriday.domain.auth.dto.KakaoUserInfo;
import com.drunkenlion.alcoholfriday.domain.auth.dto.LoginResponse;
import com.drunkenlion.alcoholfriday.domain.auth.dto.SocialUserInfo;
import com.drunkenlion.alcoholfriday.domain.auth.enumerated.ProviderType;
import com.drunkenlion.alcoholfriday.domain.member.dao.MemberRepository;
import com.drunkenlion.alcoholfriday.domain.member.dto.MemberResponse;
import com.drunkenlion.alcoholfriday.domain.member.entity.Member;
import com.drunkenlion.alcoholfriday.domain.member.enumerated.MemberRole;
import com.drunkenlion.alcoholfriday.global.common.response.HttpResponse;
import com.drunkenlion.alcoholfriday.global.exception.BusinessException;
import com.drunkenlion.alcoholfriday.global.security.auth.UserPrincipal;
import com.drunkenlion.alcoholfriday.global.security.jwt.JwtTokenProvider;
import com.drunkenlion.alcoholfriday.global.security.jwt.application.TokenService;
import com.drunkenlion.alcoholfriday.global.security.jwt.dto.JwtResponse;
import com.drunkenlion.alcoholfriday.global.security.jwt.enumerated.TokenType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.time.*;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthServiceImpl implements AuthService {
    @Value("${oauth2.kakao.user-info-uri}")
    private String kakaoUserinfoUri;

    @Value("${port-one.imp_key}")
    private String impKey;

    @Value("${port-one.imp_secret}")
    private String impSecret;

    @Value("${port-one.get-token-uri}")
    private String getTokenUri;

    @Value("${port-one.get-certifications-uri}")
    private String getCertificationsUri;

    private final RestTemplate restTemplate;
    private final MemberRepository memberRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final TokenService tokenService;

    /**
     * 테스터 로그인
     */
    @Transactional
    @Override
    public LoginResponse testLogin(String email) {
        Member member = memberRepository.findByEmail(email)
                .orElseGet(() -> {
                    Member saveMember = Member.builder()
                            .email(email)
                            .name(email.split("@")[0])
                            .nickname(email.split("@")[0])
                            .role(MemberRole.MEMBER)
                            .provider(ProviderType.KAKAO)
                            .build();

                    return memberRepository.save(saveMember);
                });

        UserPrincipal userPrincipal = UserPrincipal.create(member);

        Authentication authentication =
                new UsernamePasswordAuthenticationToken(
                        userPrincipal, null, userPrincipal.getAuthorities()
                );

        tokenService.deleteRefreshToken(authentication.getName());

        JwtResponse jwtResponse = jwtTokenProvider.generateToken(authentication);
        MemberResponse memberResponse = MemberResponse.of(tokenService.findRefreshToken(jwtResponse.getRefreshToken()).getMember());

        return LoginResponse.builder()
                .jwtResponse(jwtResponse)
                .memberResponse(memberResponse)
                .build();
    }

    @Transactional
    @Override
    public LoginResponse socialLogin(ProviderType provider, String accessToken) {
        OAuth2User oAuth2User = loadUser(provider, accessToken);

        OAuth2AuthenticationToken authentication = new OAuth2AuthenticationToken(
                oAuth2User,
                oAuth2User.getAuthorities(),
                provider.getProviderName()
        );

        tokenService.deleteRefreshToken(authentication.getName());

        JwtResponse jwtResponse = this.jwtTokenProvider.generateToken(authentication);
        MemberResponse memberResponse = MemberResponse.of(tokenService.findRefreshToken(jwtResponse.getRefreshToken()).getMember());

        return LoginResponse.builder()
                .jwtResponse(jwtResponse)
                .memberResponse(memberResponse)
                .build();
    }

    public OAuth2User loadUser(ProviderType provider, String accessToken) {
        SocialUserInfo userInfo = getSocialUserInfoFactory(provider, getAttributes(provider, accessToken));

        log.info(getAttributes(provider, accessToken).toString());

        if (!StringUtils.hasText(userInfo.getEmail())) {
            throw new OAuth2AuthenticationException(String.format("%s 이메일을 찾을 수 없습니다.", provider.getProviderName()));
        }

        Member member = retrieveOrCreateMember(userInfo);

        return UserPrincipal.create(member, userInfo.getAttributes());
    }

    private SocialUserInfo getSocialUserInfoFactory(ProviderType provider, Map<String, Object> attributes) {
        return switch (provider) {
            case KAKAO -> new KakaoUserInfo(attributes);
            default -> throw new OAuth2AuthenticationException("지원하지 않는 소셜로그인 입니다.");
        };
    }

    private Member retrieveOrCreateMember(SocialUserInfo userInfo) {
        return this.memberRepository.findByEmail(userInfo.getEmail())
                .orElseGet(() -> {
                    Member member = Member.builder()
                            .email(userInfo.getEmail())
                            .name(userInfo.getName())
                            .nickname(userInfo.getNickname())
                            .role(MemberRole.MEMBER)
                            .provider(userInfo.getProvider())
                            .build();

                    return memberRepository.save(member);
                });
    }

    /**
     * 제공처로 액세스 토큰을 보내 회원 정보를 받아온다.
     */
    private Map<String, Object> getAttributes(ProviderType provider, String accessToken) {
        String endPoint = getEndPoint(provider);

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + accessToken);
        HttpEntity<String> entity = new HttpEntity<>("parameters", headers);

        ResponseEntity<Map<String, Object>> response = this.restTemplate.exchange(
                endPoint,
                HttpMethod.POST,
                entity,
                new ParameterizedTypeReference<>() {
                });

        return response.getBody();
    }

    private String getEndPoint(ProviderType providerType) {
        return switch (providerType) {
            case KAKAO -> kakaoUserinfoUri;
            default -> throw new OAuth2AuthenticationException("지원하지 않는 서비스 입니다.");
        };
    }

    /**
     * 리프레시 토큰 확인 후, 액세스 토큰 재발급
     */
    @Transactional
    public JwtResponse reissueToken(String requestRefreshToken) {
        if (!jwtTokenProvider.validateRefreshToken(requestRefreshToken))
            throw new BusinessException(HttpResponse.Fail.WRONG_TOKEN);

        Authentication authentication = jwtTokenProvider.getAuthentication(
                tokenService.findRefreshToken(requestRefreshToken).getToken(),
                TokenType.REFRESH_TOKEN.getValue());

        tokenService.deleteRefreshToken(authentication.getName());

        return jwtTokenProvider.generateToken(authentication);
    }

    /**
     * 성인 인증
     */
    @Transactional
    @Override
    public void authenticateAdultUser(Member member, String impUid) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        String accessToken = getPortOneToken(headers);

        Map<String, Object> data = getCertificationsInfo(headers, accessToken, impUid);
//        String uniqueKey = String.valueOf(data.get("unique_key")); TODO : 추후 활용하도록 구현
        String birth = String.valueOf(data.get("birth"));
        String phone = String.valueOf(data.get("phone"));

        if (!isAdult(birth)) {
            throw new BusinessException(HttpResponse.Fail.NOT_ADULT);
        }

        memberRepository.save(
                member.toBuilder()
                        .certifyAt(LocalDate.now())
                        .phone(Long.parseLong(phone))
                        .build());
    }

    private String getPortOneToken(HttpHeaders headers) {
        Map<String, String> tokenRequest = new HashMap<>();
        tokenRequest.put("imp_key", impKey);
        tokenRequest.put("imp_secret", impSecret);

        // 요청 본문 설정
        HttpEntity<Map<String, String>> requestEntity = new HttpEntity<>(tokenRequest, headers);

        // API 호출 및 ResponseEntity<Map>로 응답 받기
        ResponseEntity<Map> responseEntity = restTemplate.postForEntity(
                getTokenUri,
                requestEntity,
                Map.class);

        // 응답 본문에서 Map 객체 얻기
        Map<String, Object> tokenResponse = responseEntity.getBody();

        return String.valueOf(((Map) tokenResponse.get("response")).get("access_token"));
    }

    private Map<String, Object> getCertificationsInfo(HttpHeaders headers,
                                                      String accessToken,
                                                      String impUid) {
        headers.set("Authorization", accessToken);

        HttpEntity<String> certificationEntity = new HttpEntity<>("parameters", headers);

        ResponseEntity<Map> certificationResponse = restTemplate.exchange(
                getCertificationsUri + "/" + impUid,
                HttpMethod.GET,
                certificationEntity,
                Map.class
        );

        Map<String, Object> certificationsInfo = certificationResponse.getBody();

        return (Map<String, Object>) certificationsInfo.get("response");
    }

    private boolean isAdult(String birth) {
        long birthTimestamp = Long.parseLong(birth);
        Instant instant = Instant.ofEpochSecond(birthTimestamp);
        ZonedDateTime zonedDateTime = instant.atZone(ZoneId.of("Asia/Seoul"));

        LocalDate birthDate = zonedDateTime.toLocalDate();
        LocalDate currentDate = LocalDate.now();

        Period period = Period.between(birthDate, currentDate);

        int age = period.getYears();

        return age >= 19;
    }
}
