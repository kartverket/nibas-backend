package no.kartverket.nibas.kodelister

interface KodelisteService {
    /**
     * Throws NoSuchElementException hvis ikke match
     */
    fun getKommunenummerIdByCodeValue(codeValue: String): String

    fun getCodeValueByKommunenummerId(kommunenummerId: String): String

    fun getCodeValueByFylkesnummerId(fylkesnummerId: String): String?

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

    fun getKommunenumre(): KodelisteRespons
    fun getFylkesnumre(): KodelisteRespons
    fun getMaalemetodeKoder(): KodelisteRespons
    fun getLandkoder(): KodelisteRespons
    fun getGrensetyper(): KodelisteRespons
}
