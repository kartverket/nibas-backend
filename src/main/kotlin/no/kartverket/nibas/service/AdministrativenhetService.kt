package no.kartverket.nibas.service

import no.kartverket.nibas.api.v1.common.AdministrativEnhetType
import no.kartverket.nibas.api.v1.common.geojson.Feature
import no.kartverket.nibas.domain.*
import no.kartverket.nibas.kodelister.KodelisteService
import no.kartverket.nibas.repository.FylkeRepository
import no.kartverket.nibas.repository.FylkesgrenseRepository
import no.kartverket.nibas.repository.KommuneRepository
import no.kartverket.nibas.repository.KommunegrenseRepository
import no.kartverket.nibas.repository.NasjonRepository
import no.kartverket.nibas.repository.RiksgrenseRepository
import no.kartverket.nibas.repository.SpraakkodeRepository
import no.kartverket.nibas.unwrap
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import no.kartverket.nibas.api.v1.request.AdministrativEnhet as AdministrativEnhetRequest

@Service
@Transactional
class AdministrativEnhetService @Autowired constructor(
    private val kommuneRepository: KommuneRepository,
    private val fylkeRepository: FylkeRepository,
    private val nasjonRepository: NasjonRepository,
    private val kommunegrenseRepository: KommunegrenseRepository,
    private val fylkesgrenseRepository: FylkesgrenseRepository,
    private val riksgrenseRepository: RiksgrenseRepository,
    private val spraakkodeRepository: SpraakkodeRepository,
    private val kodelisteService: KodelisteService
) {

    fun hentAlleAdministrativeEnheter(type: AdministrativEnhetType): Iterable<AdministrativEnhet> {
        return when (type) {
            AdministrativEnhetType.KOMMUNE -> kommuneRepository.findAll()
            AdministrativEnhetType.FYLKE -> fylkeRepository.findAll()
            AdministrativEnhetType.NASJON -> nasjonRepository.findAll()
        }
    }

    fun hentAdministrativEnhet(id: Int, type: AdministrativEnhetType): AdministrativEnhet? {
        return when (type) {
            AdministrativEnhetType.KOMMUNE -> kommuneRepository.findById(id).unwrap()
            AdministrativEnhetType.FYLKE -> fylkeRepository.findById(id).unwrap()
            AdministrativEnhetType.NASJON -> nasjonRepository.findById(id).unwrap()
        }
    }

    fun hentAdministrativeEnheterMedId(
        ids: List<Int>,
        type: AdministrativEnhetType
    ): MutableIterable<AdministrativEnhet> {
        return when (type) {
            AdministrativEnhetType.KOMMUNE -> kommuneRepository.findAllById(ids)
            AdministrativEnhetType.FYLKE -> fylkeRepository.findAllById(ids)
            AdministrativEnhetType.NASJON -> nasjonRepository.findAllById(ids)
        }
    }

    fun oppdaterGrenser(type: AdministrativEnhetType, oppdaterteGrenseFeatures: MutableList<Feature>) {
        when (type) {
            AdministrativEnhetType.KOMMUNE -> {
                val grenseIderSomSkalOppdateres = oppdaterteGrenseFeatures.map{it.properties.id}.toSet()
                val gamleGrenser = kommunegrenseRepository.findAllById(grenseIderSomSkalOppdateres)

                val oppdaterteGrenser = gamleGrenser.map { grense ->
                    val featureForGrense = oppdaterteGrenseFeatures.find { feature -> feature.properties.id == grense.id }
                    grense.grense = featureForGrense?.geometry
                    grense
                }.toSet()

                kommunegrenseRepository.saveAll(oppdaterteGrenser)
            }
            AdministrativEnhetType.FYLKE -> {
                val grenseIderSomSkalOppdateres = oppdaterteGrenseFeatures.map{it.properties.id}.toSet()
                val gamleGrenser = fylkesgrenseRepository.findAllById(grenseIderSomSkalOppdateres)

                val oppdaterteGrenser = gamleGrenser.map { grense ->
                    val featureForGrense = oppdaterteGrenseFeatures.find { feature -> feature.properties.id == grense.id }
                    grense.grense = featureForGrense?.geometry
                    grense
                }.toSet()

                fylkesgrenseRepository.saveAll(oppdaterteGrenser)
            }
            AdministrativEnhetType.NASJON -> {
                val grenseIderSomSkalOppdateres = oppdaterteGrenseFeatures.map{it.properties.id}.toSet()
                val gamleGrenser = riksgrenseRepository.findAllById(grenseIderSomSkalOppdateres)

                val oppdaterteGrenser = gamleGrenser.map { grense ->
                    val featureForGrense = oppdaterteGrenseFeatures.find { feature -> feature.properties.id == grense.id }
                    grense.grense = featureForGrense?.geometry
                    grense
                }.toSet()
                riksgrenseRepository.saveAll(oppdaterteGrenser)
            }
        }
    }

    fun oppdaterAdministrativEnhet(
        id: Int,
        oppdatertAdministrativEnhet: AdministrativEnhetRequest
    ): AdministrativEnhet {
        val oppdatertAdmEnhetNr = oppdatertAdministrativEnhet.nummer
        when (oppdatertAdministrativEnhet.type) {
            AdministrativEnhetType.KOMMUNE -> {
                val gammelKommune = kommuneRepository.findById(id).unwrap() as Kommune
                gammelKommune.administrativenhetnavn = oppdatertAdministrativEnhet.navn.map {
                    val navn = Administrativenhetnavn()
                    navn.navn = it.navn
                    navn.spraak = spraakkodeRepository.findByIdOrNull(it.spraak)
                    navn
                }.toMutableSet()
                if (!kodelisteService.kommunenummerIdExists(oppdatertAdmEnhetNr)) {
                    throw RuntimeException("Ugyldig innsendt kommunenummerId: $oppdatertAdmEnhetNr")
                }
                gammelKommune.kommunenummer = oppdatertAdmEnhetNr

                return kommuneRepository.save(gammelKommune)
            }
            AdministrativEnhetType.FYLKE -> {
                val gammeltFylke = fylkeRepository.findById(id).unwrap() as Fylke
                gammeltFylke.administrativenhetnavn = oppdatertAdministrativEnhet.navn.map {
                    val navn = Administrativenhetnavn()
                    navn.navn = it.navn
                    navn.spraak = spraakkodeRepository.findByIdOrNull(it.spraak)
                    navn
                }.toMutableSet()
                if (!kodelisteService.fylkesnummerIdExists(oppdatertAdmEnhetNr)) {
                    throw RuntimeException("Ugyldig innsendt fylkesnummerId: $oppdatertAdmEnhetNr")
                }
                gammeltFylke.fylkesnummer = oppdatertAdmEnhetNr
                return fylkeRepository.save(gammeltFylke)
            }
            AdministrativEnhetType.NASJON -> {
                val gammelNasjon = nasjonRepository.findById(id).unwrap() as Nasjon
                gammelNasjon.administrativenhetnavn = oppdatertAdministrativEnhet.navn.map {
                    val navn = Administrativenhetnavn()
                    navn.navn = it.navn
                    navn.spraak = spraakkodeRepository.findByIdOrNull(it.spraak)
                    navn
                }.toMutableSet()
                return nasjonRepository.save(gammelNasjon)
            }
        }
    }
}


