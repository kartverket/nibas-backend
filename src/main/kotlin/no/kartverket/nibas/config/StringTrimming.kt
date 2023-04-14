package no.kartverket.nibas.config

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.deser.std.StdScalarDeserializer
import com.fasterxml.jackson.databind.module.SimpleModule
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
