package no.kartverket.nibas.nibasserverapi.web

import no.kartverket.nibas.nibasserverapi.domain.Layer
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.Optional

@RestController
@RequestMapping("/layers")
class LayerController {

    val layers = listOf(
            Layer(1, "Layer 1"),
            Layer(2, "Layer 2"),
            Layer(3, "Layer 3"))

    @GetMapping("/", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun getAll(): ResponseEntity<List<Layer>> {
        return ResponseEntity.of(Optional.of(layers));
    }

    @GetMapping("/{layerId}", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun getOne(@PathVariable layerId: Long): ResponseEntity<Layer> {
        return ResponseEntity.ok(layers.firstOrNull { it -> it.id == layerId });
    }
}