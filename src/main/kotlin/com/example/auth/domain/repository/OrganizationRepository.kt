package com.example.auth.domain.repository

import com.example.auth.domain.entity.organization.Organization
import org.springframework.data.r2dbc.repository.R2dbcRepository
import org.springframework.data.repository.reactive.ReactiveCrudRepository

interface OrganizationRepository: R2dbcRepository<Organization, String> {
}