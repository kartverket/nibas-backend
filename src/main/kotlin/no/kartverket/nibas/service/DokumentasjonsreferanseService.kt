package no.kartverket.nibas.service

import no.kartverket.nibas.api.v1.common.geojson.metadata.DokumentasjonsreferanseDTO
import no.kartverket.nibas.common.ResponseException.BadRequestException
import no.kartverket.nibas.domain.DokrefGrenseKobling
import no.kartverket.nibas.domain.Dokumentasjonsreferanse
import no.kartverket.nibas.domain.Endringstype
import no.kartverket.nibas.domain.Grense
import no.kartverket.nibas.repository.DokrefGrenseKoblingRepository
import no.kartverket.nibas.repository.DokumentasjonsreferanseRepository
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class DokumentasjonsreferanseService(private val dokumentasjonsreferanseRepository: DokumentasjonsreferanseRepository, private val grenseKoblingRepository: DokrefGrenseKoblingRepository) {
    /*
    * Create new versions of dokumentasjonsreferanser if changes are detected.
    * Returns any new DokrefGrenseKoblinger
    * */
    fun updateDokumentasjonsreferanser(
        dokumentasjonsreferanseDTOer: List<DokumentasjonsreferanseDTO>,
        grense: Grense,
        gyldighetsdato: LocalDate = LocalDate.now(),
        endringstype: Endringstype
    ): Set<DokrefGrenseKobling> {
        val dtosToUpdate = findDokumentasjonsreferanserWithChanges(dokumentasjonsreferanseDTOer, grense.getAllDokumentasjonsreferanser())
        val dtosToArchive = dtosToUpdate.filter { dto -> dto.shouldArchive }

        val allExistingDokrefsOnGrense = grense.dokrefGrensekobling.map { it.dokumentasjonsreferanse }.toSet()
        val updatedDokrefs = dtosToUpdate.map { dokref -> createNyVersjonAvDokumentasjonsreferanse(dokref, allExistingDokrefsOnGrense, endringstype, gyldighetsdato) }.toSet()
        val mappedOldAndNewDokrefs = mapOldVersionToNewVersion(updatedDokrefs, allExistingDokrefsOnGrense)

        updatedDokrefs.forEach { dokref -> dokref.setGyldigTilIfArchived(dtosToArchive, gyldighetsdato) }
        setGyldighetOnDokrefs(mappedOldAndNewDokrefs, gyldighetsdato)
        applyCascadingOperationsOnDokrefs(mappedOldAndNewDokrefs)

        val grensekoblinger = createGrensekoblinger(updatedDokrefs, grense)

        dokumentasjonsreferanseRepository.saveAll(allExistingDokrefsOnGrense)
        dokumentasjonsreferanseRepository.saveAllAndFlush(updatedDokrefs)
        grenseKoblingRepository.saveAllAndFlush(grensekoblinger)

        return grensekoblinger
    }

    private fun applyCascadingOperationsOnDokrefs(mappedOldAndNewDokrefs: Set<Pair<Dokumentasjonsreferanse, Dokumentasjonsreferanse>>) =
        mappedOldAndNewDokrefs.forEach {
            it.second.applyCascadingOperationsBasedOnExistingDokumentasjonsreferanse(it.first)
        }


    /*
    * Returns a set of dokumentasjonsreferanser mapped by old version to new version
    * */
    private fun mapOldVersionToNewVersion(updatedDokrefs: Set<Dokumentasjonsreferanse>, existingDokrefs: Set<Dokumentasjonsreferanse>): Set<Pair<Dokumentasjonsreferanse, Dokumentasjonsreferanse>> =
        updatedDokrefs.mapNotNull { newVersion ->
            val oldVersion = existingDokrefs.firstOrNull { oldVersion -> newVersion.lokalid == oldVersion.lokalid }

            if (oldVersion != null)
                Pair(oldVersion, newVersion)
            else null
        }.toSet()

    private fun setGyldighetOnDokrefs(
        mappedOldAndNewDokrefs: Set<Pair<Dokumentasjonsreferanse, Dokumentasjonsreferanse>>,
        gyldighetsdato: LocalDate
    ) = mappedOldAndNewDokrefs.forEach {
        it.first.setGyldigTil(gyldighetsdato)
        it.second.setGyldigFra(gyldighetsdato)
    }

    private fun createGrensekoblinger(updatedDokrefs: Set<Dokumentasjonsreferanse>, grense: Grense): Set<DokrefGrenseKobling> =
        updatedDokrefs.map { dokref ->
            DokrefGrenseKobling().apply {
                dokumentasjonsreferanse = dokref
                this.grense = grense
                dokref.dokrefGrenseKobling.add(this)
            }
        }.toSet()

    private fun findDokumentasjonsreferanserWithChanges(dokumentasjonsreferanseDTOer: List<DokumentasjonsreferanseDTO>, dokumentasjonsreferanser: Set<Dokumentasjonsreferanse>) = dokumentasjonsreferanseDTOer.asSequence().filter { dto ->
        dto.id == null || dokumentasjonsreferanser.any { existingDokref -> existingDokref.id == dto.id && existingDokref.hasChanges(dto) }
    }.toSet()

    private fun createNyVersjonAvDokumentasjonsreferanse(dokumentasjonsreferanseDTO: DokumentasjonsreferanseDTO, dokumentasjonsreferanser: Set<Dokumentasjonsreferanse>, endringstype: Endringstype, gyldighetsdato: LocalDate): Dokumentasjonsreferanse {
        val dokref = dokumentasjonsreferanseDTO.id?.let { id ->
            val lokalid = dokumentasjonsreferanser.firstOrNull { existingDokref -> existingDokref.id == id }?.lokalid
                ?: throw BadRequestException("Dokumentasjonsreferanse-id eksisterer ikke: $id")

            Dokumentasjonsreferanse(lokalid = lokalid)
        } ?: Dokumentasjonsreferanse().apply {
            setGyldigFra(gyldighetsdato)
        }

        return dokref.apply {
            auditInfo.typeEndring = endringstype
            setFieldsFromDto(dokumentasjonsreferanseDTO)
        }
    }

    private fun Dokumentasjonsreferanse.setGyldigTilIfArchived(shouldArchiveDTOs: List<DokumentasjonsreferanseDTO>, gyldighetsdato: LocalDate) {
        val shouldArchive = shouldArchiveDTOs.firstOrNull { it.id == this.id }?.shouldArchive ?: false
        if (shouldArchive) setGyldigTil(gyldighetsdato)
    }
}
