package com.collederas.kroll.core.config.validation

import com.collederas.kroll.core.config.entry.ConfigType
import com.fasterxml.jackson.databind.JsonNode
import org.springframework.stereotype.Component

@Component
class ConfigDraftValidator(
    private val entryValidator: ConfigEntryValidator,
) {
    fun validate(draft: JsonNode): List<String> {
        val errors = mutableListOf<String>()

        if (!draft.isObject) {
            errors.add("Draft must be a JSON object")
            return errors
        }

        val valuesNode = draft["values"] ?: return errors
        if (!valuesNode.isObject) {
            errors.add("'values' must be a JSON object")
            return errors
        }

        valuesNode.properties().forEach { (key, entry) ->
            val typeText = entry["type"]?.asText()
            val valueText = entry["value"]?.asText()

            if (typeText == null || valueText == null) {
                errors.add("Entry '$key' must contain type and value")
                return@forEach
            }

            val type =
                try {
                    ConfigType.valueOf(typeText)
                } catch (_: Exception) {
                    errors.add("Entry '$key' has invalid type '$typeText'")
                    return@forEach
                }

            entryValidator
                .validate(valueText, type)
                .forEach { msg ->
                    errors.add("Entry '$key': $msg")
                }
        }

        return errors
    }
}
