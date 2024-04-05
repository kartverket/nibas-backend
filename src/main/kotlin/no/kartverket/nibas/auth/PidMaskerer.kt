package no.kartverket.nibas.auth

import java.util.regex.Pattern

object PidMaskerer {
    fun getMaskedName(name: String): String {
        val elevenDigitsRegex = Pattern.compile("\\d{11}").toRegex()

        val nameTrimmed = name.trim()
        return if (nameTrimmed.matches(elevenDigitsRegex)) {
            nameTrimmed.replaceRange(nameTrimmed.length - 5, nameTrimmed.length, "XXXXX")
        } else {
            name
        }
    }
}
