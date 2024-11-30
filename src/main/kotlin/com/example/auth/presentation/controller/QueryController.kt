package com.example.auth.presentation.controller

import com.example.auth.business.service.ApplicationFinder
import com.example.auth.domain.entity.application.Application
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.woo.http.SucceededApiResponseBody

@RestController
@RequestMapping("/api/v1/auth")
class QueryController(
    private val applicationFinder: ApplicationFinder,
) {
    @GetMapping("/application/{name}")
    suspend fun findApplication(
        @PathVariable("name")
        name: String,
    ): SucceededApiResponseBody<Application?> {
        val response = applicationFinder.findByName(name)
        return SucceededApiResponseBody(response)
    }
}
