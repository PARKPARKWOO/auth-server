package com.example.unit.business

import com.example.auth.business.facade.OAuthApplicationFacade
import com.example.auth.business.service.RegistrationService
import com.example.auth.business.service.application.ApplicationOAuthService
import com.example.auth.business.service.application.ApplicationService
import com.example.auth.business.service.user.EndUserFinder
import com.example.auth.business.service.user.EndUserWriter
import com.example.auth.domain.entity.application.ApplicationAuthority
import com.example.auth.domain.entity.application.ApplicationOAuthProvider
import com.example.auth.domain.entity.applicationuser.ApplicationUser
import com.example.auth.domain.model.oauth.SocialProvider
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

class OAuthApplicationFacadeTest {
    private val applicationService = mockk<ApplicationService>()
    private val applicationOAuthService = mockk<ApplicationOAuthService>()
    private val facade = OAuthApplicationFacade(
        applicationService = applicationService,
        endUserFinder = mockk<EndUserFinder>(),
        endUserWriter = mockk<EndUserWriter>(),
        registrationService = mockk<RegistrationService>(),
        applicationOAuthService = applicationOAuthService,
    )

    @Test
    fun `membership creation delegates once to idempotent ensure without role update`() = runTest {
        val provider = ApplicationOAuthProvider(
            id = REGISTRATION_ID,
            applicationId = APP_ID,
            provider = SocialProvider.GOOGLE,
            clientId = "client-id",
            clientSecret = null,
        )
        val defaultAuthority = ApplicationAuthority(
            id = USER_AUTHORITY_ID,
            applicationId = APP_ID,
            authority = "ROLE_USER",
            level = 0,
        )
        val existingAdmin = ApplicationUser(
            id = 11L,
            userId = USER_ID,
            applicationId = APP_ID,
            authorityId = ADMIN_AUTHORITY_ID,
        )
        coEvery { applicationOAuthService.findById(REGISTRATION_ID) } returns provider
        coEvery { applicationService.getLowLevelApplicationAuthority(APP_ID) } returns defaultAuthority
        coEvery {
            applicationService.ensureApplicationUser(APP_ID, USER_ID, USER_AUTHORITY_ID)
        } returns existingAdmin

        facade.createApplicationUserIfNotExist(REGISTRATION_ID, USER_ID)

        coVerify(exactly = 1) {
            applicationService.ensureApplicationUser(APP_ID, USER_ID, USER_AUTHORITY_ID)
        }
        coVerify(exactly = 0) { applicationService.getApplicationUser(any(), any()) }
        coVerify(exactly = 0) { applicationService.createApplicationUser(any(), any(), any()) }
        coVerify(exactly = 0) { applicationService.updateApplicationUserRole(any(), any(), any()) }
    }

    private companion object {
        const val REGISTRATION_ID = 42L
        const val APP_ID = "00000000-0000-4000-8000-000000000100"
        const val USER_ID = "00000000-0000-4000-8000-000000000001"
        const val USER_AUTHORITY_ID = 7L
        const val ADMIN_AUTHORITY_ID = 9L
    }
}
