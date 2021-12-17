package no.kartverket.nibas.service.historikk

import no.kartverket.nibas.api.v1.common.AdministrativEnhetType
import no.kartverket.nibas.domain.*
import no.kartverket.nibas.domain.historikk.GeometriHistorikkObjekt
import no.kartverket.nibas.domain.historikk.HistorikkObjekt
import no.kartverket.nibas.domain.historikk.HistorikkStatus
import no.kartverket.nibas.domain.historikk.InfoObjekt
import org.hibernate.envers.AuditReader
import org.hibernate.envers.RevisionType
import org.hibernate.envers.query.AuditEntity
import org.springframework.stereotype.Service

@Service
class HistorikkService(val auditReader: AuditReader) {

    @Throws(Exception::class)
    fun hentHistorikkForType(id: Int, type: AdministrativEnhetType): List<HistorikkObjekt> {
        val clazz = hentAdministrativEnhetKlasseForType(type)

        val historikkRaadata = hentHistorikkRaadata(clazz, id)

        return historikkRaadata.map{
            val array = it as Array<*>
            when(val historikkObjekt = array[0]) {
                is AdministrativEnhet -> {
                    HistorikkObjekt(
                        historikkObjekt.administrativenhetnavn.elementAt(0).navn!!,
                        InfoObjekt(type, historikkObjekt.id!!),
                        historikkObjekt.posisjon!!,
                        historikkObjekt.flate!!,
                        toHistorikkStatus(array[2] as RevisionType)
                    )
                }
                else -> throw RuntimeException("Henter historikk for ugyldig type")
            }
        }

    }

    @Throws(Exception::class)
    fun hentAllHistorikkForType(type: AdministrativEnhetType): List<HistorikkObjekt> {
        val clazz = hentAdministrativEnhetKlasseForType(type)

        val historikkRaadata = hentAlleHistorikkRaadata(clazz)

        return historikkRaadata.map{
            val array = it as Array<*>
            when(val historikkObjekt = array[0]) {
                is AdministrativEnhet -> {
                    HistorikkObjekt(
                        historikkObjekt.administrativenhetnavn.elementAt(0).navn!!,
                        InfoObjekt(AdministrativEnhetType.KOMMUNE, historikkObjekt.id!!),
                        historikkObjekt.posisjon!!,
                        historikkObjekt.flate!!,
                        toHistorikkStatus(array[2] as RevisionType)
                    )
                }
                else -> throw RuntimeException("Henter historikk for ugyldig type")
            }
        }
    }

    @Throws(Exception::class)
    fun hentGrenseHistorikkForType(id: Int, type: AdministrativEnhetType): List<GeometriHistorikkObjekt> {
        val clazz = hentGrenseKlasseForType(type)

        val historikkRaadata = hentHistorikkRaadata(clazz, id)
        return historikkRaadata.map{
            val array = it as Array<*>
            when(val historikkObjekt = array[0]) {
                is AdministrativGrense -> {
                    GeometriHistorikkObjekt(
                        InfoObjekt(AdministrativEnhetType.KOMMUNE, historikkObjekt.id!!),
                        historikkObjekt.grense!!,
                        toHistorikkStatus(array[2] as RevisionType)
                    )
                }
                else -> throw RuntimeException("Henter grense-historikk for ugyldig type")
            }
        }

    }

    private fun hentAlleHistorikkRaadata(clazz: Class<*>): List<*> {
        return hentHistorikkRaadata(clazz, null)
    }

    private fun hentHistorikkRaadata(
        clazz: Class<*>,
        id: Int?
    ): List<*> {
        val q = auditReader.createQuery().forRevisionsOfEntity(clazz, false, false)
        id?.let { q.add(AuditEntity.id().eq(it)) }
        return q.resultList as List<*>
    }

    private fun hentAdministrativEnhetKlasseForType(type: AdministrativEnhetType): Class<out AdministrativEnhet> {
        return when (type) {
            AdministrativEnhetType.KOMMUNE -> Kommune::class.java
            AdministrativEnhetType.FYLKE -> Fylke::class.java
            AdministrativEnhetType.NASJON -> Nasjon::class.java
        }
    }

    private fun hentGrenseKlasseForType(type: AdministrativEnhetType): Class<out Grense> {
        return when (type) {
            AdministrativEnhetType.KOMMUNE -> Kommunegrense::class.java
            AdministrativEnhetType.FYLKE -> Fylkesgrense::class.java
            AdministrativEnhetType.NASJON -> Riksgrense::class.java
        }
    }

    private fun toHistorikkStatus(revType: RevisionType): HistorikkStatus {
        return when(revType) {
            RevisionType.ADD -> HistorikkStatus.NY
            RevisionType.MOD -> HistorikkStatus.ENDRET
            RevisionType.DEL -> HistorikkStatus.SLETTET
        }
    }

}