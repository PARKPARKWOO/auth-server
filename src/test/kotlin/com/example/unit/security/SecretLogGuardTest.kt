package com.example.unit.security

import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class SecretLogGuardTest {
    @Test
    fun `auth source never sends raw token identifiers or exception details to logs`() {
        val violations = loadKotlinSources(AUTH_SOURCE_ROOT).flatMap { source ->
            val unsafeCalls = findUnsafeLogCalls(source.content)
                .map { call -> "${source.path}: $call" }
            val dynamicCalls = extractLogCalls(source.content)
                .filterNot(::isFixedLiteralLogCall)
                .map { call -> "${source.path}: $call" }
            val unsafeInterpolations = EXCEPTION_DETAIL_INTERPOLATION.findAll(source.content)
                .map { match -> "${source.path}: ${match.value}" }
                .toList()
            val printStackTrace = PRINT_STACK_TRACE.findAll(source.content)
                .map { match -> "${source.path}: ${match.value}" }
                .toList()
            val alternateLoggerCalls = ALTERNATE_LOGGER_CALL.findAll(source.content)
                .map { match -> "${source.path}: ${match.value}" }
                .toList()
            unsafeCalls + dynamicCalls + unsafeInterpolations + printStackTrace + alternateLoggerCalls
        }

        assertTrue(
            violations.isEmpty(),
            violations.joinToString(
                prefix = "secret-bearing or exception-detail log source detected:\n",
                separator = "\n",
            ),
        )
    }

    @Test
    fun `guard detects direct concatenated parameterized and interpolated secret logs`() {
        val unsafeExamples = listOf(
            "log().info(accessToken)",
            "log().debug(\"refresh=\" + refreshToken)",
            "log().info(\"cached={}\", refreshTokenInRedis)",
            "log().warn(\"jwt=${'$'}jwtToken\")",
            "log().error(token)",
            "log().trace(userId)",
            "log().warn(error.message)",
            "log().error(\"failed\", exception.cause)",
            "log().error(\"failed\", throwable)",
        )

        unsafeExamples.forEach { source ->
            assertTrue(
                findUnsafeLogCalls(source).isNotEmpty() || extractLogCalls(source).any { !isFixedLiteralLogCall(it) },
                "guard missed unsafe logger call: $source",
            )
        }
    }

    @Test
    fun `guard rejects aliases paths and alternate logger APIs`() {
        listOf(
            "log().warn(message)",
            "log().error(detail)",
            "log().info(\"path=${'$'}path\")",
            "logger.info(\"safe-looking alias\")",
            "LoggerFactory.getLogger(javaClass).warn(\"raw\")",
            "val audit = LoggerFactory.getLogger(javaClass); audit.info(token)",
            "println(accessToken)",
            "System.err.println(passport)",
        ).forEach { source ->
            val rejected =
                extractLogCalls(source).any { !isFixedLiteralLogCall(it) } ||
                    ALTERNATE_LOGGER_CALL.containsMatchIn(source)
            assertTrue(rejected, "guard missed alias/path/alternate logger: $source")
        }
    }

    @Test
    fun `guard accepts value-free security logs`() {
        val safeExamples = listOf(
            "log().error(\"access token parsing failed\")",
            "log().info(\"incoming reissue request\")",
            "log().warn(\"authentication request rejected\")",
        )

        safeExamples.forEach { source ->
            assertTrue(findUnsafeLogCalls(source).isEmpty(), "guard rejected value-free logger call: $source")
        }
    }

    @Test
    fun `source loading fails closed when the source root is missing`() {
        assertFailsWith<IllegalArgumentException> {
            loadKotlinSources(Paths.get("src/main/kotlin/com/example/auth-missing-for-log-guard"))
        }
    }

    @Test
    fun `framework security logging cannot emit raw request paths`() {
        val applicationYaml = Files.readString(Paths.get("src/main/resources/application.yml"))
        val normalized = applicationYaml.replace(Regex("\\s+"), " ").lowercase()

        assertTrue(
            !normalized.contains("springframework: security: debug") &&
                !normalized.contains("springframework: security: trace"),
            "Spring Security DEBUG or TRACE can log raw URI paths",
        )
    }

    @Test
    fun `logging filter is registered once before the security web filter chain`() {
        val filterSource = Files.readString(
            Paths.get("src/main/kotlin/com/example/auth/presentation/rest/filter/LoggingFilter.kt"),
        )
        val securitySource = Files.readString(
            Paths.get("src/main/kotlin/com/example/auth/common/config/SecurityConfig.kt"),
        )

        assertTrue(
            filterSource.contains("@Order(Ordered.HIGHEST_PRECEDENCE)"),
            "logging sanitizer must run before Spring Security",
        )
        assertTrue(
            !securitySource.contains("addFilterBefore(loggingFilter") &&
                !securitySource.contains("val loggingFilter: LoggingFilter"),
            "logging sanitizer must not be registered a second time inside Spring Security",
        )
    }

    private fun loadKotlinSources(root: Path): List<KotlinSource> {
        require(Files.isDirectory(root)) { "auth source root is missing or unreadable: $root" }
        val paths = Files.walk(root).use { stream ->
            stream
                .filter { path -> Files.isRegularFile(path) && path.fileName.toString().endsWith(".kt") }
                .sorted()
                .toList()
        }
        check(paths.isNotEmpty()) { "auth source root contains no Kotlin files: $root" }
        return paths.map { path -> KotlinSource(path, Files.readString(path)) }
    }

    private fun findUnsafeLogCalls(source: String): List<String> =
        extractLogCalls(source).filter { call ->
            SECRET_VALUE_IN_LOG.containsMatchIn(call) ||
                EXCEPTION_DETAIL_IN_LOG.containsMatchIn(call) ||
                THROWABLE_ARGUMENT_IN_LOG.containsMatchIn(call)
        }

    private fun isFixedLiteralLogCall(call: String): Boolean = FIXED_LITERAL_LOG_CALL.matches(call.trim())

    private fun extractLogCalls(source: String): List<String> {
        val calls = mutableListOf<String>()
        var searchStart = 0
        while (true) {
            val start = LOG_CALL_START.find(source, searchStart) ?: break
            val openingParenthesis = start.range.last
            var index = openingParenthesis + 1
            var depth = 1
            var quote = Quote.NONE
            var escaped = false

            while (index < source.length && depth > 0) {
                val current = source[index]
                when (quote) {
                    Quote.STRING -> when {
                        escaped -> escaped = false
                        current == '\\' -> escaped = true
                        current == '"' -> quote = Quote.NONE
                    }

                    Quote.TRIPLE_STRING -> if (source.startsWith("\"\"\"", index)) {
                        quote = Quote.NONE
                        index += 2
                    }

                    Quote.CHAR -> when {
                        escaped -> escaped = false
                        current == '\\' -> escaped = true
                        current == '\'' -> quote = Quote.NONE
                    }

                    Quote.NONE -> when {
                        source.startsWith("\"\"\"", index) -> {
                            quote = Quote.TRIPLE_STRING
                            index += 2
                        }

                        current == '"' -> quote = Quote.STRING
                        current == '\'' -> quote = Quote.CHAR
                        current == '(' -> depth += 1
                        current == ')' -> depth -= 1
                    }
                }
                index += 1
            }

            check(depth == 0) { "unterminated logger call at source offset ${start.range.first}" }
            calls += source.substring(start.range.first, index)
            searchStart = index
        }
        return calls
    }

    private data class KotlinSource(val path: Path, val content: String)

    private enum class Quote {
        NONE,
        STRING,
        TRIPLE_STRING,
        CHAR,
    }

    private companion object {
        val AUTH_SOURCE_ROOT: Path = Paths.get("src/main/kotlin/com/example/auth")
        const val SENSITIVE_IDENTIFIER =
            "(?:refreshTokenInRedis|accessToken|refreshToken|jwtToken|userId|token)"
        val LOG_CALL_START = Regex("""log\(\)\.(?:trace|debug|info|warn|error)\s*\(""")
        val SECRET_VALUE_IN_LOG = Regex(
            """(?:\$${SENSITIVE_IDENTIFIER}\b|\$\{\s*(?:[A-Za-z_]\w*\.)?$SENSITIVE_IDENTIFIER\b|""" +
                """(?:\(\s*|,\s*|\+\s*)(?:[A-Za-z_]\w*\.)?$SENSITIVE_IDENTIFIER\b|""" +
                """(?:[A-Za-z_]\w*\.)?$SENSITIVE_IDENTIFIER\b\s*\+)""",
        )
        val EXCEPTION_DETAIL_IN_LOG = Regex("""\.\s*(?:message|cause)\b""")
        val THROWABLE_ARGUMENT_IN_LOG = Regex(
            """,\s*(?:[A-Za-z_]\w*\.)?(?:e|error|exception|throwable)\s*\)""",
        )
        val EXCEPTION_DETAIL_INTERPOLATION = Regex(
            """\$\{[^}\n]*(?:\.\s*(?:message|cause)\b|printStackTrace\s*\()[^}\n]*}""",
        )
        val PRINT_STACK_TRACE = Regex("""\bprintStackTrace\s*\(""")
        val FIXED_LITERAL_LOG_CALL = Regex(
            """log\(\)\.(?:trace|debug|info|warn|error)\s*\(\s*\"(?:\\.|[^\"\\$])*\"\s*\)""",
            setOf(RegexOption.DOT_MATCHES_ALL),
        )
        val ALTERNATE_LOGGER_CALL = Regex(
            """(?:\b(?:LoggerFactory|KotlinLogging)\b|\bprintln\s*\(|\bSystem\s*\.\s*(?:out|err)\b|""" +
                """\bprintStackTrace\s*\(|\b(?!Mono\b)[A-Za-z_]\w*\s*\.\s*""" +
                """(?:trace|debug|info|warn|error|atTrace|atDebug|atInfo|atWarn|atError)\s*\()""",
        )
    }
}
