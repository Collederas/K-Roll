package com.collederas.kroll.core.config.validation

import com.collederas.kroll.core.config.entry.ConfigType
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.stereotype.Component

@Component
class ConfigEntryValidator(
    private val objectMapper: ObjectMapper,
) {
    companion object {
        private const val MAX_JSON_BYTES: Int = 64 * 1024
    }

    fun validate(
        value: String,
        type: ConfigType,
    ): List<String> {
        val errors = mutableListOf<String>()
        when (type) {
            ConfigType.NUMBER -> {
                if (value.toBigDecimalOrNull() == null) {
                    errors.add("Value '$value' is not a valid NUMBER")
                }
            }

            ConfigType.BOOLEAN -> {
                if (!value.equals("true", ignoreCase = true) &&
                    !value.equals("false", ignoreCase = true)
                ) {
                    errors.add("Value '$value' is not a valid BOOLEAN")
                }
            }

            ConfigType.STRING -> {
                if (value.isBlank()) {
                    errors.add("Config value cannot be empty for STRING type")
                }
            }

            ConfigType.JSON -> {
                validateJsonConfig(value, errors)
            }
        }
        return errors
    }

    private fun validateJsonConfig(
        value: String,
        errors: MutableList<String>,
    ) {
        var isValid = true

        if (value.isBlank()) {
            errors.add("JSON value cannot be blank")
            isValid = false
        }

        if (value.toByteArray(Charsets.UTF_8).size > MAX_JSON_BYTES) {
            errors.add("JSON value exceeds maximum size of 64KB")
            isValid = false
        }

        val mapper = objectMapper.enable(JsonParser.Feature.ALLOW_SINGLE_QUOTES)
        if (isValid) {
            val node =
                try {
                    mapper.readTree(value)
                } catch (_: JsonProcessingException) {
                    errors.add("Value is not valid JSON")
                    return
                }

            if (!node.isObject && !node.isArray) {
                errors.add("JSON value must be a JSON object or array")
            }
        }
    }
}
