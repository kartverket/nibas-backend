package no.kartverket.nibas.utils

import tools.jackson.databind.JsonNode
import tools.jackson.databind.node.ArrayNode
import tools.jackson.databind.node.JsonNodeType

fun sammenlignJsonFeltnavn(forventet: JsonNode, faktisk: JsonNode, arrayNavn: IdType): SammenligningResultat {
    if (forventet.isArray) {
        if (!faktisk.isArray) {
            return SammenligningResultat("Forventet array, faktisk var ${faktisk.nodeType}", false)
        }
        return sammenlignJsonLister(forventet as ArrayNode, faktisk as ArrayNode, arrayNavn)
    } else {
        if (!faktisk.isObject) {
            return SammenligningResultat("Forventet object, faktisk var ${faktisk.nodeType}", false)
        }
        return sammenlignJsonNoder(forventet, faktisk)
    }
}

private fun sammenlignJsonNoder(forventet: JsonNode, faktisk: JsonNode): SammenligningResultat {
    val forventedeFelter = forventet.properties().toList()
    val faktiskeFelter = faktisk.properties().toList()

    for (forventetFelt in forventedeFelter) {
        val faktiskFeltMedSammeNavn = faktiskeFelter.find { it.key == forventetFelt.key }
        if (faktiskFeltMedSammeNavn == null) {
            return SammenligningResultat("Node med navn ${forventetFelt.key} ble ikke funnet i faktisk respons: $faktiskeFelter", false)
        }

        if (felterHarIkkeSammeType(forventetFelt, faktiskFeltMedSammeNavn) && faktiskFeltMedSammeNavn.value.nodeType != JsonNodeType.NULL) {
            return SammenligningResultat("Felter med samme navn (${forventetFelt.key}) har ikke samme nodetype, forventet: ${forventetFelt.value.nodeType}, faktisk: ${faktiskFeltMedSammeNavn.value.nodeType}", false)
        }
        if (forventetFelt.value.isObject) {
            val resultatAvSammenligning = sammenlignJsonNoder(forventetFelt.value, faktiskFeltMedSammeNavn.value)
            if (!resultatAvSammenligning.erLike) {
                return resultatAvSammenligning
            }
        } else if (forventetFelt.value.isArray && forventetFelt.key != "coordinates") {
            val idType = if (forventetFelt.value.size() > 1) {
                when (forventetFelt.key) {
                    "kontekstEgenskaper" -> IdType.ID_LOKALID_VALUE
                    "features" -> IdType.ID_DIREKTE
                    else -> return SammenligningResultat("Fant ukjent listetype: ${forventetFelt.key}, vet ikke hvordan man slår opp ID", false)
                }
            } else {
                null
            }
            val resultatAvSammenligning = sammenlignJsonLister(forventetFelt.value as ArrayNode, faktiskFeltMedSammeNavn.value as ArrayNode, idType)
            if (!resultatAvSammenligning.erLike) {
                return resultatAvSammenligning
            }
        }
    }

    return SammenligningResultat("Noder er like", true)
}

private fun sammenlignJsonLister(forventet: ArrayNode, faktisk: ArrayNode, idType: IdType?): SammenligningResultat {
    forventet.forEach { forventetNode ->
        val faktiskeFelter = faktisk.toList()
        val faktiskNode = if (forventet.size() == 1) {
            faktiskeFelter[0]
        } else {
            when (idType) {
                IdType.ID_LOKALID_VALUE -> faktiskeFelter.find { it["id"]["lokalid"]["value"] == forventetNode["id"]["lokalid"]["value"] }
                IdType.ID_DIREKTE -> faktiskeFelter.find { it["id"] == forventetNode["id"] }
                null -> return SammenligningResultat("Trenger id-type for å slå opp element i lista", false)
            }
        }

        if (forventet.size() > 1 && faktiskNode == null) {
            return SammenligningResultat("Klarte ikke å finne tilsvarende liste basert på lokalid, forventet: ${forventetNode["id"]["lokalid"]["value"]}. Faktiske lokalid-er: ${faktiskeFelter.map { it["id"]["lokalid"]["value"] }}", false)
        }

        if (forventetNode.isObject) {
            val resultatAvSammenligning = sammenlignJsonNoder(forventetNode, faktiskNode!!)
            if (!resultatAvSammenligning.erLike) {
                return resultatAvSammenligning
            }
        }
    }

    return SammenligningResultat("Liste-elementer er like", true)
}

private fun felterHarIkkeSammeType(
    forventetFelt: MutableMap.MutableEntry<String, JsonNode>,
    faktiskFelt: Map.Entry<String, JsonNode>
): Boolean {
    val nodeTypenSkalSjekkes = listOf(JsonNodeType.STRING, JsonNodeType.OBJECT, JsonNodeType.ARRAY).contains(forventetFelt.value.nodeType)
    return nodeTypenSkalSjekkes && forventetFelt.value.nodeType != faktiskFelt.value.nodeType
}

data class SammenligningResultat(val resultat: String, val erLike: Boolean)

enum class IdType {
    ID_LOKALID_VALUE, ID_DIREKTE
}
