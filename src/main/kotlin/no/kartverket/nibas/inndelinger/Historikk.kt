package no.kartverket.nibas.inndelinger

import no.kartverket.nibas.api.v1.response.RevisjonRef
import no.kartverket.nibas.common.Lokalid
import org.hibernate.envers.AuditReader
import org.hibernate.envers.DefaultRevisionEntity
import org.hibernate.envers.RevisionType
import org.hibernate.envers.query.AuditEntity
import org.hibernate.envers.query.criteria.AuditCriterion
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.servlet.support.ServletUriComponentsBuilder

@Transactional(readOnly = true)
@Service
class HistorikkService(val auditReader: AuditReader) {

    private fun Lokalid.createIdCriterion() = AuditEntity.property("lokalid").eq(this.value)

    fun <T> hentRevisjoner(id: String, type: Class<T>): List<Pair<DefaultRevisionEntity, RevisionType>> {
        return fetchRevisjoner(AuditEntity.id().eq(id), type)
    }

    fun <T> hentRevisjonerForLokalid(lokalid: Lokalid, type: Class<T>): List<Pair<DefaultRevisionEntity, RevisionType>> {
        return fetchRevisjoner(lokalid.createIdCriterion(), type)
    }

    fun <T> hentRevisjon(id: String, revisjon: Int, type: Class<T>): T {
        return fetchRevisjon(AuditEntity.id().eq(id), revisjon, type)
    }

    fun <T> hentRevisjonForLokalId(lokalid: Lokalid, revisjon: Int, type: Class<T>): T {
        return fetchRevisjon(lokalid.createIdCriterion(), revisjon, type)
    }

    fun <T> hentGjeldendeRevisjonsId(id: String, type: Class<T>): Int {
        val revisionData = auditReader.createQuery()
            .forRevisionsOfEntity(type, false, false)
            .add(AuditEntity.id().eq(id))
            .add(AuditEntity.revisionNumber().maximize())
            .singleResult as Array<*>

        return (revisionData[1] as DefaultRevisionEntity).id

    }

    @Suppress("UNCHECKED_CAST")
    private fun <T> fetchRevisjon(idCriterion: AuditCriterion, revisjon: Int, type: Class<T>): T {
        return auditReader.createQuery().forEntitiesAtRevision(type, revisjon)
            .add(idCriterion).singleResult as T
    }

    private fun <T> fetchRevisjoner(idCriterion: AuditCriterion, type: Class<T>): List<Pair<DefaultRevisionEntity, RevisionType>> {
        return auditReader.createQuery()
            .forRevisionsOfEntity(type, false, false)
            .add(idCriterion)
            .resultList.map {
                val arr = it as Array<*>
                Pair(arr[1] as DefaultRevisionEntity, arr[2] as RevisionType)
            }
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
