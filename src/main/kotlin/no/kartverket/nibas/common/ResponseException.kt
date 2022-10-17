package no.kartverket.nibas.common

sealed class ResponseException(msg: String): RuntimeException(msg) {
    class NotFoundException(msg: String) : ResponseException(msg)
    class BadRequestException(msg: String) : ResponseException(msg)
}
