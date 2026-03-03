package no.kartverket.nibas.service.administrativenhetnavn

import no.kartverket.nibas.common.getInSpraak
import no.kartverket.nibas.config.DummyAuthenticationToken
import no.kartverket.nibas.domain.AdministrativEnhet
import no.kartverket.nibas.domain.Endringstype
import no.kartverket.nibas.repository.AdministrativenhetnavnRepository
import no.kartverket.nibas.repository.FylkeRepository
import no.kartverket.nibas.repository.KommuneRepository
import no.kartverket.nibas.repository.findGyldigKommuneForKommunenummer
import no.kartverket.nibas.service.AdministrativenhetnavnService
import org.slf4j.LoggerFactory
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Service
class OppdaterAdmenhetnavnService(
    val fylkeRepository: FylkeRepository,
    val kommuneRepository: KommuneRepository,
    val administrativenhetnavnRepository: AdministrativenhetnavnRepository,
    val administrativenhetnavnService: AdministrativenhetnavnService
) {
    private val logger = LoggerFactory.getLogger(this.javaClass)

    //Språkkoder:
    //fkv = Kvensk-finsk
    //nor = Norsk (både bokmål og nynorsk)
    //sma = Sørsamisk
    //sme = Nordsamisk
    //smj = Lulesamisk

    @Transactional
    fun oppdaterAdministrativenhetnavn() {
        SecurityContextHolder.getContext().authentication = DummyAuthenticationToken(
            user = "vedtakAdmenhetNavn",
            credentials = "DUMMY",
            authorities = listOf(SimpleGrantedAuthority("NIBAS"))
        )
        val gyldighetsdato = LocalDate.now()

        val oslo = kommuneRepository.findGyldigKommuneForKommunenummer("0301", gyldighetsdato)
            ?: throw RuntimeException("Fant ikke kommune med kommunenummer 0301")
        val lyngen = kommuneRepository.findGyldigKommuneForKommunenummer("5536", gyldighetsdato)
            ?: throw RuntimeException("Fant ikke kommune med kommunenummer 5536")
        val steinkjer = kommuneRepository.findGyldigKommuneForKommunenummer("5006", gyldighetsdato)
            ?: throw RuntimeException("Fant ikke kommune med kommunenummer 5006")


        val nyeLyngenNavn = listOf(
            Pair("Yykeä", "fkv"), Pair("Ivgu", "sme")
        )
        val nyeOsloNavn = listOf(
            Pair("Oslove", "sma"), Pair("Oslo", "smj")
        )
        val nyeSteinkjerNavn = listOf(
            Pair("Stïentje", "sma")
        )

        leggTilNyeAdminenhetNavnForEnhetMedEttNavn(lyngen, gyldighetsdato, nyeLyngenNavn)
        leggTilNyeAdminenhetNavnForEnhetMedEttNavn(oslo, gyldighetsdato, nyeOsloNavn)
        leggTilNyeAdminenhetNavnForEnhetMedEttNavn(steinkjer, gyldighetsdato, nyeSteinkjerNavn)
    }

    //Opprinnelig navn med rekkefølge null arkiveres
    //Oppretter ny versjon for opprinnelig navn med rekkefølge 1
    //Nye navn legges til i stigende rekkefølge
    @Suppress("SameParameterValue")
    private fun leggTilNyeAdminenhetNavnForEnhetMedEttNavn(
        enhet: AdministrativEnhet,
        gyldighetsdato: LocalDate,
        nyeNavn: List<Pair<String, String>>,
    ) {
        val enhetSineNavn = enhet.getAdministrativenhetNavn(gyldighetsdato)
        //Gjør ikke noe med enheten hvis den har flere enn ett navn. Da er patchen allerede kjørt
        if (enhetSineNavn.size == 1) {
            logger.info("Oppdaterer navn for " + enhetSineNavn.getInSpraak("nor"))

            //Det opprinnelige norske navnet hadde null i rekkefølge, må arkiveres og få ny versjon med rekkefølge 1
            val opprinneligNorskNavn = enhetSineNavn[0]
            opprinneligNorskNavn.setGyldigTil(gyldighetsdato)
            administrativenhetnavnRepository.save(opprinneligNorskNavn)

            val navnList = listOf(
                administrativenhetnavnService.createNyVersjonAvAdministrativenhetnavn(opprinneligNorskNavn).apply { rekkefoelge = 1 },
                *nyeNavn.mapIndexed { index, pair ->
                    administrativenhetnavnService.createNyttAdmEnhetNavn(oppgittNavn = pair.first, spraakForkortelse = pair.second, oppgittRekkefoelge = index + 2)
                }.toTypedArray()
            )

            administrativenhetnavnService.saveNyeAdmEnhetNavnOgKnyttTilEnhet(navnList,
                gyldighetsdato, Endringstype.KVALITETSHEVING, enhet)
        }
    }

}
