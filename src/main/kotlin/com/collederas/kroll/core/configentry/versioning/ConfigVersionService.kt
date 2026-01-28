package com.collederas.kroll.core.configentry.versioning

import com.collederas.kroll.core.configentry.ConfigDiff
import com.collederas.kroll.core.configentry.ConfigResolver
import com.collederas.kroll.core.configentry.ConfigVersionDto
import com.collederas.kroll.core.configentry.VersionDetailsDto
import com.collederas.kroll.core.configentry.versioning.snapshot.ConfigSnapshotEntity
import com.collederas.kroll.core.configentry.versioning.snapshot.ConfigSnapshotRepository
import com.collederas.kroll.crypto.Sha256
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.*


@Service
class ConfigVersionService(
    private val configResolver: ConfigResolver,
    private val versionRepository: ConfigVersionRepository,
    private val activeVersionRepository: ActiveVersionRepository,
    private val snapshotRepository: ConfigSnapshotRepository,
    private val objectMapper: ObjectMapper
) {

    @PreAuthorize("@envAuth.isOwner(#envId, authentication.principal.userId)")
    fun listAllVersions(envId: UUID): List<ConfigVersionDto> {
        val versions = versionRepository.findAllByEnvironmentId(envId)
        val activeVersion =
            activeVersionRepository.findByEnvironmentId(envId)?.version

        return versions.map {
            ConfigVersionDto(
                id = it.id.toString(),
                version = it.version,
                environmentId = it.environmentId,
                createdAt = it.createdAt,
                createdBy = it.createdBy,
                isActive = it.version == activeVersion,
                contractHash = it.contractHash,
                notes = it.notes,
            )
        }
    }

    @PreAuthorize("@envAuth.isOwner(#envId, authentication.principal.userId)")
    fun listActiveVersions(envId: UUID): List<ConfigVersionDto> {
        val versions = versionRepository.findByEnvironmentId(envId)
        val activeVersion =
            activeVersionRepository.findByEnvironmentId(envId)?.version

        return versions.map {
            ConfigVersionDto(
                id = it.id.toString(),
                version = it.version,
                environmentId = it.environmentId,
                createdAt = it.createdAt,
                createdBy = it.createdBy,
                isActive = it.version == activeVersion,
                contractHash = it.contractHash,
                notes = it.notes,
            )
        }
    }

    @Transactional
    @PreAuthorize("@envAuth.isOwner(#envId, authentication.principal.userId)")
    fun publishNewVersion(
        userId: UUID,
        envId: UUID,
        notes: String? = null
    ) {
        val resolvedConfig = configResolver.resolveForEnvironment(envId)

//        valalidator.validate(resolvedConfig)

        val nextVersion = getNextVersionString(envId)

        val contract =
            resolvedConfig.values
                .mapValues { it.value.type.name }

        val bytes = objectMapper.writeValueAsBytes(contract)
        val contractHash = Sha256.hashHex(bytes)

        val version = ConfigVersionEntity(
            environmentId = envId,
            version = nextVersion,
            contractHash = contractHash,
            createdBy = userId,
            notes = notes,
        )

        versionRepository.save(version)

        snapshotRepository.save(
            ConfigSnapshotEntity(
                environmentId = envId,
                version = nextVersion,
                contractHash = contractHash,
                snapshotJson = objectMapper.writeValueAsString(resolvedConfig),
            )
        )
    }

    @Transactional
    @PreAuthorize("@envAuth.isOwner(#envId, authentication.principal.userId)")
    fun promoteVersion(
        envId: UUID,
        version: String,
        promotedBy: UUID,
    ) {
        require(
            versionRepository.existsByEnvironmentIdAndVersion(envId, version)
        ) { "Version does not exist" }

        require(
            snapshotRepository.existsByEnvironmentIdAndVersion(envId, version)
        ) { "Snapshot does not exist for version" }

        val existing =
            activeVersionRepository.findByEnvironmentId(envId)

        if (existing == null) {
            activeVersionRepository.save(
                ActiveVersionEntity(
                    environmentId = envId,
                    version = version,
                    updatedBy = promotedBy,
                    updatedAt = Instant.now(),
                )
            )
        } else {
            existing.version = version
            existing.updatedAt = Instant.now()
            existing.updatedBy = promotedBy
        }

        // TODO: emit audit event
    }

    @Transactional
    @PreAuthorize("@envAuth.isOwner(#envId, authentication.principal.userId)")
    fun rollbackToVersion(envId: UUID, version: String, userId: UUID) {
        promoteVersion(envId, version, userId)
    }

    @PreAuthorize("@envAuth.isOwner(#envId, authentication.principal.userId)")
    fun getActiveVersion(envId: UUID): ConfigVersionEntity? {
        val active =
            activeVersionRepository.findByEnvironmentId(envId)
                ?: return null

        return versionRepository.findByEnvironmentIdAndVersion(
            envId,
            active.version
        )
    }

    @PreAuthorize("@envAuth.isOwner(#envId, authentication.principal.userId)")
    fun getVersionDetails(
        envId: UUID,
        version: String,
    ): VersionDetailsDto {
        val versionEntity =
            versionRepository.findByEnvironmentIdAndVersion(envId, version)
                ?: error("Version not found")

        val snapshot =
            snapshotRepository.findByEnvironmentIdAndVersion(envId, version)
                ?: error("Snapshot missing for version")

        return VersionDetailsDto(
            version = versionEntity.version,
            createdAt = versionEntity.createdAt,
            createdBy = versionEntity.createdBy,
            contractHash = versionEntity.contractHash,
            notes = versionEntity.notes,
            snapshotJson = snapshot.snapshotJson,
        )
    }

    @PreAuthorize("@envAuth.isOwner(#envId, authentication.principal.userId)")
    fun diffVersions(
        envId: UUID,
        fromVersion: String,
        toVersion: String,
    ): ConfigDiff {
        val fromSnapshot =
            snapshotRepository.findByEnvironmentIdAndVersion(envId, fromVersion)
                ?: error("From-version snapshot missing")

        val toSnapshot =
            snapshotRepository.findByEnvironmentIdAndVersion(envId, toVersion)
                ?: error("To-version snapshot missing")

        val fromMap: Map<String, Any> =
            objectMapper.readValue(fromSnapshot.snapshotJson)

        val toMap: Map<String, Any> =
            objectMapper.readValue(toSnapshot.snapshotJson)

        val fromKeys = fromMap.keys
        val toKeys = toMap.keys

        val added = toKeys - fromKeys
        val removed = fromKeys - toKeys

        val typeChanged =
            fromKeys.intersect(toKeys)
                .filter { key ->
                    fromMap[key]?.javaClass != toMap[key]?.javaClass
                }
                .toSet()

        return ConfigDiff(
            added = added,
            removed = removed,
            typeChanged = typeChanged,
        )
    }

    fun getNextVersionString(environmentId: UUID): String {
        val latest = versionRepository.findLatestVersionByEnvironmentId(environmentId)?.version ?: "v0"
        val numeric = latest.removePrefix("v").toInt()
        return "v${numeric + 1}"
    }

}
