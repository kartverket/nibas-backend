package no.kartverket.nibas.service

import no.kartverket.nibas.api.v1.common.AdministrativEnhetType
import no.kartverket.nibas.api.v1.common.geojson.Feature
import no.kartverket.nibas.domain.*
import no.kartverket.nibas.api.v1.request.AdministrativEnhet as AdministrativEnhetRequest
import no.kartverket.nibas.repository.*
import no.kartverket.nibas.unwrap
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.repository.findByIdOrNull
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
    lateinit var nasjonRepository: NasjonRepository

    @Autowired
    lateinit var kommunegrenseRepository: KommunegrenseRepository

    @Autowired
    lateinit var fylkesgrenseRepository: FylkesgrenseRepository

    @Autowired
    lateinit var riksgrenseRepository: RiksgrenseRepository

    @Autowired
    lateinit var spraakkodeRepository: SpraakkodeRepository

    @Autowired
    lateinit var kommunenummerRepository: KommunenummerRepository

    @Autowired
    lateinit var fylkesnummerRepository: FylkesnummerRepository

    fun hentAlleAdministrativeEnheter(type: AdministrativEnhetType): Iterable<AdministrativEnhet> {
        return when(type) {
            AdministrativEnhetType.KOMMUNE -> kommuneRepository.findAll()
            AdministrativEnhetType.FYLKE -> fylkeRepository.findAll()
            AdministrativEnhetType.NASJON -> nasjonRepository.findAll()
        }
    }

    fun hentAdministrativEnhet(id: Int, type: AdministrativEnhetType): AdministrativEnhet? {
        return when(type) {
            AdministrativEnhetType.KOMMUNE -> kommuneRepository.findById(id).unwrap()
            AdministrativEnhetType.FYLKE -> fylkeRepository.findById(id).unwrap()
            AdministrativEnhetType.NASJON -> nasjonRepository.findById(id).unwrap()
        }
    }
    fun hentAdministrativeEnheterMedId(ids: List<Int>, type: AdministrativEnhetType): MutableIterable<AdministrativEnhet> {
        return when(type) {
            AdministrativEnhetType.KOMMUNE ->  kommuneRepository.findAllById(ids)
            AdministrativEnhetType.FYLKE -> fylkeRepository.findAllById(ids)
            AdministrativEnhetType.NASJON -> nasjonRepository.findAllById(ids)
        }
    }
    fun oppdaterGrenser(type: AdministrativEnhetType, oppdaterteGrenseFeatures: MutableList<Feature>) {
        when(type) {
            AdministrativEnhetType.KOMMUNE -> {
                val kommunegrenser = oppdaterteGrenseFeatures.map { feature ->
                    val eksisterendeGrense = kommunegrenseRepository.findById(feature.properties.id).unwrap()
                    eksisterendeGrense?.grense = feature.geometry
                    eksisterendeGrense
                }.toSet()
                kommunegrenseRepository.saveAll(kommunegrenser)
            }
            AdministrativEnhetType.FYLKE -> {
                val fylkesgrenser = oppdaterteGrenseFeatures.map { feature ->
                    val eksisterendeGrense = fylkesgrenseRepository.findById(feature.properties.id).unwrap()
                    eksisterendeGrense?.grense = feature.geometry
                    eksisterendeGrense
                }.toSet()
                fylkesgrenseRepository.saveAll(fylkesgrenser)
            }
            AdministrativEnhetType.NASJON -> {
                val riksgrenser = oppdaterteGrenseFeatures.map { feature ->
                    val eksisterendeGrense = riksgrenseRepository.findById(feature.properties.id).unwrap()
                    eksisterendeGrense?.grense = feature.geometry
                    eksisterendeGrense
                }.toSet()
                riksgrenseRepository.saveAll(riksgrenser)
            }
        }
    }

    fun oppdaterAdministrativEnhet(id: Int, oppdatertAdministrativEnhet: AdministrativEnhetRequest): AdministrativEnhet {
        return when(oppdatertAdministrativEnhet.type) {
            AdministrativEnhetType.KOMMUNE -> {
                val gammelKommune = kommuneRepository.findById(id).unwrap() as Kommune
                gammelKommune.administrativenhetnavn = oppdatertAdministrativEnhet.navn.map { it ->
                    val navn = Administrativenhetnavn()
                    navn.navn = it.navn
                    navn.spraak = spraakkodeRepository.findByIdOrNull(it.spraak)
                    navn
                }.toMutableSet()
                gammelKommune.kommunenummer = kommunenummerRepository.findByIdOrNull(oppdatertAdministrativEnhet.nummer)
                return kommuneRepository.save(gammelKommune)
            }
            AdministrativEnhetType.FYLKE -> {
                val gammeltFylke = fylkeRepository.findById(id).unwrap() as Fylke
                gammeltFylke.administrativenhetnavn = oppdatertAdministrativEnhet.navn.map { it ->
                    val navn = Administrativenhetnavn()
                    navn.navn = it.navn
                    navn.spraak = spraakkodeRepository.findByIdOrNull(it.spraak)
                    navn
                }.toMutableSet()
                gammeltFylke.fylkesnummer = fylkesnummerRepository.findByIdOrNull(oppdatertAdministrativEnhet.nummer)
                return fylkeRepository.save(gammeltFylke)
            }
            AdministrativEnhetType.NASJON -> nasjonRepository.findById(id).unwrap() as Nasjon
        }
    }
}


