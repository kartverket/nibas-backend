package no.kartverket.nibas.kodelister

interface KodelisteService {
    /**
     * Throws NoSuchElementException hvis ikke match
     */
    fun getKommunenummerIdByCodeValue(codeValue: String): String

    /**
     * Throws NoSuchElementException hvis ikke match
     */
    fun getFylkesnummerIdByCodeValue(codeValue: String): String

    /**
     * Throws NoSuchElementException hvis ikke match
     */
    fun getMalemetodeIdByCodeValue(codeValue: String): String

    /**
     * Skedulert metode som automatisk invaliderer/tømmer kodelistene ved jevnt konfigurerbart mellomrom.
     *
     * Tømmer cachede kodelister (de eksternt forvaltede).
     */
    fun clearCachedeKodelister()

    fun getMaalemetodeKoder(): KodelisteRespons
}
