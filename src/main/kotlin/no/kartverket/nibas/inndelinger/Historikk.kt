package no.kartverket.nibas.inndelinger

import no.kartverket.nibas.api.v1.response.HistorikkRef
import org.hibernate.envers.AuditReader
import org.hibernate.envers.DefaultRevisionEntity
import org.hibernate.envers.RevisionType
import org.hibernate.envers.query.AuditEntity
import org.springframework.stereotype.Service
import org.springframework.web.servlet.support.ServletUriComponentsBuilder

@Service
class HistorikkService(val auditReader: AuditReader) {

    fun <T> hentHistorikk(id: Int, type: Class<T>): List<Pair<DefaultRevisionEntity, RevisionType>> {
        return auditReader.createQuery()
            .forRevisionsOfEntity(type, false, false)
            .add(AuditEntity.id().eq(id))
            .resultList.map {
                val arr = it as Array<*>
                Pair(arr[1] as DefaultRevisionEntity, arr[2] as RevisionType)
            }
    }

    @Suppress("UNCHECKED_CAST")
    fun <T> hentHistorikkRevisjon(id: Int, revision: Int, type: Class<T>): T {
        return auditReader.createQuery().forEntitiesAtRevision(type, revision)
            .add(AuditEntity.id().eq(id)).singleResult as T
    }
}

fun toHistorikkRef(revision: Pair<DefaultRevisionEntity, RevisionType>) =
    HistorikkRef(
        revision.first.id,
        revision.first.revisionDate,
        toHistorikkStatus(revision.second),
        ServletUriComponentsBuilder.fromCurrentRequestUri().pathSegment(revision.first.id.toString()).build()
            .toUriString()
    )


fun toHistorikkStatus(revType: RevisionType): HistorikkStatus {
    return when (revType) {
        RevisionType.ADD -> HistorikkStatus.NY
        RevisionType.MOD -> HistorikkStatus.ENDRET
        RevisionType.DEL -> HistorikkStatus.SLETTET
    }
}

enum class HistorikkStatus {
    NY, ENDRET, SLETTET
}
