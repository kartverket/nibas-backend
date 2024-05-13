package no.kartverket.nibas.service.deltgeometri

import no.kartverket.nibas.api.v1.common.Flatetype
import no.kartverket.nibas.config.SimpleAuthenticationToken
import no.kartverket.nibas.events.EventRad
import no.kartverket.nibas.kodelister.KodelisteService
import no.kartverket.nibas.service.KommuneService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDate
import no.kartverket.nibas.events.NibasEventPublisher
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder

/**
 * Danner delt geometri for en eller flere kommuner. Med andre ord sørger den for at stemmekretser og grunnkretser som gjelder for samme geometriske område
 * vil bruke de samme underliggende grensene, og ikke de dupliserte.
 *
 * Algoritmen er implementert slik at den ser isolert på en kommune om gangen. Så om man ønsker å danne delt geometri for flere kommuner kaller man bare
 * `dannDeltGeometriForKommune` for alle de kommunene man ønsker.
 *
 * Algoritmen fungerer på følgende måte:
 *      1. Vi henter ut alle grensene for grunnkretsene i en kommune
 *      2. Vi filtrerer så vi kun beholder de administrative grensene
 *      3. Vi gjør om grensene til punkter, slik at vi nå har en liste med punkter som representerer geometrien til skallet for den administrative enheten
 *      4. Vi henter ut alle stemmekretsgrenser, og filtrerer så vi beholder kun de administrative grensene
 *      5. Lager en liste med kun knutepunktene for stemmekrets sine administrative grenser
 *          5.1 Filtrerer denne lista så den kun inneholder punkter som ikke allerede er i listen over grensepunkter fra grunnkretser
 *      6. For hvert punkt i listen over - finn ut mellom hvilke 2 grensepunkter i listen laget i punkt 3. Insert dette punktet der
 *          6.1 Dette gjøres ved at man for hvert grensepunkt tegner en strek fra punktet til neste punkt, og ser om punktet man sjekker ligger på denne linjen
 *      7. Vi må nå gjøre grensepunktene om til grensestykker. Dette gjøres med en løkke:
 *         * Plukk første punkt som startpunkt på første grense
 *         * Så lenge vi har punkter som ikke er del av en grense:
 *              * Plukk grensepunkt fra listen og legg de til på grensen vi jobber på
 *              * Hvis punktet er et knutepunkt for enten stemmekretsgrenser eller grunnkretsgrenser:
 *                  * Sett punktet som sluttpunkt for grensen
 *                  * Legg grensen til i listen over grenser
 *                  * Start ny grense med dette punktet som startpunkt
 *      8. Vi må så bygge nye grenser for kretsene hvor vi bytter ut de administrative grensene med grenser som er delt mellom stemmekrets og grunnkrets
 *          * For hvert nytt grensestykke finner vi den/de gamle adm.grensene (for grunnkretser) som gjelder mellom samme 2 punkter
 *              * Hvis den gamle adm.grensa ikke skal splittes, beholdes den uendret
 *              * Hvis den gamle adm.grensa skal splittes, opprettes det en ny adm.grense som knyttes til ny versjon av de samme grunnkretsene
 *                som den gamle adm.grensa hadde (dvs også for grunnkretsen i nabokommunen).
 *                Det opprettes også ny versjon av kommune/fylke/nasjon, avhengig av grensetypen. Den gamle adm.grensa arkiveres
 *          * For hver gammel adm.grense for stemmekrets finner vi den/de nye grensene som gjelder mellom samme 2 punkter
 *              * Vi gjør ett breddeførst-søk fra startpunkt til sluttpunktet for grensen. Grensen(e) som gir korteste vei mellom disse punktene
 *                vil som regel være riktige grenser, i noen tilfeller må vi også sjekke et mellompunkt
 *              * Grensen(e) som skal erstatte en gammel adm.grense for stemmekrets knyttes til ny versjon av de samme stemmekretsene
 *  *             som den gamle adm.grensa hadde (dvs også for stemmekretsen i nabokommunen). Den gamle adm.grensa arkiveres
 *      9. Når man er ferdig med en kommune, vil også (deler) av grunnkretser og stemmekretser i nabokommunene ha fått oppdatert adm.grensene sine.
 *         Til slutt for kommunen dannes det flater for berørte grunnkretser, stemmekretser, kommuner, fylker og evt nasjon.
 *         Kjøringen committer til databasen for hver kommune.
 *         Når alle kommunene er prosessert, lages det en event med modified for alle de berørte flatene.
 */

@Service
class DeltGeometriService(
    val kommuneService: KommuneService,
    val kodelisteService: KodelisteService,
    val deltGeometriAdministrativeGrenserService: DeltGeometriAdministrativeGrenserService,
    val nibasEventPublisher: NibasEventPublisher
) {
    private val logger = LoggerFactory.getLogger(this.javaClass)

    fun dannDeltGeometriForAdministrativeGrenser() {
        SecurityContextHolder.getContext().authentication = SimpleAuthenticationToken(
            user = "deltgeometri",
            credentials = "DUMMY",
            authorities = listOf(SimpleGrantedAuthority("NIBAS"))
        )
        val gyldighetsdato = LocalDate.now()
        val alleKommuner = kommuneService.hentAlleKommuner(gyldighetsdato)
        val eventRaderForOppdaterteFlater = mutableMapOf<String, EventRad>()

        alleKommuner.mapIndexed { index, kommune ->
            val loggmelding = "[${index + 1}/${alleKommuner.size}] Danner delt geometri for"
            val (_, evtNyeEventRaderForFlater) =
                deltGeometriAdministrativeGrenserService.dannDeltGeometriForKommune(kommuneLokalid = kommune.lokalid, gyldighetsDato = gyldighetsdato,
                    eventRaderForAlleredeOppdaterteFlater = eventRaderForOppdaterteFlater, loggmelding = loggmelding)
            skrivLoggmelding(evtNyeEventRaderForFlater, kommune.kommunenummer)

            evtNyeEventRaderForFlater.forEach { eventRaderForOppdaterteFlater[it.key] = it.value }
        }

        nibasEventPublisher.publishNibasEvent(inntreffer = gyldighetsdato, redigerteFlater = eventRaderForOppdaterteFlater.values.toList())
    }

    private fun skrivLoggmelding(eventer: Map<String, EventRad>, kommunenr: String) {
        val antallGrunnkretser = eventer.values.count { it.flateType == Flatetype.GRUNNKRETS }
        val antallStemmekretser = eventer.values.count { it.flateType == Flatetype.STEMMEKRETS }
        val antallKommuner = eventer.values.count { it.flateType == Flatetype.KOMMUNE }
        val antallFylker = eventer.values.count { it.flateType == Flatetype.FYLKE }
        val antallNasjoner = eventer.values.count { it.flateType == Flatetype.NASJON }
        var infomelding = ""
        if (antallGrunnkretser > 0 || antallStemmekretser > 0 || antallKommuner > 0 || antallFylker > 0 || antallNasjoner > 0) {
            infomelding = "Antall oppdaterte for $kommunenr: "
        }
        if (antallGrunnkretser > 0) infomelding += " -grunnkretser: $antallGrunnkretser"
        if (antallStemmekretser > 0) infomelding += " -stemmekretser: $antallStemmekretser"
        if (antallKommuner > 0) infomelding += " -kommuner: $antallKommuner"
        if (antallFylker > 0) infomelding += " -fylker: $antallFylker"
        if (antallNasjoner > 0) infomelding += " -nasjoner: $antallNasjoner"
        logger.info(infomelding)
    }

}
