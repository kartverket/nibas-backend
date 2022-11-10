package no.kartverket.nibas.sosi.sosiimport

enum class Fylkesnummer(val fylkesnummer: String) {
    VIKEN("30"),
    OSLO("03"),
    INNLANDET("34"),
    VESTFOLD_OG_TELEMARK("38"),
    AGDER("42"),
    ROGALAND("11"),
    VESTLAND("46"),
    MOERE_OG_ROMSDAL("15"),
    TROENDELAG("50"),
    NORDLAND("18"),
    TROMS_OG_FINNMARK("54");

    companion object {
        fun fromValue(value: String): Fylkesnummer = Fylkesnummer.values().first { it.fylkesnummer == value }
    }
}
