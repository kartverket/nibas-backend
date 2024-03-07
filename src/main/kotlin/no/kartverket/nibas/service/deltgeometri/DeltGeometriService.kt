package no.kartverket.nibas.service.deltgeometri

import no.kartverket.nibas.common.getInSpraak
import no.kartverket.nibas.domain.GrunnkretsGrenseKobling
import no.kartverket.nibas.domain.Kommune
import no.kartverket.nibas.domain.StemmekretsGrenseKobling
import no.kartverket.nibas.kodelister.KodelisteService
import no.kartverket.nibas.service.KommuneService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

/**
 * Danner delt geometri for en eller flere kommuner. Med andre ord sørger den for at stemmekretser og grunnkretser som gjelder for samme geometriske område
 * vil bruke de samme underliggende grensene, og ikke de dupliserte.
 *
 * Algoritmen er implementert slik at den ser isolert på en kommune om gangen. Så om man ønsker å danne delt geometri for flere kommuner kaller man bare
 * `dannDeltGeometriForKommune` for alle de kommunene man ønsker. Om man kaller den for 2 nabokommuner vil de delte grensene bli "fikset" 2 ganger. Dette er
 * ikke optimalt. Men siden den i første omgang kun skal kjøre som en del av import er ytelse ikke det viktigste fokuset her.
 *
 * Algoritmen fungerer på følgende måte:
 *      1. Vi henter ut alle grensene for grunnkretsene i en kommune
 *      2. Vi filtrere så vi kun beholder de administrative grensene
 *      3. Vi gjør om grensene til grensepunkter, slik at vi nå har en liste med grensepunkter som representerer geometrien til skallet for den administrative enheten
 *      4. Vi henter ut alle stemmekretsgrenser, og filtrerer så vi beholder kun de administrative grensene
 *      5. Lager en liste med kun knutepunktene for stemmekrets sine administrative grenser
 *          5.1 Filtrerer denne lista så den kun inneholder punkter som ikke allerede er i listen over grensepunkter fra grunnkretser
 *      6. For hvert punkt i listen over finn ut mellom hvilke 2 grensepunkter i listen laget i punkt 3. Insert dette punktet der
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
 *          * For hver (gammel) administrative grense finner vi de nye grensene som gjelder mellom samme 2 punkter
 *          * Vi gjør ett breddeførst-søk fra startpunkt til sluttpunktet for grensen. Grensen(e) som gir korteste vei mellom disse punktene vil være riktige grenser
 *      9. For hver grense må man nå oppdatere grensekobling for både stemmekretser og grunnkretser denne er tilknyttet.
 *          * Vi kan finne ut hvilken kretser en grense deler ved å velge 2 punkter i grensen, og finne den originale grensen med samme 2 punkter - grensen vil være koblet mot samme kretser
 *              * Dette fungerer siden eneste endringen som blir gjort ved delt geometri er å potensielt splitte enkelte grenser opp i flere deler
 *          * Vi må også oppdatere metadata for grensekoblingen. Altså rekkefølge og retning med klokken
 *              * Letteste måten er å bare bygge opp flaten på nytt. Velg 1 grense som første grense, og så velg neste grense som grensen som starter på punktet forrige sluttet
 */

@Service
class DeltGeometriService(
    val kommuneService: KommuneService,
    val kodelisteService: KodelisteService,
    val deltGeometriKretsService: DeltGeometriKretsService,
    val deltGeometriAdministrativeGrenserService: DeltGeometriAdministrativeGrenserService
) {
    private val logger = LoggerFactory.getLogger(this.javaClass)

    @Transactional
    fun dannDeltGeometriForAdministrativeGrenser() {
        val alleKommuner = kommuneService.hentAlleKommuner(LocalDate.now())
        alleKommuner.mapIndexed { index, kommune ->
            logger.info("[${index+1}/${alleKommuner.size}] Danner delt geometri for ${kommune.getAdministrativenhetNavn(LocalDate.now()).getInSpraak("nor")}")
            dannDeltGeometriForKommune(kommune)
        }
    }

    fun dannDeltGeometriForKommune(kommune: Kommune, gyldighetsDato: LocalDate = LocalDate.now()) {
        val kommuneNr = kommune.kommunenummer
        val nyeAdministrativeGrenser = deltGeometriAdministrativeGrenserService.getAdministrativeGrenserMedDeltGeometriForKommune(kommune, gyldighetsDato)

        val grunnkretserSomSkalOppdateres = deltGeometriKretsService.getGrunnkretserSomGrenserTilKommunegrense(kommuneNr, gyldighetsDato)
        val stemmekretserSomSkaOppdateres = deltGeometriKretsService.getStemmekretserSomGrenserTilKommunegrense(kommuneNr, gyldighetsDato)

        val grunnkretsGrenserMap = grunnkretserSomSkalOppdateres.associateWith {
            nyeAdministrativeGrenser.map { grense ->
                GrunnkretsGrenseKobling(
                    grunnkrets = it,
                    grense = grense
                )
            }
        }

        val stemmekretsGrenserMap = stemmekretserSomSkaOppdateres.associateWith {
            nyeAdministrativeGrenser.map { grense ->
                StemmekretsGrenseKobling(
                    stemmekrets = it,
                    grense = grense
                )
            }
        }


        /**
         * TODO: Bytt ut alle grensene med grensekoblinger med de nye grensene+grensekoblingene. Siden dette skjer som en del av import går det fint
         *       om man her bare sletter alle de gamle grensene/grensekoblingene og ikke lager nye versjoner. De gamle er ikke relevante.
         *       Over har vi 2 maps (Krets -> nye grensekoblinger). Så her ligger dataene vi skal bytte ut til.
         */
    }
}
