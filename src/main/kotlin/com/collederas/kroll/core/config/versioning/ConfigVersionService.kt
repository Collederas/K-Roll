package com.collederas.kroll.core.config.versioning

import com.collederas.kroll.core.config.ChangedKeyDto
import com.collederas.kroll.core.config.ConfigDiffDto
import com.collederas.kroll.core.config.ConfigResolver
import com.collederas.kroll.core.config.ConfigVersionDto
import com.collederas.kroll.core.config.ResolveMode
import com.collederas.kroll.core.config.VersionDetailsDto
import com.collederas.kroll.core.config.diff.ConfigDiffCalculator
import com.collederas.kroll.core.config.diff.DiffResult
import com.collederas.kroll.core.config.versioning.snapshot.ConfigSnapshotEntity
import com.collederas.kroll.core.config.versioning.snapshot.ConfigSnapshotRepository
import com.collederas.kroll.crypto.Sha256
import com.fasterxml.jackson.databind.ObjectMapper
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
    private val configDiffCalculator: ConfigDiffCalculator,
    private val objectMapper: ObjectMapper,
) {
    @PreAuthorize("@envAuth.isOwner(#envId, authentication.principal.userId)")
    fun listAllVersions(envId: UUID): List<ConfigVersionDto> {
        val versions = versionRepository.findAllByEnvironmentIdOrderByVersionSequenceDesc(envId)
        val active =
            activeVersionRepository.findByEnvironmentId(envId)

        val activeVersionId = active.activeVersionId

        return versions.map { version ->
            ConfigVersionDto(
                id = version.id,
                versionLabel = version.versionLabel,
                versionSequence = version.versionSequence,
                environmentId = version.environmentId,
                createdAt = version.createdAt,
                createdBy = version.createdBy,
                createdByName = "",
                isActive = version.id == activeVersionId,
                contractHash = version.contractHash,
                changeLog = version.changeLog,
                publishedAt = active.publishedAt.takeIf { version.id == activeVersionId },
                parentHash = version.parentHash,
            )
        }
    }

    @Transactional
    @PreAuthorize("@envAuth.isOwner(#envId, authentication.principal.userId)")
    fun publishNewVersion(
        userId: UUID,
        envId: UUID,
        notes: String? = null,
    ) {
        val resolvedConfig = configResolver.resolveForEnvironment(envId, ResolveMode.DRAFT)

        // TODO: defensive validate on publish
        // validator.validate(resolvedConfig)

        val latest = versionRepository.findLatestByEnvironmentId(envId)

        val nextVersion = (latest?.versionSequence ?: 0L) + 1
        val nextVersionLabel = "v$nextVersion"
        val parentHash = latest?.contractHash

        val contract =
            resolvedConfig.values
                .mapValues { it.value.type.name }

        val bytes = objectMapper.writeValueAsBytes(contract)
        val contractHash = Sha256.hashHex(bytes)

        val version =
            ConfigVersionEntity(
                environmentId = envId,
                versionSequence = nextVersion,
                versionLabel = nextVersionLabel,
                contractHash = contractHash,
                parentHash = parentHash,
                createdBy = userId,
                changeLog = notes,
            )

        versionRepository.save(version)

        val snapshotJson =
            objectMapper.writeValueAsString(resolvedConfig)

        // TODO: calculate and store diff payload
        snapshotRepository.save(
            ConfigSnapshotEntity(
                versionId = version.id,
                snapshotJson = snapshotJson,
            ),
        )
    }

    @Transactional
    @PreAuthorize("@envAuth.isOwner(#envId, authentication.principal.userId)")
    fun promoteVersion(
        envId: UUID,
        versionId: UUID,
        promotedBy: UUID,
    ) {
        val version =
            versionRepository
                .findById(versionId)
                .orElseThrow { IllegalArgumentException("Version does not exist") }

        require(version.environmentId == envId) {
            "Version does not belong to environment"
        }

        require(snapshotRepository.existsById(versionId)) {
            "Snapshot does not exist for version"
        }

        val now = Instant.now()

        val active =
            activeVersionRepository.findLocked(envId)

        active.activeVersionId = versionId
        active.publishedAt = now
        active.publishedBy = promotedBy

        active.draftJson = null
        active.draftUpdatedAt = null
        active.draftUpdatedBy = null

        // TODO: emit audit event
    }

    @Transactional
    @PreAuthorize("@envAuth.isOwner(#envId, authentication.principal.userId)")
    fun createInitialVersion(
        userId: UUID,
        envId: UUID,
        notes: String?,
    ): UUID {
        // Resolve draft config
        val resolvedConfig =
            configResolver.resolveForEnvironment(envId, ResolveMode.DRAFT)

        // Determine next version metadata
        val latest = versionRepository.findLatestByEnvironmentId(envId)
        val nextSequence = (latest?.versionSequence ?: 0L) + 1
        val versionLabel = "v$nextSequence"
        val parentHash = latest?.contractHash

        // Compute contract hash
        val contract =
            resolvedConfig.values.mapValues { it.value.type.name }

        val contractBytes = objectMapper.writeValueAsBytes(contract)
        val contractHash = Sha256.hashHex(contractBytes)

        // Persist version
        val version =
            versionRepository.save(
                ConfigVersionEntity(
                    environmentId = envId,
                    versionSequence = nextSequence,
                    versionLabel = versionLabel,
                    contractHash = contractHash,
                    parentHash = parentHash,
                    createdBy = userId,
                    changeLog = notes,
                ),
            )

        // Persist snapshot
        val snapshotJson =
            objectMapper.writeValueAsString(resolvedConfig)

        snapshotRepository.save(
            ConfigSnapshotEntity(
                versionId = version.id,
                snapshotJson = snapshotJson,
            ),
        )

        return version.id
    }

    @Transactional
    @PreAuthorize("@envAuth.isOwner(#envId, authentication.principal.userId)")
    fun rollbackToVersion(
        envId: UUID,
        versionId: UUID,
        userId: UUID,
    ) {
        promoteVersion(envId, versionId, userId)
    }

    @PreAuthorize("@envAuth.isOwner(#envId, authentication.principal.userId)")
    fun getActiveVersion(envId: UUID): ConfigVersionEntity? {
        val active =
            activeVersionRepository.findByEnvironmentId(envId)
                ?: return null

        val versionId =
            active.activeVersionId
                ?: return null // DB has constraint on this, should never happen

        return versionRepository.findById(versionId).orElse(null)
    }

    @PreAuthorize("@envAuth.isOwner(#envId, authentication.principal.userId)")
    fun getVersionDetails(
        envId: UUID,
        versionId: UUID,
    ): VersionDetailsDto {
        val version =
            versionRepository
                .findById(versionId)
                .orElseThrow { IllegalArgumentException("Version not found") }

        require(version.environmentId == envId) {
            "Version does not belong to environment"
        }

        val snapshot =
            snapshotRepository
                .findById(versionId)
                .orElseThrow { IllegalStateException("Snapshot missing for version") }

        return VersionDetailsDto(
            id = version.id,
            versionSequence = version.versionSequence,
            versionLabel = version.versionLabel,
            createdAt = version.createdAt,
            createdBy = version.createdBy,
            createdByName = "",
            contractHash = version.contractHash,
            parentHash = version.parentHash,
            changeLog = version.changeLog,
            snapshotJson = snapshot.snapshotJson,
            diffPayload = snapshot.diffPayload,
        )
    }

    @PreAuthorize("@envAuth.isOwner(#envId, authentication.principal.userId)")
    fun diffVersions(
        envId: UUID,
        fromVersionId: UUID,
        toVersionId: UUID,
    ): ConfigDiffDto {
        val fromVersion =
            versionRepository
                .findById(fromVersionId)
                .orElseThrow { IllegalArgumentException("From-version not found") }

        val toVersion =
            versionRepository
                .findById(toVersionId)
                .orElseThrow { IllegalArgumentException("To-version not found") }

        require(fromVersion.environmentId == envId)
        require(toVersion.environmentId == envId)

        val fromSnapshot =
            snapshotRepository
                .findById(fromVersionId)
                .orElseThrow { IllegalStateException("From-version snapshot missing") }

        val toSnapshot =
            snapshotRepository
                .findById(toVersionId)
                .orElseThrow { IllegalStateException("To-version snapshot missing") }

        // DIFF!
        val diffs =
            configDiffCalculator.diffSnapshots(fromSnapshot, toSnapshot)

        val added = mutableSetOf<String>()
        val removed = mutableSetOf<String>()
        val typeChanged = mutableListOf<ChangedKeyDto>()
        val valueChanged = mutableListOf<ChangedKeyDto>()

        diffs.forEach { diff ->
            when (diff) {
                is DiffResult.Added ->
                    added += diff.key

                is DiffResult.Removed ->
                    removed += diff.key

                is DiffResult.Changed -> {
                    val dto =
                        ChangedKeyDto(
                            key = diff.key,
                            oldType = diff.old.type.name,
                            newType = diff.new.type.name,
                            oldValue = diff.old.value,
                            newValue = diff.new.value,
                        )

                    if (diff.old.type != diff.new.type) {
                        typeChanged += dto
                    } else {
                        valueChanged += dto
                    }
                }
            }
        }

        return ConfigDiffDto(
            fromVersion = fromVersion.versionLabel,
            toVersion = toVersion.versionLabel,
            added = added,
            removed = removed,
            typeChanged = typeChanged,
            valueChanged = valueChanged,
        )
    }
}
