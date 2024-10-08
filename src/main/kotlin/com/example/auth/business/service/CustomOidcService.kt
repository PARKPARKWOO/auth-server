package com.example.auth.business.service

import org.springframework.security.oauth2.client.oidc.userinfo.OidcReactiveOAuth2UserService
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest
import org.springframework.security.oauth2.client.userinfo.ReactiveOAuth2UserService
import org.springframework.security.oauth2.core.oidc.user.OidcUser
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono

@Service
class CustomOidcService : ReactiveOAuth2UserService<OidcUserRequest, OidcUser> {
    override fun loadUser(userRequest: OidcUserRequest?): Mono<OidcUser> {
        val oidcReactiveOAuth2UserService = OidcReactiveOAuth2UserService()
        return oidcReactiveOAuth2UserService.loadUser(userRequest)
    }
}
