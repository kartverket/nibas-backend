package no.kartverket.nibas.vedtak2025

import no.kartverket.nibas.api.v1.common.Grensetype
import no.kartverket.nibas.common.Identifikator
import no.kartverket.nibas.common.Lokalid
import no.kartverket.nibas.config.SimpleAuthenticationToken
import no.kartverket.nibas.domain.Endringstype
import no.kartverket.nibas.domain.GrunnkretsGrenseKobling
import no.kartverket.nibas.events.EventType
import no.kartverket.nibas.events.NibasEvent
import no.kartverket.nibas.events.NibasEventsService
import no.kartverket.nibas.service.FlateService
import no.kartverket.nibas.service.GrenseService
import no.kartverket.nibas.service.GrunnkretsService
import no.kartverket.nibas.service.KommuneService
import no.kartverket.nibas.service.StemmekretsService
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import java.lang.RuntimeException
import java.time.LocalDate
import jakarta.transaction.Transactional

@Service
class Vedtak2025JobService(
    private val grunnkretsService: GrunnkretsService,
    private val kommuneService: KommuneService,
    private val stemmekretsService: StemmekretsService,
    private val flateService: FlateService,
    private val grenseService: GrenseService,
    private val nibasEventsService: NibasEventsService
) {

    fun setupDummyUser(){
        SecurityContextHolder.getContext().authentication = SimpleAuthenticationToken(
            user = "vedtak_2025_risør/tvedestrand_job",
            credentials = "DUMMY",
            authorities = listOf(SimpleGrantedAuthority("NIBAS"))
        )
    }

    @Transactional
    fun doVedtakt2025Job() {
        val roysland = grunnkretsService.hentGrunnkrets(Identifikator(Lokalid("391e3537-d3ca-44aa-940d-73ff5a71951c")))
            ?: throw RuntimeException("Fant ikke Røysland")

        val endringstype = Endringstype.VEDTATT_GRENSEJUSTERING
        val gyldigFra112025 = LocalDate.of(2025,1,1)

        val lokalidRisor = "f25a6be2-d809-4eb4-806b-d2fab4c9ca09"
        val lokalidTvedestrand = "8bbe531e-9251-425a-aff7-06395d4e3e52"
        val kommuneRisor = kommuneService.hentKommune(Identifikator(Lokalid(lokalidRisor))) ?: throw RuntimeException("Fant ikke Risør kommune")
        val kommuneRisorNy = kommuneService.createNyVersjonAvKommune(kommuneRisor, gyldigFra112025, endringstype)
        val kommuneTvedestrand = kommuneService.hentKommune(Identifikator(Lokalid(lokalidTvedestrand))) ?: throw RuntimeException("Fant ikke Tvedestrand kommune")
        val kommuneTvedestrandNy = kommuneService.createNyVersjonAvKommune(kommuneTvedestrand, gyldigFra112025, endringstype)
        val kommuneVegardshei = kommuneService.hentKommune(Identifikator(Lokalid("25c2b192-c6b5-4d2c-ba4e-08ab31b4432d"))) ?: throw RuntimeException("Fant ikke Vegårdshei kommune")
        val kommuneVegardsheiNy = kommuneService.createNyVersjonAvKommune(kommuneVegardshei, gyldigFra112025, endringstype)

        val royslandNy = grunnkretsService.createNyVersjonAvGrunnkretsIAnnenKommune(roysland, kommuneTvedestrandNy,  "42130305",  gyldigFra112025, endringstype)

        val stemmekretsSogne = stemmekretsService.hentStemmekrets(Identifikator(Lokalid("01bd8ef5-670b-4f73-9932-66fb449a5f9c"))) ?: throw RuntimeException("Fant ikke Songe stemmekrets")
        val stemmekretsSogneNy = stemmekretsService.createNyVersjonAvStemmekrets(stemmekretsSogne, gyldigFra112025, endringstype)
        val stemmekretsSondeled = stemmekretsService.hentStemmekrets(Identifikator(Lokalid("865341e0-fcd1-4a69-ae10-a7c92b207240"))) ?: throw RuntimeException("Fant ikke Søndeled stemmekrets")
        val stemmekretsSondeledNy = stemmekretsService.createNyVersjonAvStemmekrets(stemmekretsSondeled, gyldigFra112025, endringstype)
        val stemmekretsMyra = stemmekretsService.hentStemmekrets(Identifikator(Lokalid("cc680e25-3bd5-4427-bbae-fb47f3d8ce88"))) ?: throw RuntimeException("Fant ikke Myra stemmekrets")
        val stemmekretsMyraNy = stemmekretsService.createNyVersjonAvStemmekrets(stemmekretsMyra, gyldigFra112025, endringstype)

        val songeGrunnkretsNy = grunnkretsService.createNyVersjonAvGrunnkrets(grunnkretsService.hentGrunnkrets(Identifikator(Lokalid("b4c67415-aba6-44ed-8581-992418319fb8"))) ?: throw RuntimeException("Fant ikke Songe grunnkrets"), gyldigFra112025, endringstype)
        val lagetGrunnkretsNy = grunnkretsService.createNyVersjonAvGrunnkrets(grunnkretsService.hentGrunnkrets(Identifikator(Lokalid("39738ff9-4254-44e8-afba-1936c88988fe"))) ?: throw RuntimeException("Fant ikke Laget grunnkrets"), gyldigFra112025, endringstype)
        val moenGrunnkretsNy = grunnkretsService.createNyVersjonAvGrunnkrets(grunnkretsService.hentGrunnkrets(Identifikator(Lokalid("57e90381-490a-4b8a-b04a-fd88542674a9"))) ?: throw RuntimeException("Fant ikke Moen grunnkrets"), gyldigFra112025, endringstype)
        val aklandGrunnkretsNy = grunnkretsService.createNyVersjonAvGrunnkrets(grunnkretsService.hentGrunnkrets(Identifikator(Lokalid("7b261e83-7ec8-4cc4-9371-51c1fa958303"))) ?: throw RuntimeException("Fant ikke Akland grunnkrets"), gyldigFra112025, endringstype)
        val naerestadGrunnkretsNy = grunnkretsService.createNyVersjonAvGrunnkrets(grunnkretsService.hentGrunnkrets(Identifikator(Lokalid("38a294ef-2740-4623-b756-bb692d5b658a"))) ?: throw RuntimeException("Fant ikke Nærestad grunnkrets"), gyldigFra112025, endringstype)

        // Disse skal bli kommunegrenser
        val grunnkretsgrenserLokalid = listOf(
            "e61ab1f7-51da-3b74-91b2-2754c9c49f50",
            "7ed8bb2b-5453-3df5-b44b-f796abe4b975"
        )

        // Disse skal bli grunnkretsgrenser
        val kommunegrenserLokalid = listOf(
            "c06046e1-60e1-36a4-98c3-34ef39fbebc7",
            "dc4fba85-b2b1-313f-be42-7d86d31be2bf",
            "18dc1ccc-9d03-32df-9f92-78b2fab55ba8",
            "7998f539-4931-3685-9217-2916a5234066",
            "1cb89497-66fd-356b-86ef-0850a2f7f9a1",
            "14421203-fee7-3759-a037-310c2650a60d",
            "0ea161cc-6e33-35ea-a8e7-7ba30437b0b3",
            "1320de79-e90a-38fb-b9b4-03d31538f556",
            "d6e9f6ba-9d7c-3344-8d08-03362804d8e5",
            "33b1282e-3e53-3725-96a1-a09aaa1c9203"
        )

        val eksisterendeKommunegrenserSomKunSkalBytteTilhorighetLokalid = listOf(
            "7f20c1da-76ff-30c9-8fb7-73ee8ef63297",
            "4441c45f-00a6-3952-8249-270b74826a28",
            "345c44f7-3a05-3eb2-a751-17c0d1cba457",
            "3a74e3e9-4c13-3184-b06b-9685435c0d1b",
            "5293d1b8-bfb2-3fe7-b64e-977fb875d48b"
        )

        val nyeGrenser = (grunnkretsgrenserLokalid+kommunegrenserLokalid+eksisterendeKommunegrenserSomKunSkalBytteTilhorighetLokalid).toList().map {
            grenseService.createNyVersjonAvGrense(grenseService.hentGrense(Identifikator(Lokalid(it))) ?: throw RuntimeException("Fant ikke grense med lokalid $it"), gyldigFra112025, endringstype)
        }

        listOf(songeGrunnkretsNy, lagetGrunnkretsNy, moenGrunnkretsNy, aklandGrunnkretsNy, royslandNy, naerestadGrunnkretsNy).forEach {
            nyKrets ->
                nyKrets.grenser = (nyKrets.grenser.map { gkgk ->
                    if(grunnkretsgrenserLokalid.contains(gkgk.grense.lokalid)){
                        val nyGrense = nyeGrenser.find { it.lokalid == gkgk.grense.lokalid } ?: throw RuntimeException("Fant ikke grense med lokalid ${gkgk.grense.lokalid}")

                        nyGrense.grensetype = Grensetype.Kommunegrense

                        GrunnkretsGrenseKobling(nyKrets, nyGrense)
                    }
                    else if (kommunegrenserLokalid.contains(gkgk.grense.lokalid)){
                        val nyGrense = nyeGrenser.find { it.lokalid == gkgk.grense.lokalid } ?: throw RuntimeException("Fant ikke grense med lokalid ${gkgk.grense.lokalid}")

                        nyGrense.grensetype = Grensetype.Grunnkretsgrense
                        nyGrense.stemmekretser = mutableSetOf()

                        GrunnkretsGrenseKobling(nyKrets, nyGrense)
                    }
                    else if (eksisterendeKommunegrenserSomKunSkalBytteTilhorighetLokalid.contains(gkgk.grense.lokalid)){
                        val nyGrense = nyeGrenser.find { it.lokalid == gkgk.grense.lokalid } ?: throw RuntimeException("Fant ikke grense med lokalid ${gkgk.grense.lokalid}")

                        nyGrense.grunnkretser = mutableSetOf(
                            GrunnkretsGrenseKobling(naerestadGrunnkretsNy, nyGrense),
                            GrunnkretsGrenseKobling(royslandNy, nyGrense)
                        )

                        GrunnkretsGrenseKobling(nyKrets, nyGrense)
                    }
                    else gkgk
                }).toMutableSet()
        }

        nyeGrenser.filter { grunnkretsgrenserLokalid.contains(it.lokalid) }.forEach {
            stemmekretsSogneNy.addGrensekobling(it)
            stemmekretsSondeledNy.addGrensekobling(it)
        }
        nyeGrenser.filter { kommunegrenserLokalid.contains(it.lokalid) }.forEach {
            stemmekretsSogneNy.removeGrensekobling(it)
            stemmekretsSondeledNy.removeGrensekobling(it)
        }
        nyeGrenser.filter { eksisterendeKommunegrenserSomKunSkalBytteTilhorighetLokalid.contains(it.lokalid) }.forEach {
            stemmekretsSogneNy.addGrensekobling(it)
            stemmekretsMyraNy.addGrensekobling(it)
            stemmekretsSondeled.removeGrensekobling(it)
        }

        val endredeFlater = setOf(
            kommuneRisorNy, kommuneTvedestrandNy, kommuneVegardsheiNy,
            royslandNy, songeGrunnkretsNy, lagetGrunnkretsNy, aklandGrunnkretsNy, naerestadGrunnkretsNy,
            stemmekretsSogneNy, stemmekretsSondeledNy, stemmekretsMyraNy
        )

        flateService.oppdaterFlaterFor(endredeFlater)

        val events = endredeFlater.map { flate ->
            flate.toEventRad(EventType.MODIFIED)
        }.toSet()

        nibasEventsService.publishEvent(NibasEvent(gyldigFra112025, events))
    }
}
