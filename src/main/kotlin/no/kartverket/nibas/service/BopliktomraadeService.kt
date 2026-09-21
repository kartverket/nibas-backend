package no.kartverket.nibas.service

import no.kartverket.nibas.api.v1.common.Flatetype
import no.kartverket.nibas.api.v1.common.ObjektIdentifikator
import no.kartverket.nibas.api.v1.request.BopliktomraadeRequest
import no.kartverket.nibas.api.v1.request.BopliktomraadeRequestFields
import no.kartverket.nibas.api.v1.request.CreateBopliktomraadeRequest
import no.kartverket.nibas.common.Identifikator
import no.kartverket.nibas.common.Lokalid
import no.kartverket.nibas.common.ResponseException
import no.kartverket.nibas.common.UgyldigTilstandException
import no.kartverket.nibas.domain.Bopliktomraade
import no.kartverket.nibas.domain.BopliktomraadeGrenser
import no.kartverket.nibas.domain.BopliktomraadeGrenseKobling
import no.kartverket.nibas.domain.Endringstype
import no.kartverket.nibas.domain.UnikLokalid
import no.kartverket.nibas.repository.BopliktomraadeRepository
import no.kartverket.nibas.repository.KommuneRepository
import no.kartverket.nibas.repository.findAllByLokalidSortedByGyldigFra
import no.kartverket.nibas.repository.findAllGyldigPaaDato
import no.kartverket.nibas.repository.findAntallFramtidigeVersjonerForIdentifikator
import no.kartverket.nibas.repository.findGyldigObjektForIdentifikator
import no.kartverket.nibas.repository.findGyldigObjektPaaGyldighetsdato
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.MultiPolygon
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Service
@Transactional(readOnly = true)
class BopliktomraadeService(
    private val bopliktomraadeRepository: BopliktomraadeRepository,
    private val kommuneRepository: KommuneRepository
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

    @Transactional(readOnly = false)
    fun arkiverBopliktomraade(lokalId: Lokalid, gyldigTil: LocalDate): Bopliktomraade {
        val identifikator = Identifikator(lokalId, gyldigTil)
        val bopliktomraade = hentBopliktomraade(identifikator)
            ?: throw ResponseException.NotFoundException(
                title = "Bopliktområde ikke funnet",
                msg = "Bopliktområde med $identifikator ble ikke funnet."
            )

        val egneGrenser = when (val grenser = bopliktomraade.bopliktomraadeGrenser) {
            is BopliktomraadeGrenser.DelvisBoplikt -> grenser.getGrensekoblinger(gyldigTil).map { it.grense }
            is BopliktomraadeGrenser.FullBoplikt -> emptyList()
        }

        bopliktomraade.setGyldigTil(gyldigTil)
        egneGrenser.forEach { it.setGyldigTil(gyldigTil) }
        return bopliktomraade
    }

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
            if(bopliktomraade.gjelderKunDelAvKommunen) it.copyGrenserFromExistingBopliktomraade(bopliktomraade, gyldigFra) else it
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

    fun createNyttBopliktomraade(request: CreateBopliktomraadeRequest, gyldigFra: LocalDate, endringstype: Endringstype): Bopliktomraade {
        val lokalidForKommune = request.kommuneIdentifikasjon.lokalid
        val kommune = kommuneRepository.findAllByLokalid(lokalidForKommune).maxBy { it.gyldighet.gyldigFra }

        val geometryFactory = GeometryFactory()
        return bopliktomraadeRepository.save(Bopliktomraade().applyChangesFromRequest(request)
            .apply {
                kommuneLokalid = kommune.kommuneLokalid
                setGyldigFra(gyldigFra)
                auditInfo.typeEndring = endringstype
                omraade = if (request.gjelderKunDelAvKommunen) { MultiPolygon(arrayOfNulls(0), geometryFactory) } else { null }
                posisjon = if (request.gjelderKunDelAvKommunen) { geometryFactory.createPoint() } else {
                   geometryFactory.createPoint(kommune.posisjon?.coordinate)
                }
            }
        )
    }

    @Transactional(readOnly = true)
    fun hentBopliktomraadeForNummerIKommune(kommuneIdentifikator: ObjektIdentifikator, nummer: String, gyldighetsdato: LocalDate): Bopliktomraade? {
        return findGyldigObjektPaaGyldighetsdato(bopliktomraadeRepository.findAllByNummerAndKommuneLokalid(nummer, UnikLokalid(kommuneIdentifikator.lokalid.value, Flatetype.KOMMUNE)), gyldighetsdato)
    }
}

fun Bopliktomraade.applyChangesFromRequest(request: BopliktomraadeRequestFields): Bopliktomraade {
    return this.apply {
        this.navn = request.navn
        this.nummer = request.nummer
        this.harUsikkerAvgrensning = request.harUsikkerAvgrensning
        this.forskriftsreferanse = request.forskriftsreferanse
        val gjeldendeMaterielleVilkaar = request.gjeldendeMaterielleVilkaar
        this.setGjeldendeMaterielleVilkaar(gjeldendeMaterielleVilkaar)
        this.andreLokaleAvgrensninger = request.andreLokaleAvgrensninger
        this.url = request.url
        if (gjelderKunDelAvKommunen != request.gjelderKunDelAvKommunen) {
            this.bopliktomraadeGrenser =
                if (request.gjelderKunDelAvKommunen) BopliktomraadeGrenser.DelvisBoplikt()
                else BopliktomraadeGrenser.FullBoplikt()
        }
    }
}

