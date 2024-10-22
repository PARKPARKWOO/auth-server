package com.example.auth.domain.model.user

enum class Role {
    ROLE_USER,
    ROLE_ADMIN,
    ;

    companion object {
        fun from(name: String): Role {
            return Role.valueOf(name)
        }
    }
}
