package no.kartverket.nibas.nibasserverapi.config

import no.kartverket.nibas.nibasserverapi.api.v1.common.AdministrativEnhetType
import org.springframework.core.convert.converter.Converter

class StringToEnumConverter : Converter<String, AdministrativEnhetType> {
    override fun convert(source: String): AdministrativEnhetType {
        return AdministrativEnhetType.valueOf(source.uppercase())
    }
}