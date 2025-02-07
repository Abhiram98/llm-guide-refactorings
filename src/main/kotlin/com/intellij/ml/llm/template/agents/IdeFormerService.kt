package com.intellij.ml.llm.template.agents

//package ai.grazie.code.agents.example

import ai.grazie.code.agents.core.JwtTokenProvider
import ai.grazie.code.agents.ideformer.connection.*
import ai.grazie.code.agents.ideformer.daemon.IdeFormerDaemonConfig
import ai.grazie.code.agents.ideformer.daemon.IdeFormerDaemonManager
import ai.grazie.code.agents.ideformer.executable.ideFormerExecutableResourcePath
import ai.grazie.code.agents.ideformer.model.GrazieEnvironment
import ai.grazie.model.cloud.AuthType
import ai.grazie.model.cloud.AuthVersion
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.http.*
import java.io.File
import java.nio.file.Files
import kotlin.io.path.Path
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

internal object IdeFormerService {
    /**
     * Grazie token required for authentication on the platform.
     *
     * The token can be obtained from:
     * - https://platform.jetbrains.ai/
     * - https://platform.stgn.jetbrains.ai/
     */
    val token: String
        get() = System.getenv("GRAZIE_JWT_TOKEN")


    /**
     * Return connection to IdeFormer environment.
     *
     * @return [IdeFormerClient] connected to the running IdeFormer executable of the [ideformerVersion].
     * */
    suspend fun getAgentClient(tokenProvider: JwtTokenProvider): IdeFormerClient {
        val clientConfig = IdeFormerClientConfig(
            connection = IdeFormerClientConnectionConfig(
                protocol = URLProtocol.HTTP,
                host = "127.0.0.1",
                port = 5137,
            ),
            auth = IdeFormerClientAuthConfig(
                version = AuthVersion.V5,
                type = AuthType.Application
            ),
            timeout = IdeFormerConnectionTimeoutConfig(
                requestTimeoutMillis = 5.minutes.inWholeMilliseconds,
                connectTimeoutMillis = 1.minutes.inWholeMilliseconds,
                socketTimeoutMillis = 30.seconds.inWholeMilliseconds,
            ),
            grazieEnvironment = GrazieEnvironment.Staging
        )

        return IdeFormerClient(clientConfig, tokenProvider, HttpClient(CIO))
    }
}