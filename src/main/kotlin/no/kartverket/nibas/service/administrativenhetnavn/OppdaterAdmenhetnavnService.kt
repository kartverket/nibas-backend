package no.kartverket.nibas.service.administrativenhetnavn

import no.kartverket.nibas.common.getInSpraak
import no.kartverket.nibas.config.DummyAuthenticationToken
import no.kartverket.nibas.domain.AdministrativEnhet
import no.kartverket.nibas.domain.Endringstype
import no.kartverket.nibas.repository.AdministrativenhetnavnRepository
import no.kartverket.nibas.repository.FylkeRepository
import no.kartverket.nibas.repository.KommuneRepository
import no.kartverket.nibas.repository.findGyldigFylkeForFylkesnummer
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

        /**
         * Kommunenr Norsk navn Samisk navn (nominativ, dvs uten “kommune”)
         * 5516 Gratangen Rivttàk (nordsamisk)
         * 5037 Levanger Levangke (sørsamisk)
         * 1833 Rana Raane (sørsamisk)
         * 1845 Sørfold Fuolldá (lulesamisk)
         */
        val gratangen = kommuneRepository.findGyldigKommuneForKommunenummer("5516", gyldighetsdato)
            ?: throw RuntimeException("Fant ikke kommune med kommunenummer 5516")
        val levanger = kommuneRepository.findGyldigKommuneForKommunenummer("5037", gyldighetsdato)
            ?: throw RuntimeException("Fant ikke kommune med kommunenummer 5037")
        val rana = kommuneRepository.findGyldigKommuneForKommunenummer("1833", gyldighetsdato)
            ?: throw RuntimeException("Fant ikke kommune med kommunenummer 1833")
        val sorfold = kommuneRepository.findGyldigKommuneForKommunenummer("1845", gyldighetsdato)
            ?: throw RuntimeException("Fant ikke kommune med kommunenummer 1845")

        endreRekkefolgeNullPaOpprinneligNavnOgleggTilNavn(gratangen, gyldighetsdato, "Rivttàk", "sme")
        endreRekkefolgeNullPaOpprinneligNavnOgleggTilNavn(levanger, gyldighetsdato, "Levangke", "sma")
        endreRekkefolgeNullPaOpprinneligNavnOgleggTilNavn(rana, gyldighetsdato, "Raane", "sma")
        endreRekkefolgeNullPaOpprinneligNavnOgleggTilNavn(sorfold, gyldighetsdato, "Fuolldá", "smj")
    }

    //Legger til nytt navn med rekkefølge 2 og oppretter ny versjon for opprinnelig navn med rekkefølge 1
    //Opprinnelig navn med rekkefølge null arkiveres
    @Suppress("SameParameterValue")
    private fun endreRekkefolgeNullPaOpprinneligNavnOgleggTilNavn(
        enhet: AdministrativEnhet,
        gyldighetsdato: LocalDate,
        oppgittNavn: String,
        spraakForkortelse: String
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
                administrativenhetnavnService.createNyttAdmEnhetNavn(oppgittNavn = oppgittNavn, spraakForkortelse = spraakForkortelse, oppgittRekkefoelge = 2)
            )

            administrativenhetnavnService.saveNyeAdmEnhetNavnOgKnyttTilEnhet(navnList,
                gyldighetsdato, Endringstype.KVALITETSHEVING, enhet)
        }
    }

}
