package com.example.auth.domain.repository

import com.example.auth.domain.entity.applicationuser.ApplicationUser
import org.springframework.data.repository.reactive.ReactiveCrudRepository

interface ApplicationUserRepository : ReactiveCrudRepository<ApplicationUser, Long> {
}