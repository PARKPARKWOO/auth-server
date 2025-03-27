package com.example.auth.business

import com.example.auth.domain.repository.OrganizationApplicationRepository
import com.example.auth.domain.repository.OrganizationRepository
import org.springframework.stereotype.Service

@Service
class OrganizationService(
    private val organizationRepository: OrganizationRepository,
    private val organizationApplicationRepository: OrganizationApplicationRepository,
) {
    suspend fun createOrganization() {

    }

    suspend fun joinApplicationInOrganization() {

    }
}