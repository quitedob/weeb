package com.web.Config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;

/**
 * OAuth2 配置类
 * 配置第三方登录（Google、Microsoft）
 */
@Configuration
@EnableWebSecurity
public class OAuth2Config {

    /**
     * 配置 OAuth2 客户端注册
     */
    @Bean
    public ClientRegistrationRepository clientRegistrationRepository() {
        return new InMemoryClientRegistrationRepository(
            googleClientRegistration(),
            microsoftClientRegistration()
        );
    }

    /**
     * Google OAuth2 客户端配置
     */
    private ClientRegistration googleClientRegistration() {
        return ClientRegistration.withRegistrationId("google")
            .clientId("${oauth2.google.client-id:your-google-client-id}")
            .clientSecret("${oauth2.google.client-secret:your-google-client-secret}")
            .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
            .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
            .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
            .scope("openid", "profile", "email")
            .authorizationUri("https://accounts.google.com/o/oauth2/auth")
            .tokenUri("https://oauth2.googleapis.com/token")
            .userInfoUri("https://www.googleapis.com/oauth2/v2/userinfo")
            .userNameAttributeName("id")
            .jwkSetUri("https://www.googleapis.com/oauth2/v2/certs")
            .clientName("Google")
            .build();
    }

    /**
     * Microsoft OAuth2 客户端配置
     */
    private ClientRegistration microsoftClientRegistration() {
        return ClientRegistration.withRegistrationId("microsoft")
            .clientId("${oauth2.microsoft.client-id:your-microsoft-client-id}")
            .clientSecret("${oauth2.microsoft.client-secret:your-microsoft-client-secret}")
            .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
            .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
            .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
            .scope("openid", "profile", "email")
            .authorizationUri("https://login.microsoftonline.com/common/oauth2/v2.0/authorize")
            .tokenUri("https://login.microsoftonline.com/common/oauth2/v2.0/token")
            .userInfoUri("https://graph.microsoft.com/oidc/userinfo")
            .userNameAttributeName("sub")
            .jwkSetUri("https://login.microsoftonline.com/common/discovery/v2.0/keys")
            .clientName("Microsoft")
            .build();
    }
}

