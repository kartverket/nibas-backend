package no.kartverket.nibas.service.avrundkoordinater

import no.kartverket.nibas.config.SimpleAuthenticationToken
import no.kartverket.nibas.events.EventRad
import no.kartverket.nibas.events.NibasEventPublisher
import no.kartverket.nibas.repository.FylkeRepository
import no.kartverket.nibas.repository.GrenseRepository
import no.kartverket.nibas.repository.GrunnkretsRepository
import no.kartverket.nibas.repository.KommuneRepository
import no.kartverket.nibas.repository.NasjonRepository
import no.kartverket.nibas.repository.StemmekretsRepository
import no.kartverket.nibas.service.GrenseService
import no.kartverket.nibas.service.GrunnkretsService
import no.kartverket.nibas.service.KommuneService
import no.kartverket.nibas.service.StemmekretsService
import no.kartverket.nibas.service.deltgeometri.DeltGeometriService
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import java.time.LocalDate

/**
 * Avrunder koordinater for alle kommuner. Algoritmen er implementert slik at den ser isolert på en kommune om gangen.
 *
 * Algoritmen fungerer på følgende måte:
 *      1. Vi henter ut alle grensene for grunnkretsene i en kommune
 *      2. Vi filtrerer så vi kun beholder grensene med koordinater som skal avrundes
 *      3. Vi lager ny versjon av grensa med avrundede koordinater
 *      4. Vi lager ny versjon av grensa sine grunnkretser. Hvis grensa er en administrativ grense lages det også en ny versjon av grensa sine stemmekretser,
 *      og kommuner, fylker og nasjon.
 *      5. Den nye grensa knyttes til aktuell krets.
 *      6. Gjenta punkt 1-5 for de indre grensene for stemmekretsene i kommunen.
 *      7. Når man er ferdig med en kommune, vil også (deler) av grunnkretser og stemmekretser i nabokommunene ha fått oppdatert grensene sine.
 *         Kjøringen committer til databasen for hver kommune.
 *
 *      Når alle kommunene er prosessert dannes det flater for berørte grunnkretser, stemmekretser, kommuner, fylker og evt nasjon.
 *      Deretter lages det en event med modified for alle de berørte flatene.
 */

@Service
class AvrundKoordinaterService(
    val kommuneService: KommuneService,
    val grenseService: GrenseService,
    val grenseRepository: GrenseRepository,
    val grunnkretsService: GrunnkretsService,
    val grunnkretsRepository: GrunnkretsRepository,
    val stemmekretsService: StemmekretsService,
    val stemmekretsRepository: StemmekretsRepository,
    val kommuneRepository: KommuneRepository,
    val fylkeRepository: FylkeRepository,
    val nasjonRepository: NasjonRepository,
    val avrundKoordinaterKommuneService: AvrundKoordinaterKommuneService,
    val deltGeometriService: DeltGeometriService,
    val nibasEventPublisher: NibasEventPublisher
) {

    fun avrundKoordinaterForAlleGrenser() {
        SecurityContextHolder.getContext().authentication = SimpleAuthenticationToken(
            user = "avrundKoordinater",
            credentials = "DUMMY",
            authorities = listOf(SimpleGrantedAuthority("NIBAS"))
        )
        val gyldighetsdato = LocalDate.now()
        val alleKommuner = kommuneService.hentAlleKommuner(gyldighetsdato)
        val eventRaderForOppdaterteFlater = mutableMapOf<String, EventRad>()

        alleKommuner.mapIndexed { index, kommune ->
            val loggmelding = "[${index + 1}/${alleKommuner.size}] Avrunder koordinater for"
            val evtNyeEventRaderForFlater =
                avrundKoordinaterKommuneService.avrundKoordinaterForKommune(
                    kommuneLokalid = kommune.lokalid, gyldighetsDato = gyldighetsdato,
                    eventRaderForAlleredeOppdaterteFlater = eventRaderForOppdaterteFlater, loggmelding = loggmelding
                )
            deltGeometriService.skrivLoggmelding(evtNyeEventRaderForFlater, kommune.kommunenummer)

            evtNyeEventRaderForFlater.forEach { eventRaderForOppdaterteFlater[it.key] = it.value }
        }

        avrundKoordinaterKommuneService.oppdaterAlleFlaterOgLoggEvtFeil(eventRaderForOppdaterteFlater)

        nibasEventPublisher.publishNibasEvent(
            inntreffer = gyldighetsdato,
            redigerteFlater = eventRaderForOppdaterteFlater.values.toList()
        )
    }

}
