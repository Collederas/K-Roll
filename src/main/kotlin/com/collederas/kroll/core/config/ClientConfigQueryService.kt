package com.collederas.kroll.core.config

import com.collederas.kroll.core.config.versioning.ActiveVersionRepository
import com.collederas.kroll.core.config.versioning.ConfigVersionRepository
import com.collederas.kroll.exceptions.PublishedConfigNotFoundException
import org.springframework.dao.DataAccessException
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class ClientConfigQueryService(
    private val activeVersionRepository: ActiveVersionRepository,
    private val versionRepository: ConfigVersionRepository,
    private val resolver: ConfigResolver,
) {
    fun fetchPublishedConfig(envId: UUID): ClientConfigFetchResponseDto {
        try {
            val active =
                activeVersionRepository.findById(envId).orElseThrow {
                    PublishedConfigNotFoundException("No active version row for environment $envId")
                }

            val versionId =
                active.activeVersionId
                    ?: throw PublishedConfigNotFoundException("No published version for environment $envId")
            val publishedAt =
                active.publishedAt
                    ?: throw PublishedConfigNotFoundException("Published timestamp missing for environment $envId")

            val version =
                versionRepository.findById(versionId).orElseThrow {
                    PublishedConfigNotFoundException("Published version $versionId not found for environment $envId")
                }

            val values = resolver.resolvePublishedValues(envId)

            return ClientConfigFetchResponseDto(
                meta =
                    ClientConfigMetaDto(
                        schemaVersion = SCHEMA_VERSION,
                        activeSnapshotId = version.id,
                        activeSnapshotHash = version.contractHash,
                        publishedAt = publishedAt,
                        label = version.versionLabel.takeIf { it.isNotBlank() },
                    ),
                values = values,
            )
        } catch (_: DataAccessException) {
            throw PublishedConfigNotFoundException("Published configuration not found for environment $envId")
        } catch (_: IllegalStateException) {
            throw PublishedConfigNotFoundException("Published configuration not found for environment $envId")
        } catch (_: IllegalArgumentException) {
            throw PublishedConfigNotFoundException("Published configuration not found for environment $envId")
        }
    }

    companion object {
        private const val SCHEMA_VERSION = 1
    }
}
