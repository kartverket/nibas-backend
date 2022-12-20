package no.kartverket.nibas.common

import java.math.BigInteger
import java.security.MessageDigest
import java.time.LocalDate

fun LocalDate.inThePast(): Boolean = this.isBefore(LocalDate.now())
fun String.md5(): String {
    val md = MessageDigest.getInstance("MD5")
    return BigInteger(1, md.digest(this.toByteArray())).toString(16).padStart(32, '0')
}
