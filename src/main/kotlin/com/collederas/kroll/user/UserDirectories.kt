package com.collederas.kroll.user

import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Service
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

@Service
@Primary
class CachedUserDirectory(
    private val delegate: JpaUserDirectory
    // TODO: meter registry for metrics cache hit/miss instead of logs
    // meterRegistry: MeterRegistry
) : UserDirectory {

    private val log = LoggerFactory.getLogger(javaClass)
    //    private val hits = meterRegistry.counter("user_directory.cache.hit")
    //    private val misses = meterRegistry.counter("user_directory.cache.miss")

    private val cache = ConcurrentHashMap<UUID, UserDisplayName>()


    override fun resolveDisplayName(userId: UUID): String? {
        val entry = cache.computeIfAbsent(userId) {
            log.debug("UserDirectory cache MISS for userId={}", userId)
            delegate.resolveDisplayName(it)
                ?.let(UserDisplayName::Known)
                ?: UserDisplayName.Unknown
        }

        log.debug("UserDirectory cache HIT for userId={}", userId)

        return when (entry) {
            is UserDisplayName.Known -> entry.value
            UserDisplayName.Unknown -> null
        }
    }

    override fun resolveDisplayNames(userIds: Set<UUID>): Map<UUID, String> {
        val missing = userIds - cache.keys

        if (missing.isNotEmpty()) {
            log.debug(
                "UserDirectory batch MISS for userIds={}",
                missing.joinToString()
            )
            delegate.resolveDisplayNames(missing).forEach { (id, name) ->
                cache[id] = UserDisplayName.Known(name)
            }
            missing.forEach { id ->
                cache.putIfAbsent(id, UserDisplayName.Unknown)
            }
        }

        val hits = userIds - missing
        if (hits.isNotEmpty()) {
            log.debug(
                "UserDirectory batch HIT for userIds={}",
                hits.joinToString()
            )
        }

        return userIds.mapNotNull { id ->
            (cache[id] as? UserDisplayName.Known)?.let { id to it.value }
        }.toMap()
    }
}


@Service
class JpaUserDirectory(
    private val userRepository: AppUserRepository
) : UserDirectory {

    override fun resolveDisplayName(userId: UUID): String? =
        userRepository.findById(userId)
            .map { it.username }
            .orElse(null)

    override fun resolveDisplayNames(userIds: Set<UUID>): Map<UUID, String> =
        if (userIds.isEmpty()) {
            emptyMap()
        } else {
            userRepository.findAllById(userIds)
                .associate { it.id to it.username }
        }
}

sealed class UserDisplayName {
    data class Known(val value: String) : UserDisplayName()
    object Unknown : UserDisplayName()
}
