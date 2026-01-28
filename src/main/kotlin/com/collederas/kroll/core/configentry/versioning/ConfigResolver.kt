package com.collederas.kroll.core.configentry.versioning

import com.collederas.kroll.core.configentry.ConfigEntryEntity
import com.collederas.kroll.core.configentry.ConfigEntryRepository
import com.collederas.kroll.core.configentry.ConfigType
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.stereotype.Component
import java.time.Clock
import java.time.Instant
import java.util.UUID

data class ResolvedConfig(
    val values: Map<String, ResolvedValue>
)

data class ResolvedValue(
    val type: ConfigType,
    val value: Any
)


@Component
class ConfigResolver(
    private val configEntryRepo: ConfigEntryRepository,
    private val objectMapper: ObjectMapper,
    private val clock: Clock,
) {
    fun resolveForEnvironment(envId: UUID): ResolvedConfig {
        val now = Instant.now(clock)

        val activeEntries = configEntryRepo.findActiveConfigs(envId, now)

        val resolved =
            activeEntries
                .groupBy { it.configKey }
                .mapValues { (_, entries) ->
                    val e = entries.maxBy { it.createdAt }
                    ResolvedValue(
                        type = e.configType,
                        value = parseValue(e),
                    )
                }
                .toSortedMap()

        return ResolvedConfig(resolved)
    }

    private fun chooseEffectiveEntry(
        entries: List<ConfigEntryEntity>
    ): ConfigEntryEntity =
        entries.maxBy { it.createdAt }

    private fun parseValue(entry: ConfigEntryEntity): Any =
        when (entry.configType) {
            ConfigType.BOOLEAN -> entry.configValue.toBooleanStrict()
            ConfigType.NUMBER -> entry.configValue.toBigDecimal()
            ConfigType.STRING -> entry.configValue
            ConfigType.JSON ->
                objectMapper.readValue(entry.configValue, Any::class.java)
        }
}

