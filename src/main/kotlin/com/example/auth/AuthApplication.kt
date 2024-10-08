package com.example.auth

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.http.server.reactive.ReactorHttpHandlerAdapter
import org.springframework.web.reactive.config.EnableWebFlux
import org.springframework.web.server.adapter.WebHttpHandlerBuilder

@SpringBootApplication
@EnableWebFlux
class AuthApplication

fun main(args: Array<String>) {
    runApplication<AuthApplication>(*args)
}
