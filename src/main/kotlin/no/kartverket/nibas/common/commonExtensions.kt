package no.kartverket.nibas.common

import no.kartverket.nibas.domain.Administrativenhetnavn
import java.math.BigInteger
import java.security.MessageDigest
import java.time.LocalDate
import java.time.format.DateTimeFormatter

fun LocalDate.inThePast(): Boolean = this.isBefore(LocalDate.now())

fun LocalDate.toNorskFormat(): String = this.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))

fun String.md5(): String {
    val md = MessageDigest.getInstance("MD5")
    return BigInteger(1, md.digest(this.toByteArray())).toString(16).padStart(32, '0')
}

fun Collection<Administrativenhetnavn>.getInSpraak(spraak: String): String? =
    this.find { it.spraak?.id == spraak }?.navn
