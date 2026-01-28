package com.collederas.kroll.config

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class JsonConfig {

    @Bean
    @Qualifier("strictJsonMapper")
    fun strictJsonMapper(): ObjectMapper {
        return ObjectMapper()
            .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
    }
}
