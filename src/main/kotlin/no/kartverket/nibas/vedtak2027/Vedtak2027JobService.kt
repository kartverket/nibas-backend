package no.kartverket.nibas.vedtak2027

import no.kartverket.nibas.api.v1.common.Grensetype
import no.kartverket.nibas.domain.Endringstype
import no.kartverket.nibas.domain.Flate
import no.kartverket.nibas.domain.Grense
import no.kartverket.nibas.domain.Grunnkrets
import no.kartverket.nibas.domain.Kommune
import no.kartverket.nibas.domain.Stemmekrets
import no.kartverket.nibas.events.EventType
import no.kartverket.nibas.events.NibasEventPublisher
import no.kartverket.nibas.service.FlateService
import no.kartverket.nibas.service.GrenseService
import no.kartverket.nibas.service.GrunnkretsService
import no.kartverket.nibas.service.KommuneService
import no.kartverket.nibas.service.StemmekretsService
import no.kartverket.nibas.service.lagMuligeStemmekretsnumre
import no.kartverket.nibas.sporing.Gyldighet
import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.geom.MultiPolygon
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import kotlin.math.abs

/**
 * Engangsjobb for vedtakene som gjelder fra 1. januar 2027: flytter en hel grunnkrets, og
 * stemmekretsen som dekker (nesten) samme område, til en annen kommune i samme fylke.
 *
 * ## Datamodellen
 *
 * - En [Grense] er én linje med én [Grensetype]. Kretsene bygger flatene sine av grensene de er
 *   koblet til.
 * - Grunnkretser og stemmekretser deler bare administrative grenser (kommune, fylke, kyst osv.).
 *   Inne i en kommune har de egne linjer, også der linjene ligger oppå hverandre: grunnkrets- eller
 *   delområdegrenser og stemmekretsgrenser. Se [Grunnkrets.addGrensekobling] og
 *   [Stemmekrets.addGrensekobling].
 * - Kommunen har ingen grenser selv. Flaten bygges av grunnkretsenes administrative grenser, se
 *   [Kommune.getGrensekoblinger].
 * - En endring lager en ny versjon med samme lokalid og gyldigFra = virkningsdato. Den gamle får
 *   gyldigTil = virkningsdato og beholder koblingene sine, så historikken viser det som gjaldt før.
 *
 * ## Hva som skjer med linjene rundt kretsen
 *
 * Mot **frakommunen** er linjen i dag grunnkrets- eller delområdegrense, og stemmekretsen har sin
 * egen stemmekretsgrense ved siden av. Linjen blir kommunegrense:
 * - grunnkretsgrensen får ny versjon med grensetype Kommunegrense, og begge stemmekretsene kobles til den
 * - stemmekretsens egen stemmekretsgrense avsluttes, ellers ville den ligget dobbelt
 *
 * Mot **tilkommunen** er linjen i dag kommunegrense, delt av grunnkretser og stemmekretser. Linjen
 * havner inne i kommunen:
 * - kommunegrensen får ny versjon med grensetype Grunnkretsgrense, eller Delområdegrense hvis det
 *   nye grunnkretsnummeret og naboen har ulikt delområde (siffer 5–6)
 * - stemmekretsene kan ikke kobles til en grunnkretsgrense, så de får en kopi av linjen med ny
 *   lokalid og grensetype Stemmekretsgrense. Jobben antar at stemmekretsen blir egen krets i
 *   tilkommunen.
 *
 * ## Steg
 *
 * 1. [lesOgKontrollerGrunnlag]: finner og kontrollerer alt. Ingenting lagres.
 * 2. Nye versjoner av kommunene og kretsene. Må skje før steg 3, fordi de nye versjonene kopierer
 *    koblingene til de gamle grensene. Til slutt kontrolleres det at de nye kretsnumrene ikke
 *    brukes av andre kretser på eller etter virkningsdatoen.
 * 3. Nye versjoner av grensene, og nye koblinger.
 * 4. Flatene dannes på nytt.
 * 5. [kontrollerResultat]: kontrollerer arealene.
 *
 * Endringene i databasen skjer i én transaksjon, så feiler noe, lagres ingenting der. Som det siste
 * før commit sendes de endrede flatene til nibas-events, og det kallet rulles ikke tilbake: feiler
 * commit etter at nibas-events har tatt imot hendelsen, ligger hendelsen der uten endringene. Alle
 * kontrollene ([Vedtak2027GrunnlagException]) kjører før hendelsen sendes. Med profilen
 * `eventsDisabled` (med i `localhost`, og i testene) sender [NibasEventPublisher] ingenting, så
 * jobben kan kjøres for fullt mot en lokal kopi av produksjon.
 */
@Service
class Vedtak2027JobService(
    private val kommuneService: KommuneService,
    private val grunnkretsService: GrunnkretsService,
    private val stemmekretsService: StemmekretsService,
    private val grenseService: GrenseService,
    private val flateService: FlateService,
    private val nibasEventPublisher: NibasEventPublisher,
) {
    @Transactional
    fun flyttGrunnkretsOgStemmekrets(plan: Vedtak2027Plan): Vedtak2027Rapport {
        val dato = plan.virkningsdato
        val endringstype = Endringstype.VEDTATT_GRENSEJUSTERING
        val rapport = Vedtak2027Rapport()

        // Steg 1: finn og kontroller alt før noe endres
        val grunnlag = lesOgKontrollerGrunnlag(plan)
        rapport.logg("Grunnlaget er kontrollert: ${grunnlag.beskrivelse()}")
        rapport.logg("Objektene jobben endrer:\n" + grunnlag.endringer(plan).joinToString("\n") { "  $it" })

        // Steg 2a: kommunene får nye versjoner, slik at de gamle kommuneflatene blir stående i historikken
        val fraKommuneNy = kommuneService.createNyVersjonAvKommune(grunnlag.fraKommune, dato, endringstype)
        val tilKommuneNy = kommuneService.createNyVersjonAvKommune(grunnlag.tilKommune, dato, endringstype)

        // Steg 2b: stemmekretsene
        val stemmekretsNy = stemmekretsService
            .createNyVersjonAvStemmekretsIAnnenKommune(grunnlag.stemmekrets, tilKommuneNy, dato, endringstype)
            .apply {
                stemmekretsnummer = plan.nyttStemmekretsnummer
                stemmekretsnavn = plan.nyttStemmekretsnavn
            }
        val naboStemmekretsIFraKommuneNy = stemmekretsService
            .createNyVersjonAvStemmekrets(grunnlag.naboStemmekretsIFraKommune, dato, endringstype)
        val naboStemmekretserITilKommuneNy = grunnlag.naboStemmekretserITilKommune.values
            .distinctBy { it.lokalid }
            .associate { it.lokalid to stemmekretsService.createNyVersjonAvStemmekrets(it, dato, endringstype) }

        // Steg 2c: grunnkretsene. Lokalid, navn og grenser beholdes, nummer og kommune endres.
        val grunnkretsNy = grunnkretsService.createNyVersjonAvGrunnkretsIAnnenKommune(
            grunnlag.grunnkrets, tilKommuneNy, plan.nyttGrunnkretsnummer, dato, endringstype
        )
        val nabogrunnkretserNy = grunnlag.grenser
            .map { it.nabo }
            .distinctBy { it.lokalid }
            .associate { it.lokalid to grunnkretsService.createNyVersjonAvGrunnkrets(it, dato, endringstype) }

        // Steg 2d: samme kontroll av unike kretsnumre som når et utkast publiseres. Finner også kretser
        // med samme nummer som starter etter virkningsdatoen.
        krev(!grunnkretsService.finnesAnnenGrunnkretsMedSammeGrunnkretsnummer(grunnkretsNy)) {
            "Grunnkretsnummer ${plan.nyttGrunnkretsnummer} er brukt av en annen grunnkrets som gjelder fra $dato eller senere"
        }
        krev(!stemmekretsService.finnesAnnenStemmekretsMedSammeKommuneOgStemmekretsnummer(stemmekretsNy)) {
            "Stemmekretsnummer ${plan.nyttStemmekretsnummer} er brukt av en annen stemmekrets i ${plan.tilKommunenummer} " +
                "som gjelder fra $dato eller senere"
        }

        // Steg 3a: hver linje rundt grunnkretsen får ny versjon med ny grensetype
        for ((gammelGrense, nabo, naboErIFraKommunen, nyGrensetype) in grunnlag.grenser) {
            val naboNy = nabogrunnkretserNy.getValue(nabo.lokalid)

            if (naboErIFraKommunen) {
                val nyKommunegrense = lagNyVersjonMedGrensetype(gammelGrense, nyGrensetype, dato, endringstype)
                grunnkretsNy.replaceGrensekobling(gammelGrense, nyKommunegrense)
                naboNy.replaceGrensekobling(gammelGrense, nyKommunegrense)
                // Stemmekretsene hadde egne linjer her (avsluttes i steg 3b). Nå er dette kommunegrense,
                // og den deles av grunnkretser og stemmekretser.
                stemmekretsNy.addGrensekobling(nyKommunegrense)
                naboStemmekretsIFraKommuneNy.addGrensekobling(nyKommunegrense)
            } else {
                // Kopien må lages før den nye versjonen. createNyVersjonAvGrense setter gyldigTil på den
                // gamle grensen, og kopien arver gyldigTil. Da ville kopien fått gyldigFra = gyldigTil.
                val stemmekretsgrense = grenseService
                    .createNyGrenseKopiertFraAnnenGrenseMedNyLokalid(gammelGrense, gammelGrense.grense, dato, endringstype)
                    .apply { grensetype = Grensetype.Stemmekretsgrense }
                val nyIntern = lagNyVersjonMedGrensetype(gammelGrense, nyGrensetype, dato, endringstype)

                grunnkretsNy.replaceGrensekobling(gammelGrense, nyIntern)
                naboNy.replaceGrensekobling(gammelGrense, nyIntern)
                val naboStemmekrets = grunnlag.naboStemmekretserITilKommune.getValue(gammelGrense.id)
                stemmekretsNy.replaceGrensekobling(gammelGrense, stemmekretsgrense)
                naboStemmekretserITilKommuneNy.getValue(naboStemmekrets.lokalid).replaceGrensekobling(gammelGrense, stemmekretsgrense)
            }
        }

        // Steg 3b: stemmekretsgrensene mot frakommunen erstattes av kommunegrensene fra steg 3a
        for (gammelStemmekretsgrense in grunnlag.stemmekretsgrenserMotFraKommunen) {
            stemmekretsNy.removeGrensekobling(gammelStemmekretsgrense)
            naboStemmekretsIFraKommuneNy.removeGrensekobling(gammelStemmekretsgrense)
            gammelStemmekretsgrense.setGyldigTil(dato)
        }

        // Steg 4: flatene dannes på nytt fra grensene. Feiler med UgyldigeFlaterException hvis en flate
        // ikke henger sammen.
        val endredeFlater: Set<Flate> = setOf<Flate>(fraKommuneNy, tilKommuneNy, grunnkretsNy, stemmekretsNy, naboStemmekretsIFraKommuneNy) +
            nabogrunnkretserNy.values + naboStemmekretserITilKommuneNy.values
        flateService.oppdaterFlaterFor(endredeFlater)

        // Steg 5: kontroller at resultatet ble som forventet
        kontrollerResultat(
            plan = plan,
            foer = grunnlag,
            fraKommuneNy = fraKommuneNy,
            tilKommuneNy = tilKommuneNy,
            grunnkretsNy = grunnkretsNy,
            stemmekretsNy = stemmekretsNy,
            nabogrunnkretserNy = nabogrunnkretserNy,
            naboStemmekretsIFraKommuneNy = naboStemmekretsIFraKommuneNy,
            naboStemmekretserITilKommuneNy = naboStemmekretserITilKommuneNy,
            rapport = rapport,
        )

        rapport.endredeFlater = endredeFlater.map { it.toEventRad(EventType.MODIFIED) }
        nibasEventPublisher.publishNibasEvent(rapport.endredeFlater, dato)
        rapport.logg("${endredeFlater.size} flater er endret og sendt til nibas-events med virkningsdato $dato (ikke med profilen eventsDisabled).")
        return rapport
    }

    /** Ny versjon av grensen med [nyGrensetype]. [GrenseService.createNyVersjonAvGrense] beholder ellers den gamle grensetypen. */
    private fun lagNyVersjonMedGrensetype(gammelGrense: Grense, nyGrensetype: Grensetype, dato: LocalDate, endringstype: Endringstype): Grense =
        grenseService.createNyVersjonAvGrense(gammelGrense, dato, endringstype).apply { grensetype = nyGrensetype }

    /**
     * Finner alt som skal endres og stopper hvis noe avviker fra planen.
     */
    private fun lesOgKontrollerGrunnlag(plan: Vedtak2027Plan): Grunnlag {
        val dato = plan.virkningsdato

        val fraKommune = kommuneService.hentKommuneByKommunenummer(plan.fraKommunenummer, dato)
            ?: stopp("Fant ikke kommune ${plan.fraKommunenummer} gyldig på $dato")
        val tilKommune = kommuneService.hentKommuneByKommunenummer(plan.tilKommunenummer, dato)
            ?: stopp("Fant ikke kommune ${plan.tilKommunenummer} gyldig på $dato")
        krev(fraKommune.getFylke(dato)?.lokalid == tilKommune.getFylke(dato)?.lokalid) {
            "Kommunene ${fraKommune.kommunenummer} og ${tilKommune.kommunenummer} ligger i ulike fylker. " +
                "Jobben håndterer ikke fylkesgrenser."
        }
        val bopliktomraader = fraKommune.getBopliktomraade(dato) + tilKommune.getBopliktomraade(dato)
        krev(bopliktomraader.isEmpty()) {
            "Kommunene har ${bopliktomraader.size} bopliktområder. Jobben håndterer ikke boplikt. Avklar hvordan " +
                "boplikten skal følge flyttingen før jobben kjøres."
        }

        val grunnkrets = grunnkretsService.hentGrunnkretsForGrunnkretsNummer(plan.grunnkretsnummer, dato)
            ?: stopp("Fant ingen grunnkrets ${plan.grunnkretsnummer} gyldig på $dato. Er flyttingen allerede kjørt?")
        krev(grunnkrets.getKommune(dato)?.lokalid == fraKommune.lokalid) {
            "Grunnkrets ${grunnkrets.grunnkretsnummer} ligger ikke i kommune ${fraKommune.kommunenummer}"
        }
        krev(grunnkrets.grunnkretsnavn == plan.kretsnavn) {
            "Grunnkrets ${grunnkrets.grunnkretsnummer} heter ${grunnkrets.grunnkretsnavn}, forventet ${plan.kretsnavn}"
        }
        kontrollerNyttGrunnkretsnummer(plan, tilKommune)

        // Grensene rundt grunnkretsen, og hvem som ligger på andre siden av hver av dem
        val grunnkretsensGrenser = grunnkrets.getGrensekoblinger(dato).map { it.grense }
        val grenser = grunnkretsensGrenser.map { grense ->
            klassifiserGrense(grense, grunnkrets, fraKommune, tilKommune, plan)
        }

        val stemmekrets = fraKommune.getStemmekretser(dato)
            .filter { it.stemmekretsnummer in lagMuligeStemmekretsnumre(plan.stemmekretsnummer) }
            .singleOrNull()
            ?: stopp("Fant ikke én stemmekrets ${plan.stemmekretsnummer} i kommune ${fraKommune.kommunenummer} gyldig på $dato")
        krev(stemmekrets.stemmekretsnavn == plan.kretsnavn) {
            "Stemmekrets ${stemmekrets.stemmekretsnummer} heter ${stemmekrets.stemmekretsnavn}, forventet ${plan.kretsnavn}"
        }
        krev(plan.nyttStemmekretsnummer.toIntOrNull() != null) {
            "Nytt stemmekretsnummer ${plan.nyttStemmekretsnummer} er ikke et tall"
        }

        val grunnkretsensGrenseIder = grunnkretsensGrenser.map { it.id }.toSet()
        val stemmekretsensGrenser = stemmekrets.getGrensekoblinger(dato).map { it.grense }
        val delteGrenser = stemmekretsensGrenser.filter { it.id in grunnkretsensGrenseIder }
        val stemmekretsgrenserMotFraKommunen = stemmekretsensGrenser.filter { it.id !in grunnkretsensGrenseIder }

        val grenserMotTilKommunen = grenser.filterNot { it.naboErIFraKommunen }.map { it.grense.id }.toSet()
        krev(delteGrenser.map { it.id }.toSet() == grenserMotTilKommunen) {
            "Stemmekrets ${stemmekrets.stemmekretsnummer} deler ikke nøyaktig kommunegrensene mot ${tilKommune.kommunenummer} " +
                "med grunnkrets ${grunnkrets.grunnkretsnummer}. Jobben forutsetter at de dekker samme område."
        }

        val naboStemmekretserITilKommune = delteGrenser.associate { grense ->
            val andre = grense.stemmekretser(dato).filter { it.lokalid != stemmekrets.lokalid }
            krev(andre.size == 1 && andre.single().getKommune(dato)?.lokalid == tilKommune.lokalid) {
                "Grense ${grense.lokalid} skal ha nøyaktig én annen stemmekrets, i ${tilKommune.kommunenummer}. " +
                    "Fant ${andre.map { it.stemmekretsnummer }}"
            }
            grense.id to andre.single()
        }

        stemmekretsgrenserMotFraKommunen.forEach { grense ->
            krev(grense.grensetype == Grensetype.Stemmekretsgrense) {
                "Grense ${grense.lokalid} rundt stemmekretsen er ${grense.grensetype}, forventet Stemmekretsgrense"
            }
            krev(grense.stemmekretser(dato).size == 2) {
                "Stemmekretsgrense ${grense.lokalid} er koblet til ${grense.stemmekretser(dato).size} stemmekretser, forventet 2"
            }
        }
        val naboStemmekretserIFraKommune = stemmekretsgrenserMotFraKommunen
            .flatMap { it.stemmekretser(dato) }
            .filter { it.lokalid != stemmekrets.lokalid }
            .distinctBy { it.lokalid }
        krev(naboStemmekretserIFraKommune.size == 1 && naboStemmekretserIFraKommune.single().getKommune(dato)?.lokalid == fraKommune.lokalid) {
            "Stemmekrets ${stemmekrets.stemmekretsnummer} skal grense mot nøyaktig én stemmekrets i ${fraKommune.kommunenummer}. " +
                "Fant ${naboStemmekretserIFraKommune.map { it.stemmekretsnummer }}"
        }

        val avvik = areal(symmetriskDifferanse(stemmekrets.omraade, grunnkrets.omraade))

        val grunnlag = Grunnlag(
            fraKommune = fraKommune,
            tilKommune = tilKommune,
            grunnkrets = grunnkrets,
            grenser = grenser,
            stemmekrets = stemmekrets,
            naboStemmekretsIFraKommune = naboStemmekretserIFraKommune.single(),
            naboStemmekretserITilKommune = naboStemmekretserITilKommune,
            stemmekretsgrenserMotFraKommunen = stemmekretsgrenserMotFraKommunen,
            avvikStemmekretsMotGrunnkretsM2 = avvik,
        )
        kontrollerIngenSenereVersjoner(grunnlag, dato)
        return grunnlag
    }

    /**
     * Bestemmer hva en grense rundt grunnkretsen skal bli. Hver grense skal ha nøyaktig én nabokrets
     * i frakommunen eller tilkommunen. En grense uten nabo (kyst, ytre grense) eller mot en tredje
     * kommune ville trengt andre regler, så da stopper jobben.
     */
    private fun klassifiserGrense(grense: Grense, grunnkrets: Grunnkrets, fraKommune: Kommune, tilKommune: Kommune, plan: Vedtak2027Plan): GrenseMedNabo {
        val dato = plan.virkningsdato
        val naboer = grense.grunnkretser(dato).filter { it.lokalid != grunnkrets.lokalid }
        krev(naboer.size == 1) {
            "Grense ${grense.lokalid} har ${naboer.size} nabogrunnkretser. Jobben håndterer bare grenser med nøyaktig én nabo."
        }
        val nabo = naboer.single()

        return when (nabo.getKommune(dato)?.lokalid) {
            fraKommune.lokalid -> {
                krev(grense.grensetype in setOf(Grensetype.Grunnkretsgrense, Grensetype.Delomraadegrense)) {
                    "Grense ${grense.lokalid} mot ${nabo.grunnkretsnummer} er ${grense.grensetype}, forventet grunnkrets- eller delområdegrense"
                }
                GrenseMedNabo(grense, nabo, naboErIFraKommunen = true, nyGrensetype = Grensetype.Kommunegrense)
            }

            tilKommune.lokalid -> {
                krev(grense.grensetype == Grensetype.Kommunegrense) {
                    "Grense ${grense.lokalid} mot ${nabo.grunnkretsnummer} er ${grense.grensetype}, forventet Kommunegrense"
                }
                // Samme regel som klienten bruker i UtkastValidering: samme delområde gir grunnkretsgrense
                val nyGrensetype = if (delomraade(plan.nyttGrunnkretsnummer) == delomraade(nabo.grunnkretsnummer!!)) {
                    Grensetype.Grunnkretsgrense
                } else {
                    Grensetype.Delomraadegrense
                }
                GrenseMedNabo(grense, nabo, naboErIFraKommunen = false, nyGrensetype = nyGrensetype)
            }

            else -> stopp("Grense ${grense.lokalid} grenser mot ${nabo.grunnkretsnummer} i en tredje kommune. Jobben håndterer ikke det.")
        }
    }

    /** Alle objektene jobben endrer, gruppert og sortert så listen blir lik fra kjøring til kjøring */
    private fun Grunnlag.endringer(plan: Vedtak2027Plan): List<String> = buildList {
        add("Kommune: ${fraKommune.kommunenummer} (${fraKommune.lokalid})")
        add("Kommune: ${tilKommune.kommunenummer} (${tilKommune.lokalid})")
        add("Grunnkrets: ${grunnkrets.grunnkretsnummer} ${grunnkrets.grunnkretsnavn} -> ${plan.nyttGrunnkretsnummer} (${grunnkrets.lokalid})")
        addAll(grenser.map { it.nabo }.distinctBy { it.lokalid }
            .map { "Nabogrunnkrets: ${it.grunnkretsnummer} ${it.grunnkretsnavn} (${it.lokalid})" }.sorted())
        add("Stemmekrets: ${fraKommune.kommunenummer} ${stemmekrets.stemmekretsnummer} ${stemmekrets.stemmekretsnavn} -> " +
            "${tilKommune.kommunenummer} ${plan.nyttStemmekretsnummer} ${plan.nyttStemmekretsnavn} (${stemmekrets.lokalid})")
        add("Nabostemmekrets: ${fraKommune.kommunenummer} ${naboStemmekretsIFraKommune.stemmekretsnummer} " +
            "${naboStemmekretsIFraKommune.stemmekretsnavn} (${naboStemmekretsIFraKommune.lokalid})")
        addAll(naboStemmekretserITilKommune.values.distinctBy { it.lokalid }
            .map { "Nabostemmekrets: ${tilKommune.kommunenummer} ${it.stemmekretsnummer} ${it.stemmekretsnavn} (${it.lokalid})" }.sorted())
        addAll(grenser.map { "Grense: ${it.grense.grensetype} -> ${it.nyGrensetype} mot ${it.nabo.grunnkretsnummer} (${it.grense.lokalid})" }.sorted())
        addAll(stemmekretsgrenserMotFraKommunen.map {
            "Stemmekretsgrense avsluttes: mot ${naboStemmekretsIFraKommune.stemmekretsnummer} (${it.lokalid})"
        }.sorted())
    }

    private fun kontrollerNyttGrunnkretsnummer(plan: Vedtak2027Plan, tilKommune: Kommune) {
        val nyttNummer = plan.nyttGrunnkretsnummer
        krev(Regex("\\d{8}").matches(nyttNummer)) {
            "Nytt grunnkretsnummer $nyttNummer har ikke åtte sifre"
        }
        krev(nyttNummer.startsWith(tilKommune.kommunenummer)) {
            "Nytt grunnkretsnummer $nyttNummer starter ikke med kommunenummeret ${tilKommune.kommunenummer}"
        }
    }

    /**
     * Alle objektene som får ny versjon, må være siste versjon og ha startet før virkningsdatoen.
     * Ellers ville jobben kuttet av en senere planlagt versjon, eller laget en versjon med
     * gyldigFra = gyldigTil, som behandles som slettet.
     */
    private fun kontrollerIngenSenereVersjoner(grunnlag: Grunnlag, dato: LocalDate) {
        val objekter: List<Pair<String, Gyldighet>> =
            listOf(grunnlag.fraKommune, grunnlag.tilKommune).map { "Kommune ${it.kommunenummer}" to it.gyldighet } +
                (listOf(grunnlag.grunnkrets) + grunnlag.grenser.map { it.nabo }).map { "Grunnkrets ${it.grunnkretsnummer}" to it.gyldighet } +
                (listOf(grunnlag.stemmekrets, grunnlag.naboStemmekretsIFraKommune) + grunnlag.naboStemmekretserITilKommune.values)
                    .map { "Stemmekrets ${it.stemmekretsnummer} (${it.lokalid})" to it.gyldighet } +
                (grunnlag.grenser.map { it.grense } + grunnlag.stemmekretsgrenserMotFraKommunen).map { "Grense ${it.lokalid}" to it.gyldighet }
        objekter.forEach { (hva, gyldighet) ->
            krev(gyldighet.gyldigTil == null && gyldighet.gyldigFra.isBefore(dato)) {
                "$hva har gyldighet ${gyldighet.gyldigFra}–${gyldighet.gyldigTil}. " +
                    "Jobben krever at den er siste versjon og startet før $dato."
            }
        }
    }

    /**
     * Kontrollerer arealene etter flyttingen. Linjene er de samme før og etter, så alt annet enn
     * avrunding betyr at en kobling er feil:
     * - grunnkretsen har samme område som før, og stemmekretsen dekker nøyaktig grunnkretsen
     * - frakommunen har mistet grunnkretsens areal, og tilkommunen har fått det
     * - nabogrunnkretsene og nabostemmekretsene i tilkommunen er uendret
     * - nabostemmekretsen i frakommunen og stemmekretsen har til sammen samme areal som før. Hver for
     *   seg kan de endre seg med avviket mellom stemmekrets og grunnkrets, se [lesOgKontrollerGrunnlag].
     */
    private fun kontrollerResultat(
        plan: Vedtak2027Plan,
        foer: Grunnlag,
        fraKommuneNy: Kommune,
        tilKommuneNy: Kommune,
        grunnkretsNy: Grunnkrets,
        stemmekretsNy: Stemmekrets,
        nabogrunnkretserNy: Map<String, Grunnkrets>,
        naboStemmekretsIFraKommuneNy: Stemmekrets,
        naboStemmekretserITilKommuneNy: Map<String, Stemmekrets>,
        rapport: Vedtak2027Rapport,
    ) {
        val toleranse = plan.arealToleranseM2
        fun kontrollerAreal(hva: String, faktisk: Double, forventet: Double) {
            krev(abs(faktisk - forventet) <= toleranse) { "$hva har areal $faktisk m², forventet $forventet m² (± $toleranse)" }
        }

        val grunnkretsAreal = areal(foer.grunnkrets.omraade)
        krev(areal(symmetriskDifferanse(grunnkretsNy.omraade, foer.grunnkrets.omraade)) <= toleranse) {
            "Grunnkrets ${grunnkretsNy.grunnkretsnummer} har fått et annet område enn før flyttingen"
        }
        krev(areal(symmetriskDifferanse(stemmekretsNy.omraade, grunnkretsNy.omraade)) <= toleranse) {
            "Stemmekrets ${stemmekretsNy.stemmekretsnummer} dekker ikke nøyaktig grunnkrets ${grunnkretsNy.grunnkretsnummer}"
        }
        kontrollerAreal("Kommune ${fraKommuneNy.kommunenummer}", areal(fraKommuneNy.omraade), areal(foer.fraKommune.omraade) - grunnkretsAreal)
        kontrollerAreal("Kommune ${tilKommuneNy.kommunenummer}", areal(tilKommuneNy.omraade), areal(foer.tilKommune.omraade) + grunnkretsAreal)

        foer.grenser.map { it.nabo }.distinctBy { it.lokalid }.forEach { gammel ->
            kontrollerAreal("Grunnkrets ${gammel.grunnkretsnummer}", areal(nabogrunnkretserNy.getValue(gammel.lokalid).omraade), areal(gammel.omraade))
        }
        foer.naboStemmekretserITilKommune.values.distinctBy { it.lokalid }.forEach { gammel ->
            kontrollerAreal("Stemmekrets ${gammel.stemmekretsnummer} i ${tilKommuneNy.kommunenummer}",
                areal(naboStemmekretserITilKommuneNy.getValue(gammel.lokalid).omraade), areal(gammel.omraade))
        }

        val naboFoer = areal(foer.naboStemmekretsIFraKommune.omraade)
        val naboEtter = areal(naboStemmekretsIFraKommuneNy.omraade)
        kontrollerAreal("Stemmekrets ${naboStemmekretsIFraKommuneNy.stemmekretsnummer} og ${stemmekretsNy.stemmekretsnummer} til sammen",
            naboEtter + areal(stemmekretsNy.omraade), naboFoer + areal(foer.stemmekrets.omraade))

        rapport.logg("Arealkontrollene er ok. Grunnkretsen er ${"%.0f".format(grunnkretsAreal)} m². " +
            "Nabostemmekretsen i frakommunen endret areal med ${"%.1f".format(naboEtter - naboFoer)} m².")
    }

    private fun delomraade(grunnkretsnummer: String) = grunnkretsnummer.substring(4, 6)

    private fun areal(omraade: Geometry?): Double = omraade?.area ?: stopp("Mangler område for en flate")

    /** Området som ligger i bare én av flatene. Tomt når de dekker nøyaktig det samme. */
    private fun symmetriskDifferanse(a: MultiPolygon?, b: MultiPolygon?): Geometry =
        (a ?: stopp("Mangler område for en flate")).symDifference(b ?: stopp("Mangler område for en flate"))

    private fun stopp(melding: String): Nothing = throw Vedtak2027GrunnlagException(melding)

    private inline fun krev(betingelse: Boolean, melding: () -> String) {
        if (!betingelse) stopp(melding())
    }

    private data class GrenseMedNabo(
        val grense: Grense,
        val nabo: Grunnkrets,
        val naboErIFraKommunen: Boolean,
        val nyGrensetype: Grensetype,
    )

    private class Grunnlag(
        val fraKommune: Kommune,
        val tilKommune: Kommune,
        val grunnkrets: Grunnkrets,
        val grenser: List<GrenseMedNabo>,
        val stemmekrets: Stemmekrets,
        val naboStemmekretsIFraKommune: Stemmekrets,
        /** Nabostemmekretsen på andre siden av hver kommunegrense mot tilkommunen, med grensens id som map key */
        val naboStemmekretserITilKommune: Map<String, Stemmekrets>,
        val stemmekretsgrenserMotFraKommunen: List<Grense>,
        val avvikStemmekretsMotGrunnkretsM2: Double,
    ) {
        fun beskrivelse() =
            "grunnkrets ${grunnkrets.grunnkretsnummer} ${grunnkrets.grunnkretsnavn} har ${grenser.size} grenser " +
                "(${grenser.count { it.naboErIFraKommunen }} mot ${fraKommune.kommunenummer}, " +
                "${grenser.count { !it.naboErIFraKommunen }} mot ${tilKommune.kommunenummer}) og " +
                "${grenser.map { it.nabo.lokalid }.distinct().size} nabogrunnkretser. Stemmekrets ${stemmekrets.stemmekretsnummer} " +
                "har ${stemmekretsgrenserMotFraKommunen.size} stemmekretsgrense(r) mot ${fraKommune.kommunenummer} og avviker " +
                "${"%.1f".format(avvikStemmekretsMotGrunnkretsM2)} m² fra grunnkretsen."
    }
}

/** Grunnlaget i databasen stemmer ikke med planen, eller resultatet ble ikke som forventet. Ingenting er lagret. */
class Vedtak2027GrunnlagException(melding: String) : IllegalStateException(melding)
