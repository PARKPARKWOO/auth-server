package com.example.unit.business

import com.example.auth.business.exception.BusinessException
import com.example.auth.business.service.application.ApplicationService
import com.example.auth.common.http.error.ErrorCode
import com.example.auth.domain.entity.applicationuser.ApplicationUser
import com.example.auth.domain.repository.application.ApplicationAuthorityRepository
import com.example.auth.domain.repository.application.ApplicationRepository
import com.example.auth.domain.repository.application.ApplicationUserRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.springframework.dao.DuplicateKeyException
import reactor.core.publisher.Mono
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertSame

class ApplicationServiceTest {
    private val applicationRepository = mockk<ApplicationRepository>()
    private val authorityRepository = mockk<ApplicationAuthorityRepository>()
    private val userRepository = mockk<ApplicationUserRepository>()
    private val service = ApplicationService(applicationRepository, authorityRepository, userRepository)

    @Test
    fun `ensure returns existing membership without downgrading admin authority`() = runTest {
        val existingAdmin = applicationUser(authorityId = ADMIN_AUTHORITY_ID)
        every { userRepository.findByApplicationIdAndUserId(APP_ID, USER_ID) } returns Mono.just(existingAdmin)

        val result = service.ensureApplicationUser(APP_ID, USER_ID, USER_AUTHORITY_ID)

        assertSame(existingAdmin, result)
        assertEquals(ADMIN_AUTHORITY_ID, result.authorityId)
        verify(exactly = 0) { userRepository.save(any()) }
    }

    @Test
    fun `ensure creates a missing membership once`() = runTest {
        val saved = applicationUser(authorityId = USER_AUTHORITY_ID)
        every { userRepository.findByApplicationIdAndUserId(APP_ID, USER_ID) } returns Mono.empty()
        every { userRepository.save(any()) } returns Mono.just(saved)

        val result = service.ensureApplicationUser(APP_ID, USER_ID, USER_AUTHORITY_ID)

        assertSame(saved, result)
        verify(exactly = 1) {
            userRepository.save(match {
                it.applicationId == APP_ID && it.userId == USER_ID && it.authorityId == USER_AUTHORITY_ID
            })
        }
    }

    @Test
    fun `duplicate key race rereads winner and preserves its admin authority`() = runTest {
        val winner = applicationUser(authorityId = ADMIN_AUTHORITY_ID)
        every { userRepository.findByApplicationIdAndUserId(APP_ID, USER_ID) } returnsMany
            listOf(Mono.empty(), Mono.just(winner))
        every { userRepository.save(any()) } returns Mono.error(DuplicateKeyException("duplicate membership"))

        val result = service.ensureApplicationUser(APP_ID, USER_ID, USER_AUTHORITY_ID)

        assertSame(winner, result)
        assertEquals(ADMIN_AUTHORITY_ID, result.authorityId)
        verify(exactly = 2) { userRepository.findByApplicationIdAndUserId(APP_ID, USER_ID) }
        verify(exactly = 1) { userRepository.save(any()) }
    }

    @Test
    fun `duplicate race without a visible winner throws a cause-free internal error`() = runTest {
        val rawCompositeKey = "$APP_ID:$USER_ID"
        val databaseCause = IllegalStateException("Duplicate entry '$rawCompositeKey' for membership key")
        val duplicate = DuplicateKeyException("duplicate membership $rawCompositeKey", databaseCause)
        every { userRepository.findByApplicationIdAndUserId(APP_ID, USER_ID) } returns Mono.empty()
        every { userRepository.save(any()) } returns Mono.error(duplicate)

        val error = assertFailsWith<BusinessException> {
            service.ensureApplicationUser(APP_ID, USER_ID, USER_AUTHORITY_ID)
        }

        assertEquals(ErrorCode.UNKNOWN_ERROR, error.errorCode)
        assertEquals(ErrorCode.UNKNOWN_ERROR.message, error.message)
        assertNull(error.cause)
        assertFalse(error.stackTraceToString().contains(rawCompositeKey))
        assertFalse(error.stackTraceToString().contains("Duplicate entry"))
        assertFalse(error.stackTraceToString().contains(duplicate.message.orEmpty()))
    }

    private fun applicationUser(authorityId: Long) = ApplicationUser(
        id = 11L,
        userId = USER_ID,
        applicationId = APP_ID,
        authorityId = authorityId,
    )

    private companion object {
        const val APP_ID = "00000000-0000-4000-8000-000000000100"
        const val USER_ID = "00000000-0000-4000-8000-000000000001"
        const val USER_AUTHORITY_ID = 7L
        const val ADMIN_AUTHORITY_ID = 9L
    }
}
