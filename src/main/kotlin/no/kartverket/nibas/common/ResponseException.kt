package no.kartverket.nibas.common

sealed class ResponseException(val msg: String, val title: String, val additionalInfo: String? = null): RuntimeException(msg) {
    class NotFoundException(msg: String, title: String = "404: Ikke funnet", additionalInfo: String? = null) : ResponseException(msg, title, additionalInfo)
    class BadRequestException(msg: String, title: String = "404: Ikke funnet", additionalInfo: String? = null) : ResponseException(msg, title, additionalInfo)
}

// Ønsker HTTP 500 Internal Error
class UgyldigTilstandException(msg: String) : RuntimeException(msg)
