package com.example.util

import io.grpc.Context
import org.woo.grpc.AuthMetadata.JWT_TOKEN_CONTEXT_KEY

object GrpcTestUtil {
    val DUMMY_TOKEN = "dummy-token"
    val TEST_CONTEXT = Context.current().withValue(JWT_TOKEN_CONTEXT_KEY, DUMMY_TOKEN)
}