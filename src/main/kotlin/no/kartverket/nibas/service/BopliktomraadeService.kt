package no.kartverket.nibas.service

import no.kartverket.nibas.api.v1.request.BopliktomraadeRequest
import no.kartverket.nibas.common.Identifikator
import no.kartverket.nibas.common.Lokalid
import no.kartverket.nibas.common.ResponseException
import no.kartverket.nibas.common.UgyldigTilstandException
import no.kartverket.nibas.domain.Bopliktomraade
import no.kartverket.nibas.domain.BopliktomraadeGrenser
import no.kartverket.nibas.domain.BopliktomraadeGrenseKobling
import no.kartverket.nibas.domain.Endringstype
import no.kartverket.nibas.repository.BopliktomraadeRepository
import no.kartverket.nibas.repository.findAllByLokalidSortedByGyldigFra
import no.kartverket.nibas.repository.findAllGyldigPaaDato
import no.kartverket.nibas.repository.findAntallFramtidigeVersjonerForIdentifikator
import no.kartverket.nibas.repository.findGyldigObjektForIdentifikator
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Service
@Transactional(readOnly = true)
class BopliktomraadeService(
    private val bopliktomraadeRepository: BopliktomraadeRepository
) {
    fun hentAlleBopliktomraader(dato: LocalDate): List<Bopliktomraade> =
        bopliktomraadeRepository.findAllGyldigPaaDato(dato)

    fun hentBopliktomraade(identifikator: Identifikator): Bopliktomraade? =
        bopliktomraadeRepository.findGyldigObjektForIdentifikator(identifikator)

    fun hentGrensekoblingerForBopliktomraade(identifikator: Identifikator): List<BopliktomraadeGrenseKobling> {
        return hentBopliktomraade(identifikator)
            ?.getGrensekoblinger(identifikator.gyldighetsdato)?.toList()
            ?: emptyList()
    }

    fun hentAlleVersjonerMedGyldigFraEtterGittDato(lokalId: Lokalid, dato: LocalDate): List<Bopliktomraade> =
        bopliktomraadeRepository.findAllByLokalidSortedByGyldigFra(lokalId)
            .filter { it.gyldighet.gyldigFra.isAfter(dato) }

    fun hentAntallGyldigeVersjonerFramITidFor(identifikator: Identifikator): Int =
        bopliktomraadeRepository.findAntallFramtidigeVersjonerForIdentifikator(identifikator)

    fun hentFramtidigeVersjonerForBopliktomraade(lokalId: Lokalid, gyldighetsdato: LocalDate?): List<Bopliktomraade> {
        val identifikator = Identifikator(lokalId, gyldighetsdato ?: LocalDate.now())
        val gyldigBopliktomraadeForDagensDato = hentBopliktomraade(identifikator)
            ?: throw UgyldigTilstandException("Det finnes ingen gyldig bopliktomraade for identifikator $identifikator")
        return listOf(gyldigBopliktomraadeForDagensDato)
            .plus(hentAlleVersjonerMedGyldigFraEtterGittDato(lokalId, identifikator.gyldighetsdato))
    }

    @Transactional(readOnly = false)
    fun oppdaterBopliktomraade(
        lokalId: Lokalid,
        gyldigFra: LocalDate,
        endringstype: Endringstype,
        oppdatertBopliktomraade: BopliktomraadeRequest
    ): Bopliktomraade {

        val identifikator = Identifikator(lokalId, gyldigFra)
        val gammeltBopliktomraade = hentBopliktomraade(identifikator)
            ?: throw ResponseException.NotFoundException(title = "Bopliktområde ikke funnet", msg = "Bopliktområde med $identifikator ble ikke funnet.")

        val nyttBopliktomraade = createNyVersjonAvBopliktomraade(gammeltBopliktomraade, gyldigFra, endringstype)
            .applyChangesFromRequest(oppdatertBopliktomraade)

        return nyttBopliktomraade
    }

    @Transactional(readOnly = false)
    fun createNyVersjonAvBopliktomraade(bopliktomraade: Bopliktomraade, gyldigFra: LocalDate, endringstype: Endringstype): Bopliktomraade {
        return createNyVersjonAvBopliktomraadeUtenGrenser(bopliktomraade, gyldigFra, endringstype).let{
            if(bopliktomraade.delvisBoplikt) it.copyGrenserFromExistingBopliktomraade(bopliktomraade, gyldigFra) else it
        }
    }

    @Transactional(readOnly = false)
    fun createNyVersjonAvBopliktomraadeUtenGrenser(bopliktomraade: Bopliktomraade, gyldigFra: LocalDate, endringstype: Endringstype): Bopliktomraade {
        val nyttBopliktomraade = kopierOgSaveBopliktomraade(bopliktomraade, gyldigFra, endringstype)

        val kommune = bopliktomraade.getKommune(gyldigFra) ?: throw ResponseException.NotFoundException(title = "Kommune ikke funnet",
            msg = "Fant ikke kommune for bopliktområde med id ${bopliktomraade.id} og gyldig fra $gyldigFra")
        kommune.addBopliktomraade(nyttBopliktomraade)
        return nyttBopliktomraade
    }

    private fun kopierOgSaveBopliktomraade(bopliktomraade: Bopliktomraade, gyldigFra: LocalDate, endringstype: Endringstype): Bopliktomraade {
        val nyttBopliktomraade = bopliktomraadeRepository.save(
            Bopliktomraade(lokalid = bopliktomraade.lokalid)
                .applyMappingFromExistingBopliktomraade(bopliktomraade)
                .apply {
                    setGyldigFra(gyldigFra)
                    bopliktomraade.gyldighet.gyldigTil?.let { setGyldigTil(it) }
                    version = nesteVersjonForBopliktomraade(Lokalid(bopliktomraade.lokalid))
                    auditInfo.typeEndring = endringstype
                }
        )

        return nyttBopliktomraade
            .also { bopliktomraade.setGyldigTil(gyldigFra) }
    }

    private fun nesteVersjonForBopliktomraade(lokalId: Lokalid): Int =
        bopliktomraadeRepository
            .findAllByLokalid(lokalId.value)
            .maxBy { it.version }
            .version + 1

}

fun Bopliktomraade.applyChangesFromRequest(request: BopliktomraadeRequest): Bopliktomraade {
    return this.apply {
        this.navn = request.navn
        this.nummer = request.nummer
        val usikkerAvgrensningValue = request.harUsikkerAvgrensning ?: request.usikkerAvgrensning
        this.usikkerAvgrensning = usikkerAvgrensningValue
        this.harUsikkerAvgrensning = usikkerAvgrensningValue
        this.forskriftsreferanse = request.forskriftsreferanse
        val gjeldendeMaterielleVilkaar = request.gjeldendeMaterielleVilkaar
        if (gjeldendeMaterielleVilkaar != null) {
            this.setGjeldendeMaterielleVilkaar(gjeldendeMaterielleVilkaar)
        } else {
            this.setMaterielleVilkaar(request.materielleVilkaar)
        }
        val andreAvgrensningerValue = request.andreLokaleAvgrensninger ?: request.andreAvgrensninger
        this.andreAvgrensninger = andreAvgrensningerValue
        this.andreLokaleAvgrensninger = andreAvgrensningerValue
        this.url = request.url
        informasjon = request.informasjon
        if (delvisBoplikt != request.delvisBoplikt) {
            this.bopliktomraadeGrenser =
                if (request.delvisBoplikt) BopliktomraadeGrenser.DelvisBoplikt()
                else BopliktomraadeGrenser.FullBoplikt()
        }
    }
}

