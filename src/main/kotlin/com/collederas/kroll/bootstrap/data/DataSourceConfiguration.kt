package com.collederas.kroll.bootstrap.data

import com.collederas.kroll.bootstrap.secrets.SecretResolver
import com.zaxxer.hikari.HikariDataSource
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.env.Environment
import javax.sql.DataSource

@Configuration
class DataSourceConfiguration {
    @Bean
    fun dataSource(env: Environment): DataSource {
        val passwordProperty = env.getProperty("spring.datasource.password")
        val password = SecretResolver.resolve(passwordProperty, "KROLL_DB_PASSWORD")

        return HikariDataSource().apply {
            jdbcUrl = env.getRequiredProperty("spring.datasource.url")
            username = env.getRequiredProperty("spring.datasource.username")
            this.password = password
            driverClassName = env.getRequiredProperty("spring.datasource.driver-class-name")
        }
    }
}
