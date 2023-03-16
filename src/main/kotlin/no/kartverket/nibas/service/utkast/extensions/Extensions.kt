package no.kartverket.nibas.service.utkast.extensions

import no.kartverket.nibas.api.v1.common.Grenseendringer
import no.kartverket.nibas.api.v1.common.Metadataendringer
import no.kartverket.nibas.domain.utkast.Utkast

fun Utkast.publiseringsInfo(): String =
    StringBuilder("Publiserer utkast '${this.navn}' (id: ${this.id}): ")
        .append(this.operasjoner?.metadataendringer?.logFriendlyString() ?: "")
        .append(this.operasjoner?.grenseendringer?.logFriendlyString() ?: "")
        .append("\n")
        .toString()

private fun Metadataendringer.logFriendlyString(): String {
    val logBuilder = StringBuilder("")
    val nasjon = this.nasjonsendringer
    val fylke = this.fylkesendringer
    val kommune = this.kommuneendringer
    val grunnkrets = this.grunnkretsendringer
    val stemmekrets = this.stemmekretsendringer
    val newLine = "\n -> "
    if (nasjon.isNotEmpty()) {
        logBuilder.append(newLine).append("Nasjonsendringer: ${nasjon.size}")
    }
    if (fylke.isNotEmpty()) {
        logBuilder.append(newLine).append("Fylkesendringer: ${fylke.size}")
    }
    if (kommune.isNotEmpty()) {
        logBuilder.append(newLine).append("Kommuneendringer: ${kommune.size}")
    }
    if (grunnkrets.isNotEmpty()) {
        logBuilder.append(newLine).append("Grunnkretsendringer: ${grunnkrets.size}")
    }
    if (stemmekrets.isNotEmpty()) {
        logBuilder.append(newLine).append("Stemmekretsendringer: ${stemmekrets.size}")
    }
    return logBuilder.toString()
}

private fun Grenseendringer.logFriendlyString(): String {
    val logBuilder = StringBuilder("")
    val newLine = "\n -> "
    val endredeFeatures = this.endredeFeatures
    if (endredeFeatures.isNotEmpty()) {
        logBuilder.append(newLine).append("Grenseendringer: ${endredeFeatures.size}")
    }
    return logBuilder.toString()
}
