package no.kartverket.nibas.config

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import io.hypersistence.utils.hibernate.type.util.ObjectMapperSupplier

class CustomHibernateObjectMapperSupplier: ObjectMapperSupplier {
    override fun get(): ObjectMapper {
        return ObjectMapper()
            .findAndRegisterModules()
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
    }
}
