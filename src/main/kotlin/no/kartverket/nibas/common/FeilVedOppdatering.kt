package no.kartverket.nibas.common

sealed class FeilVedOppdatering {

    class OppdateringException(val oppdateringInfo: OppdateringInfo) : RuntimeException("Det er ikke mulig å oppdatere objekt.")

    enum class Type { FYLKE, KOMMUNE, NASJON, GRUNNKRETS, STEMMEKRETS, GRENSE, BOPLIKTOMRAADE }

    data class OppdateringInfo(val id: Identifikator,
                               val type: Type,
                               val affectedIds: List<Identifikator> = emptyList()) {
        fun getMessage(): String =
            "Det er ikke mulig å oppdatere objekt med identifikator $id av type ${type.name}. " +
                "Dette berører følgende objekter med senere gyldigFra-tidspunkt: [${affectedIds.joinToString(", ")}]."
    }
}
