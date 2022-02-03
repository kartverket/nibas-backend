package no.kartverket.nibas.domain

import no.kartverket.nibas.sosi.model.SosiIdent
import no.kartverket.nibas.sosi.model.lokalIdIsValidUUID

/**
 * Datatype Identifikasjon er beskrevet her:
 * https://objektkatalog.geonorge.no/Diagram/Index/EAID_F47F753A_C178_4be5_91FE_2BFB2AD68F3B
 */
interface ContainsSosiIdentifikasjon {
    /**
     * Må være satt og skal aldri endre seg i objektets levetid. Så burde ideelt vært immutable.
     */
    var lokalid: String

    /**
     * Må være satt ihht standard og skal aldri endre seg i objektets levetid. Så burde ideelt vært immutable.
     */
    var navnerom: String

    /**
     * Ikke påkrevet i sosi-modellen.
     */
    var versjonid: String?
}

/**
 * Default er det satt en UUID for lokalId i default Nibas-navnerom.
 * Dersom det kommer inn en gyldig UUID i importen, så skal denne videreføres, og da må også
 * opprinnelig navnerom benyttes.
 */
fun ContainsSosiIdentifikasjon.updateSosiIdentifikasjon(
    sosiIdent: SosiIdent?
) {
    // Gyldig UUID fra eksternt system
    if (sosiIdent != null && sosiIdent.lokalIdIsValidUUID()) {
        this.lokalid = sosiIdent.getLokalId()!!

        // Navnerom oppdateres også (hvis angitt), siden det henger sammen med lokalId.
        val navnerom = sosiIdent.getNavnerom()
        if (!navnerom.isNullOrEmpty()) {
            this.navnerom = navnerom
        }
    }

    this.versjonid = sosiIdent?.getVersjonId()
}
