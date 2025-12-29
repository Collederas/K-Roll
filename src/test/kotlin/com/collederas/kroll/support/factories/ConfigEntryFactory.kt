package com.collederas.kroll.support.factories

import com.collederas.kroll.core.configentry.ConfigEntryEntity
import com.collederas.kroll.core.configentry.ConfigEntryRepository
import com.collederas.kroll.core.configentry.ConfigType
import com.collederas.kroll.core.environment.EnvironmentEntity
import com.collederas.kroll.core.environment.EnvironmentRepository
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

// TODO: make test only @Profile("test")
object ConfigEntryFactory {

    fun create(
        environment: EnvironmentEntity = EnvironmentFactory.create(),
        key: String = "test.flag",
        value: String = "true",
        type: ConfigType = ConfigType.BOOLEAN,
        createdBy: UUID? = null,
        activeFrom: Instant? = null,
        activeUntil: Instant? = null,
        id: UUID = UUID.randomUUID(),
    ): ConfigEntryEntity {
        return ConfigEntryEntity(
            id = id,
            environment = environment,
            createdBy = createdBy,
            configKey = key,
            configValue = value,
            configType = type,
            activeFrom = activeFrom,
            activeUntil = activeUntil,
        )
    }

    /** Common presets to avoid repetition */
    fun activeBoolean(
        environment: EnvironmentEntity,
        key: String = "flag.enabled",
        value: Boolean = true,
    ) = create(
        environment = environment,
        key = key,
        value = value.toString(),
        type = ConfigType.BOOLEAN,
    )

    fun json(
        environment: EnvironmentEntity,
        key: String = "config.json",
        value: String = """{"enabled":true}""",
    ) = create(
        environment = environment,
        key = key,
        value = value,
        type = ConfigType.JSON,
    )
}

@Component
@Profile("test")
class PersistedConfigEntryFactory(
    private val environmentRepository: EnvironmentRepository,
    private val configEntryRepository: ConfigEntryRepository,
) {
    // Accept an EnvironmentEntity: will be saved/merged and returned managed
    @Transactional
    fun create(
        environment: EnvironmentEntity,
        key: String = "test-key",
        type: ConfigType = ConfigType.BOOLEAN,
        value: String = "true",
        createdBy: UUID,
    ): ConfigEntryEntity {
        val persistedEnv = environmentRepository.save(environment)
        val entry = ConfigEntryFactory.create(
            environment = persistedEnv,
            key = key,
            type = type,
            value = value,
            createdBy = createdBy
        )
        return configEntryRepository.save(entry)
    }

    @Transactional
    fun create(
        environmentId: UUID,
        key: String = "test-key",
        type: ConfigType = ConfigType.BOOLEAN,
        value: String = "true",
        createdBy: UUID,
    ): ConfigEntryEntity {
        val env = environmentRepository.findById(environmentId)
            .orElseThrow { IllegalArgumentException("Environment $environmentId not found") }
        val entry = ConfigEntryFactory.create(
            environment = env,
            key = key,
            type = type,
            value = value,
            createdBy = createdBy
        )
        return configEntryRepository.save(entry)
    }
}
