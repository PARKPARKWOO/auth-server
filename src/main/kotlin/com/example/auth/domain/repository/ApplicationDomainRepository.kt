package com.example.auth.domain.repository

import com.example.auth.domain.model.application.ApplicationDomain
import org.springframework.data.repository.reactive.ReactiveCrudRepository

interface ApplicationDomainRepository : ReactiveCrudRepository<ApplicationDomain, Long>
