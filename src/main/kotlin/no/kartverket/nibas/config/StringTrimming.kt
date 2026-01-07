package no.kartverket.nibas.config

import tools.jackson.core.JsonParser
import tools.jackson.databind.DeserializationContext
import tools.jackson.databind.deser.std.StdScalarDeserializer
import tools.jackson.databind.module.SimpleModule
import org.springframework.stereotype.Component

@Component
class StringTrimModule : SimpleModule() {
    init {
        this.addDeserializer(String::class.java, TrimStringJsonDeserializer)
    }
}

object TrimStringJsonDeserializer : StdScalarDeserializer<String>(String::class.java) {
    override fun deserialize(jsonParser: JsonParser, ctxt: DeserializationContext): String {
        return jsonParser.valueAsString.trim()
    }
}
