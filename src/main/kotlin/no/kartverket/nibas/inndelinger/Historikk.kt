package no.kartverket.nibas.inndelinger

import no.kartverket.nibas.api.v1.response.RevisjonRef
import org.hibernate.envers.AuditReader
import org.hibernate.envers.DefaultRevisionEntity
import org.hibernate.envers.RevisionType
import org.hibernate.envers.query.AuditEntity
import org.springframework.stereotype.Service
import org.springframework.web.servlet.support.ServletUriComponentsBuilder

@Service
class HistorikkService(val auditReader: AuditReader) {

    fun <T> hentRevisjoner(id: String, type: Class<T>): List<Pair<DefaultRevisionEntity, RevisionType>> {
        return auditReader.createQuery()
            .forRevisionsOfEntity(type, false, false)
            .add(AuditEntity.id().eq(id))
            .resultList.map {
                val arr = it as Array<*>
                Pair(arr[1] as DefaultRevisionEntity, arr[2] as RevisionType)
            }
    }

    @Suppress("UNCHECKED_CAST")
    fun <T> hentRevisjon(id: String, revisjon: Int, type: Class<T>): T {
        return auditReader.createQuery().forEntitiesAtRevision(type, revisjon)
            .add(AuditEntity.id().eq(id)).singleResult as T
    }

    fun <T> hentGjeldendeRevisjonsId(id: String, type: Class<T>): Int {
        val revisionData = auditReader.createQuery()
            .forRevisionsOfEntity(type, false, false)
            .add(AuditEntity.id().eq(id))
            .singleResult as Array<*>

        return (revisionData[1] as DefaultRevisionEntity).id

    }
}

fun toRevisjonRef(revision: Pair<DefaultRevisionEntity, RevisionType>) =
    RevisjonRef(
        revision.first.id,
        revision.first.revisionDate,
        toRevisjonStatus(revision.second),
        ServletUriComponentsBuilder.fromCurrentRequestUri().pathSegment(revision.first.id.toString()).build()
            .toUriString()
    )


private fun toRevisjonStatus(revType: RevisionType): RevisjonStatus {
    return when (revType) {
        RevisionType.ADD -> RevisjonStatus.NY
        RevisionType.MOD -> RevisjonStatus.ENDRET
        RevisionType.DEL -> RevisjonStatus.SLETTET
    }
}

enum class RevisjonStatus {
    NY, ENDRET, SLETTET
}
