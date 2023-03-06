package no.kartverket.nibas.domain

import java.io.Serializable

/**
 * Datatype Identifikasjon er beskrevet her:
 * https://objektkatalog.geonorge.no/Diagram/Index/EAID_F47F753A_C178_4be5_91FE_2BFB2AD68F3B
 *
 * Vi ble enige på en fellesdag 2.11.22 om at vi ignorerer feltene
 * -navnerom
 * -versjonId
 * Det gir ikke mening å sette navnerom til en verdi fra Matrikkelen, så oppdatere objektet
 * i Inndelinger, men aldri kunne oppdatere navnerommet. VersjonId ble ikke brukt.
 * Navnerom henger igjen fra før man brukte UUID-er, da var ID-ene unike innenfor et navnerom.
 */
interface ContainsSosiIdentifikasjon {
    /**
     * Må være satt og skal aldri endre seg i objektets levetid.
     */
    val lokalid: String
}
