package com.collederas.kroll.core.configentry.versioning

import com.collederas.kroll.core.configentry.ConfigEntryEntity
import com.collederas.kroll.core.configentry.ConfigEntryRepository
import com.collederas.kroll.core.configentry.ConfigVersionDto
import com.collederas.kroll.core.environment.EnvironmentEntity
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID


@Service
class ConfigVersionService(
    private val configResolver: ConfigResolver,
    private val versionRepository: ConfigVersionRepository,
    private val contractHashCalculator: ContractHashCalculator
) {

    fun listVersions(): List<ConfigVersionDto> {
        return listOf()
    }

    @Transactional
    fun publishNewVersion(
        userId: UUID,
        envId: UUID,
        notes: String? = null
    )
    {
        val resolvedConfig = configResolver.resolveForEnvironment(envId)

//        valalidator.validate(resolvedConfig)

        val nextVersion = versionRepository.nextVersionForEnvironment(envId)
        val contractHash = contractHashCalculator.compute(resolvedConfig)

        val version = ConfigVersionEntity(
            environmentId = envId,
            version = nextVersion,
            contractHash = contractHash,
            createdBy = userId,
            notes = notes,
        )

        versionRepository.save(version)

//        snapshotRepository.save(
//            ConfigSnapshotEntity(
//                environmentId = envId,
//                version = nextVersion,
//                contractHash = contractHash,
//                snapshotJson = serialize(resolvedConfig),
//            )
        )

    fun delete()
    {

    }

    fun calcNextVersion(envId: UUID): String {
        return "${getActiveVersion()}"
    }
    fun getActiveVersion(): Int {
        //fetch active version from env
        return 0
    }

    fun computeContractHash(entries: List<ConfigEntryEntity>): String {
        return ""
    }
}
