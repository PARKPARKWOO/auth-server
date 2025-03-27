package com.example.auth.domain.repository

import com.example.auth.domain.entity.organization.Organization
import org.springframework.data.repository.reactive.ReactiveCrudRepository

interface OrganizationRepository: ReactiveCrudRepository<Organization, String> {
}