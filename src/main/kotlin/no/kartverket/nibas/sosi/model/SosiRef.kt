package no.kartverket.nibas.sosi.model

/**
 * SosiRef er en referanse til et annet objekt i sosi-filen.
 * En SosiRef er typisk benyttet i en flate, som f.eks kommune eller fylke (eller nasjon) der
 * flate utgjøres av alle grensene som blir referert til.
 *
 * Eksempel 1:
 * ..REF :111 :222
 * Dette viser en referanse til to andre objekter og det er de objektenes koordinater som utgjør flaten.
 *
 * Eksempel 2:
 * ..REF :111 :222 :-333
 * Dette viser en referanse til tre objeter, der koordinatene i objektet med id 333 skal leses i omvendt rekkefølge
 * for at flaten skal bli sluttet.
 *
 * Eksempel 3:
 * .. REF :111 :222 :333 (:444 :555)
 * Dette viser en refereanse til fem objekter der 111, 222 og 333 utgjør et lukket polygon. 444 og 555 representerer
 * et hull i flaten.
 *
 * Eksempel 1
 */
class SosiRef(niva: Int, verdier: MutableList<SosiVerdi>) : SosiObjekt(type = "REF", niva, verdier = verdier) {
    fun getVerdi(): String = verdier[0].verdi.replace(":", "")
    fun isRetningReversed(): Boolean = getVerdiUtenParentes().startsWith("-")
    fun getVerdiUtenRetning(): String = getVerdi().replace("-", "")
    fun getVerdiUtenParentes(): String = getVerdi().replace("(", "").replace(")", "")
    fun getVerdiUtenRetningOgParentes(): String = getVerdiUtenRetning().replace("(", "").replace(")", "")
}