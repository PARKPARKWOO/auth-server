package com.example.auth.domain.repository

import com.example.auth.domain.entity.organization.OrganizationApplication
import org.springframework.data.repository.reactive.ReactiveCrudRepository

interface OrganizationApplicationRepository: ReactiveCrudRepository<OrganizationApplication, Long> {
}