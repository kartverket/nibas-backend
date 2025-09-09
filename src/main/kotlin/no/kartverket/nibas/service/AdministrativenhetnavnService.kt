package no.kartverket.nibas.service

import no.kartverket.nibas.domain.AdministrativEnhet
import no.kartverket.nibas.domain.Administrativenhetnavn
import no.kartverket.nibas.domain.Endringstype
import no.kartverket.nibas.repository.AdministrativenhetnavnRepository
import no.kartverket.nibas.repository.SpraakkodeRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class AdministrativenhetnavnService @Autowired constructor(
    private val administrativenhetnavnRepository: AdministrativenhetnavnRepository,
    private val spraakkodeRepository: SpraakkodeRepository
) {

    fun saveNyeAdmEnhetNavnOgKnyttTilEnhet(
        admenhetnavnList: List<Administrativenhetnavn>,
        gyldigFra: LocalDate,
        endringstype: Endringstype,
        admEnhet: AdministrativEnhet
    ) {
        admenhetnavnList.forEach { admenhetNavn ->
            admenhetNavn.apply {
                setGyldigFra(gyldigFra)
                auditInfo.typeEndring = endringstype
            }
            val nyttNavn = administrativenhetnavnRepository.save(admenhetNavn)

            admEnhet.addAdministrativenhetNavn(nyttNavn)
        }
    }

    fun createNyVersjonAvAdministrativenhetnavn(gammeltAdmEnhetNavn: Administrativenhetnavn): Administrativenhetnavn {
        return Administrativenhetnavn(lokalid = gammeltAdmEnhetNavn.lokalid)
            .applyMappingFromExistingAdministrativenhetnavn(gammeltAdmEnhetNavn)
            .apply {
                version = gammeltAdmEnhetNavn.version + 1
            }
    }

    fun createNyttAdmEnhetNavn(oppgittNavn: String?, spraakForkortelse: String?, oppgittRekkefoelge: Int? = null): Administrativenhetnavn {
        return Administrativenhetnavn()
            .apply {
                navn = oppgittNavn
                spraak = spraakForkortelse?.let { spraakkodeRepository.findByIdOrNull(it) }
                rekkefoelge = oppgittRekkefoelge
            }
    }

}
