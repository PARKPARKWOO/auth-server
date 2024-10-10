package com.example.auth.domain.repository

import com.example.auth.domain.model.application.Application
import org.springframework.data.repository.reactive.ReactiveCrudRepository

interface ApplicationRepository : ReactiveCrudRepository<Application, String>
