package no.kartverket.nibas.sosi.model

import no.kartverket.nibas.sosi.common.*

object SosiObjektFactory {
    fun createSosiObjekt(sosiLinje: SosiLinje): SosiObjekt {
        return when (val type = sosiLinje.type()) {
            SosiKoder.HODE -> SosiHode(sosiLinje.niva(), verdier = sosiLinje.verdier())
            SosiKoder.TRANSPAR -> SosiTranspar(sosiLinje.niva(), verdier = sosiLinje.verdier())
            SosiKoder.OMRADE -> SosiOmrade(sosiLinje.niva(), verdier = sosiLinje.verdier())
            SosiKoder.BUEP -> SosiBuep(sosiLinje.niva(), verdier = sosiLinje.verdier())
            SosiKoder.FLATE -> SosiFlate(sosiLinje.niva(), verdier = sosiLinje.verdier())
            SosiKoder.KURVE -> SosiKurve(sosiLinje.niva(), verdier = sosiLinje.verdier())
            SosiKoder.NO -> SosiNordOest(niva = sosiLinje.niva(), verdier = toNoKoordinat(sosiLinje.verdier()))
            SosiKoder.ORIGO -> SosiNordOest(type = SosiKoder.ORIGO, niva = sosiLinje.niva(), verdier = toNoKoordinat(sosiLinje.verdier()))
            SosiKoder.MIN_NO -> SosiNordOest(type = SosiKoder.MIN_NO, niva = sosiLinje.niva(), verdier = toNoKoordinat(sosiLinje.verdier()))
            SosiKoder.MAKS_NO -> SosiNordOest(type = SosiKoder.MAKS_NO, niva = sosiLinje.niva(), verdier = toNoKoordinat(sosiLinje.verdier()))
            SosiKoder.NOH -> SosiNoh(niva = sosiLinje.niva(), verdier = toNohKoordinat(sosiLinje.verdier()))
            SosiKoder.IDENT -> SosiIdent(sosiLinje.niva(), toNoKoordinat(sosiLinje.verdier()))
            SosiKoder.REF -> SosiReferanse(sosiLinje.niva(), verdier = sosiLinje.verdier())
            SosiKoder.KVALITET -> SosiKvalitet(sosiLinje.niva(), verdier = sosiLinje.verdier())
            SosiKoder.FYLKESNAVN -> SosiNavn(type = SosiKoder.FYLKESNAVN, sosiLinje.niva(), sosiLinje.verdier())
            SosiKoder.KOMMUNENAVN -> SosiNavn(type = SosiKoder.KOMMUNENAVN, sosiLinje.niva(), sosiLinje.verdier())
            SosiKoder.NASJONNAVN -> SosiNavn(type = SosiKoder.NASJONNAVN, sosiLinje.niva(), sosiLinje.verdier())
            SosiKoder.DOKUMENTASJONSREFERANSE -> SosiDokumentasjonsReferanse(sosiLinje.niva(), verdier = sosiLinje.verdier())
            SosiKoder.GRUNNKRETSID -> SosiGrunnkretsId(type = SosiKoder.GRUNNKRETSID, sosiLinje.niva(), sosiLinje.verdier())
            SosiKoder.STEMMEKRETSID -> SosiStemmekretsId(type = SosiKoder.STEMMEKRETSID, sosiLinje.niva(), verdier = sosiLinje.verdier())
            else -> SosiObjekt(type = type, niva = sosiLinje.niva(), verdier = sosiLinje.verdier())
        }
    }

    fun toNoKoordinat(verdi: MutableList<SosiVerdi>): MutableList<SosiVerdi> {
        return verdi.chunked(2).map { s -> SosiKoordinat(s[0].verdi, s[1].verdi) }.toMutableList()
    }

    fun toNohKoordinat(verdi: MutableList<SosiVerdi>): MutableList<SosiVerdi> {
        return verdi.chunked(3).map { s -> SosiKoordinat(s[0].verdi, s[1].verdi, s[2].verdi) }.toMutableList()
    }
}
