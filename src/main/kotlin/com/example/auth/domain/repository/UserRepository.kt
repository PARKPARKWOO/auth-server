package com.example.auth.domain.repository

import com.example.auth.domain.model.user.User
import org.springframework.data.repository.reactive.ReactiveCrudRepository
import java.util.UUID

interface UserRepository : ReactiveCrudRepository<User, UUID>
