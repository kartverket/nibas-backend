package no.kartverket.nibas.service.administrativenhetnavn

import no.kartverket.nibas.config.SimpleAuthenticationToken
import no.kartverket.nibas.domain.Endringstype
import no.kartverket.nibas.repository.AdministrativenhetnavnRepository
import no.kartverket.nibas.repository.FylkeRepository
import no.kartverket.nibas.repository.findGyldigFylkeForFylkesnummer
import no.kartverket.nibas.service.AdministrativenhetnavnService
import org.slf4j.LoggerFactory
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Service
class OppdaterFylkesnavnService(
    val fylkeRepository: FylkeRepository,
    val administrativenhetnavnRepository: AdministrativenhetnavnRepository,
    val administrativenhetnavnService: AdministrativenhetnavnService
) {
    private val logger = LoggerFactory.getLogger(this.javaClass)

    @Transactional
    fun samiskeNavnForInnlandetOgNordland() {
        SecurityContextHolder.getContext().authentication = SimpleAuthenticationToken(
            user = "oppdaterFylkesnavn",
            credentials = "DUMMY",
            authorities = listOf(SimpleGrantedAuthority("NIBAS"))
        )
        val gyldighetsdato = LocalDate.now()

        val innlandetFylke = fylkeRepository.findGyldigFylkeForFylkesnummer("34")
            ?: throw RuntimeException("Fant ikke Innlandet fylke med fylkesnummer 34")

        val nordlandFylke = fylkeRepository.findGyldigFylkeForFylkesnummer("18")
            ?: throw RuntimeException("Fant ikke Nordland fylke med fylkesnummer 18")

        //Gjør ikke noe med Innlandet hvis det har flere enn ett navn
        val innlandetSineNavn = innlandetFylke.getAdministrativenhetNavn(gyldighetsdato)
        if (innlandetSineNavn.size == 1) {
            logger.info("Oppdaterer fylkesnavn for Innlandet")

            //Det opprinnelige norske navnet på innlandet hadde null i rekkefølge, må arkiveres og få ny versjon med rekkefølge 1
            val innlandetOpprinneligNorskNavn = innlandetSineNavn[0]
            innlandetOpprinneligNorskNavn.setGyldigTil(gyldighetsdato)
            administrativenhetnavnRepository.save(innlandetOpprinneligNorskNavn)

            val innlandetNavnList = listOf(
                administrativenhetnavnService.createNyVersjonAvAdministrativenhetnavn(innlandetOpprinneligNorskNavn).apply { rekkefoelge = 1 },
                administrativenhetnavnService.createNyttAdmEnhetNavn(oppgittNavn = "Sisdajve", spraakForkortelse = "sma", oppgittRekkefoelge = 2)
            )

            administrativenhetnavnService.saveNyeAdmEnhetNavnOgKnyttTilEnhet(innlandetNavnList,
                gyldighetsdato, Endringstype.KVALITETSHEVING, innlandetFylke)
        }

        val nordlandSineNavn = nordlandFylke.getAdministrativenhetNavn(gyldighetsdato)
        if (nordlandSineNavn.size == 2) {
            logger.info("Oppdaterer fylkesnavn for Nordland")
            val nordlandNavnList = listOf(
                administrativenhetnavnService.createNyttAdmEnhetNavn(oppgittNavn = "Nordlánda", spraakForkortelse = "sme", oppgittRekkefoelge = 3),
                administrativenhetnavnService.createNyttAdmEnhetNavn(oppgittNavn = "Nordlaante", spraakForkortelse = "sma", oppgittRekkefoelge = 4)
            )

            administrativenhetnavnService.saveNyeAdmEnhetNavnOgKnyttTilEnhet(nordlandNavnList,
                gyldighetsdato, Endringstype.KVALITETSHEVING, nordlandFylke)
        }

    }

}
