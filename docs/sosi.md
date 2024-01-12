# Eksempel på deserialisering av en sosi-fil

```kotlin
// Parse en lokal fil 
val root = NibasSosiDeserializer().deserialize(FileInputStream(File("fil.sosi")))

// Hent ut detaljer om sosi-filen
val hode = root.getHode()
hode.getSosiVersjon()
hode.getTegnsett()

// Hente alle kurver
root.getKurver()

// Hente alle flater
root.getFlater()

// Iterere over alle kurver og hente informasjon
root.getKurver().forEach { kurve ->
    println("Kurve: ${kurve.getId()}")
    kurve.getSosiNo().forEach { no ->
        no.getKoordinater().forEach { koordinat ->
            println("Koordinat: ${koordinat.north}, ${koordinat.east}")
        }
    }
}
```
