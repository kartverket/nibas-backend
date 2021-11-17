package no.kartverket.nibas.service

import no.kartverket.nibas.api.v1.common.AdministrativEnhetType
import no.kartverket.nibas.api.v1.common.geojson.Feature
import no.kartverket.nibas.api.v1.fromAdministrativEnhetRequest
import no.kartverket.nibas.domain.*
import no.kartverket.nibas.api.v1.request.AdministrativEnhet as AdministrativEnhetRequest
import no.kartverket.nibas.repository.*
import no.kartverket.nibas.unwrap
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class AdministrativEnhetService {

    @Autowired
    lateinit var kommuneRepository: KommuneRepository

    @Autowired
    lateinit var fylkeRepository: FylkeRepository

    @Autowired
    lateinit var kommunegrenseRepository: KommunegrenseRepository

    @Autowired
    lateinit var fylkesgrenseRepository: FylkesgrenseRepository

    fun hentAlleAdministrativeEnheter(type: AdministrativEnhetType): Iterable<AdministrativEnhet> {
        return when(type) {
            AdministrativEnhetType.KOMMUNE -> kommuneRepository.findAll()
            AdministrativEnhetType.FYLKE -> fylkeRepository.findAll()
        }
    }

    fun hentAdministrativEnhet(id: Int, type: AdministrativEnhetType): AdministrativEnhet? {
        return when(type) {
            AdministrativEnhetType.KOMMUNE -> kommuneRepository.findById(id).unwrap()
            AdministrativEnhetType.FYLKE -> fylkeRepository.findById(id).unwrap()
        }
    }
    fun hentAdministrativeEnheterMedId(ids: List<Int>, type: AdministrativEnhetType): MutableIterable<AdministrativEnhet> {
        return when(type) {
            AdministrativEnhetType.KOMMUNE ->  kommuneRepository.findAllById(ids)
            AdministrativEnhetType.FYLKE -> fylkeRepository.findAllById(ids)
        }
    }
    fun oppdaterGrenser(type: AdministrativEnhetType, oppdaterteGrenseFeatures: MutableList<Feature>) {
        when(type) {
            AdministrativEnhetType.KOMMUNE -> {
                val kommunegrenser = oppdaterteGrenseFeatures.map { Kommunegrense(it.properties.id, it.properties.navn, it.geometry) }.toSet()
                kommunegrenseRepository.saveAll(kommunegrenser)
            }
            AdministrativEnhetType.FYLKE -> {
                val fylkesgrenser = oppdaterteGrenseFeatures.map { Fylkesgrense(it.properties.id, it.properties.navn, it.geometry) }.toSet()
                fylkesgrenseRepository.saveAll(fylkesgrenser)
            }
        }
    }

    fun oppdaterAdministrativEnhet(id: Int, oppdatertAdministrativEnhet: AdministrativEnhetRequest): AdministrativEnhet {
        when(oppdatertAdministrativEnhet.type) {
            AdministrativEnhetType.KOMMUNE -> {
                val kommune = fromAdministrativEnhetRequest(id, oppdatertAdministrativEnhet) as Kommune
                val gammelKommune = kommuneRepository.findById(id).unwrap() as Kommune
                val nyKommune = kommune.copy(grenser = gammelKommune.grenser)
                return kommuneRepository.save(nyKommune)
            }
            AdministrativEnhetType.FYLKE -> {
                val fylke = fromAdministrativEnhetRequest(id, oppdatertAdministrativEnhet) as Fylke
                val gammeltFylke = fylkeRepository.findById(id).unwrap() as Fylke
                val nyttFylke = fylke.copy(grenser = gammeltFylke.grenser)
                return fylkeRepository.save(nyttFylke)
            }
        }
    }
}


