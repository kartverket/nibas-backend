# nibas-backend

Spring Boot applikasjon som serverer informasjon om administrative grenser.

# Innholdsfortegnelse

- [Bygge](#bygge)
- [Formatering](#formatering)

# Bygge <a name="bygge"></a>
`./gradlew assemble`

# Kjøre på lokal maskin
* Hent aut-lib fra github
```
$ ./gradlew clean build -Ppat=<PERSONLIG_PAT> -PpatUser=<BRUKERNAVN_GITHUB>
```

* Definer følgende miljøvariable:
  * VAULT_TOKEN (Logg inn på Vault, og velg "Copy Token" øverst til høyre under profil-menyen)

* Start opp nibas-db. Se [README.md](https://gitlab.statkart.no/nibas/nibas-db/-/blob/master/README.md) i nibas-db

* Start nibas-backend:
```
./gradlew bootRun --args='--spring.profiles.active=dev'
``` 
* Dersom du får en sertifikat-feilmelding mot vault under oppstart er det trolig relatert til https://stackoverflow.com/questions/60790118/java-kubernetes-client-sslhandshakeexception-extension-5-should-not-be-present - da må vi spesifisere at TLSv1.2 skal benyttes (propertyen TLSversion settes som system propertyen jdk.tls.client.protocols)
```
./gradlew bootRun -PTLSversion=TLSv1.2 --args='--spring.profiles.active=dev'
``` 

* Det er mulig å disable autentisering for lokal kjøring. Dette gjøres ved å kjøre med spring-profilen security-off. Man kan kjøre med flere profiler samtidig, så det er bare å legge den til (komma-separert) til der du vanligvis spesifiserer spring-profilen (om det er på kommandolinja eller i intellij run config)

# Hente docker image fra ghcr.io
`docker pull ghcr.io/kartverket/nibas-backend:<versjonsnummer>`

# Starte docker image fra harbor
`docker run -d --name nibas-backend -p 8080:8080 -e SPRING_PROFILES_ACTIVE=dev -e VAULT_TOKEN=<Logg inn på Vault, og vel "Copy Token" øverst til høyre under profil-menyen> harbor.statkart.no/nibas/nibas-backend:<versjonsnummer>` 

# Bygge lokalt docker image
```
./gradlew assemble
docker build --build-arg project_version_arg=0.0.1-SNAPSHOT . -t nibas-backend:0.0.1-SNAPSHOT
```

# Starte lokalt docker image
`docker run -d --name nibas-backend -p 8080:8080 -e DB_USER=postgres -e DB_PASSWORD=<samme-passord-som-db-ble-startet-med> -e DB_CONNECTION_STRING=jdbc:postgresql://<ip-address>:5432/postgres nibas-backend:0.0.1-SNAPSHOT`

# Manuell database migrering
Databasen migreres ved oppstart av applikasjonen, men dersom du ønsker å gjøre dette manuelt kan det gjøres på følgende måter:

## Migrere lokal database
`./gradlew -Dflyway.user=nibas -Dflyway.password=<se passord i vault> -Dflyway.url=jdbc:postgresql://localhost:5432/nibas flywayMigrate`

## Starte med tom database (ved lokal testing) - flywayClean
`./gradlew -Dflyway.user=nibas -Dflyway.password=<se passord i vault> -Dflyway.url=jdbc:postgresql://localhost:5432/nibas flywayClean`


# Ressurser
Gitt at <adresse-til-nibas> = localhost:8080
* Open API Specification: [http://<adresse-til-nibas>/api-docs](http://localhost:8080/api-docs)
* Swagger UI: [http://<adresse-til-nibas>/swagger-ui.html](http://localhost:8080/swagger-ui.html)

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

## Formatering <a name="formatering"></a>

Vi bruker EditorConfig for formatering (som IntelliJ default forstår). I tillegg benyttes Detekt i byggeløypa (anbefalt av SKIP). Dette kan en også sette opp i 
IntelliJ for å få varsler der. Gå til File->Settings->Plugins og installer Detekt-plugin. Etterpå gå til File->Settings->Tools->Detekt. Huk av Enable Detekt, 
Enable rules og Enable formatting. Under Configuration Files, legg til stien til detekt.yml som ligger på rota.
