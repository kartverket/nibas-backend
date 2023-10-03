package no.kartverket.nibas.service

import no.kartverket.nibas.domain.AdministrativEnhet
import no.kartverket.nibas.domain.Administrativenhetnavn
import no.kartverket.nibas.domain.Endringstype
import no.kartverket.nibas.repository.AdministrativenhetnavnRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class AdministrativenhetnavnService @Autowired constructor(
    private val administrativenhetnavnRepository: AdministrativenhetnavnRepository
) {

    fun createNyeAdmEnhetNavnOgKnyttTilEnhet(admenhetnavnList: List<Administrativenhetnavn>, gyldigFra: LocalDate, endringstype: Endringstype, admEnhet: AdministrativEnhet) {
        admenhetnavnList.forEach { admenhetNavn ->
            var nyttNavn = Administrativenhetnavn()
                .apply {
                    navn = admenhetNavn.navn
                    spraak = admenhetNavn.spraak
                    rekkefoelge = admenhetNavn.rekkefoelge
                    setGyldigFra(gyldigFra)
                    auditInfo.typeEndring = endringstype
                }
            nyttNavn = administrativenhetnavnRepository.save(nyttNavn)

            admEnhet.addAdministrativenhetNavn(nyttNavn)
        }
    }

}
