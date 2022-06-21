# nibas-backend

Spring Boot applikasjon som serverer informasjon om administrative grenser.

## Innholdsfortegnelse

- [Bygge](#bygge)
- [Kjøre på lokal maskin](#lokal)
- [Autentisering](#autentisering)
- [Formatering](#formatering)
- [Import](#import)

## Bygge <a name="bygge"></a>

* Bygge fra command line

```
$ ./gradlew clean build -Ppat=<PERSONLIG_PAT> -Pgh_username=<BRUKERNAVN_GITHUB>
```

Propertiene pat og gh_username må være tilstede for å kunne hente artifakter som aut-lib fra Github.

Alternativt til å angi Gradle-properties på command line så kan det være kjekt å definere en fil gradle.properties som må legges i katalogen GRADLE_USER_HOME
med følgende innhold:

```
pat=<PERSONLIG_PAT>
gh_username=<BRUKERNAVN_GITHUB>
```

Da vil nedlasting av artifakter (som aut-lib) fra Github fungere og en vil også kunne bruke Gradle Sync fra IntelliJ uten å bygge først. Da vil også
source-jaren bli lastet ned.

## Kjøre på lokal maskin <a name="lokal"></a>

* Definer følgende miljøvariable:
    * VAULT_TOKEN (Logg inn på Vault, og velg "Copy Token" øverst til høyre under profil-menyen)

* Start opp nibas-db. Se [README.md](https://github.com/kartverket/nibas-db) i nibas-db

* Start nibas-backend:

```
./gradlew bootRun --args='--spring.profiles.active=localhost'
``` 

* Dersom du får en sertifikat-feilmelding mot vault under oppstart er det trolig relatert
  til https://stackoverflow.com/questions/60790118/java-kubernetes-client-sslhandshakeexception-extension-5-should-not-be-present - da må vi spesifisere at
  TLSv1.2 skal benyttes (propertyen TLSversion settes som system propertyen jdk.tls.client.protocols)

```
./gradlew bootRun -PTLSversion=TLSv1.2 --args='--spring.profiles.active=localhost'
``` 

## Autentisering <a name="autentisering"></a>

Applikasjonen starter default med autentisering påskrudd. Dette betyr at alle API-endepunkt er sikret og trenger en Authorization-header for å kunne aksesseres.

### Disable autentisering ved lokal kjøring

Man kan disable sikkerhet ved lokal kjøring ved å bruke spring-profilen security-off. Man kan kjøre med flere profiler samtidig, så det er bare å legge den
til (komma-separert) til der du vanligvis spesifiserer spring-profilen (om det er på kommandolinja eller i intellij run config). Alle kall uten denne headeren
resulterer i en 401 Unauthorized

### Test av API utenfor klienten

For å teste et endepunkt (med autentisering påskrudd) uten å kalle det via nibas-frontend trenger man å legge på en Authorization-header på kallet. Man henter
dette enklest ved å gjøre følgende:

* gå til test (http://nibas.test.skip.statkart.no/)
* logg inn med en av følgende brukere (brukernavn og passord finnes her https://docs.digdir.no/idporten_testbrukere.html)
    * 21079408678, 21079408597, 21079408406, 21079408325, 21079408244
* inspiser det initielle kallet for å hente fylker (chrome dev tools eller lignende) og finn Authorization-headeren.
* kopier med verdien av denne headeren og legg det på som en ny header med nøkkel "Authorization" i det verktøyet du bruker for å teste API-et (eksempelvis
  postman)
* utfør rest-kallet

Voila! Du får tak i data fra API-et

### Testing av sikrede kontrollere fra kode

Alle tester som arver fra [NibasMvcTest](src/test/kotlin/no/kartverket/nibas/NibasMvcTest.kt) har skrudd av sikkerhet (med profilen security-off).  
Ønsker man å bruke NibasMvcTest, men allikevel skru på sikkerhet kan man override profilene som er satt opp i NibasMvcTest (se for
eksempel [WebSecurityConfigTest](src/test/kotlin/no/kartverket/nibas/config/WebSecurityConfigTest.kt)):

```kotlin
@ActiveProfiles("dev", inheritProfiles = false)
```

## Hente docker image fra ghcr.io

`docker pull ghcr.io/kartverket/nibas-backend:<versjonsnummer>`

## Starte docker image fra harbor

`docker run -d --name nibas-backend -p 8080:8080 -e SPRING_PROFILES_ACTIVE=dev -e VAULT_TOKEN=<Logg inn på Vault, og vel "Copy Token" øverst til høyre under profil-menyen> harbor.statkart.no/nibas/nibas-backend:<versjonsnummer>`

## Bygge lokalt docker image

```
./gradlew assemble
docker build --build-arg project_version_arg=0.0.1-SNAPSHOT . -t nibas-backend:0.0.1-SNAPSHOT
```

## Starte lokalt docker image

`docker run -d --name nibas-backend -p 8080:8080 -e DB_USER=postgres -e DB_PASSWORD=<samme-passord-som-db-ble-startet-med> -e DB_CONNECTION_STRING=jdbc:postgresql://<ip-address>:5432/postgres nibas-backend:0.0.1-SNAPSHOT`

## Manuell database migrering

Databasen migreres ved oppstart av applikasjonen, men dersom du ønsker å gjøre dette manuelt kan det gjøres på følgende måter:

## Migrere lokal database

`./gradlew -Dflyway.user=nibas -Dflyway.password=<se passord i vault> -Dflyway.url=jdbc:postgresql://localhost:5432/nibas flywayMigrate`

## Starte med tom database (ved lokal testing) - flywayClean

`./gradlew -Dflyway.user=nibas -Dflyway.password=<se passord i vault> -Dflyway.url=jdbc:postgresql://localhost:5432/nibas flywayClean`

## Ressurser

Gitt at <adresse-til-nibas> = localhost:8080

* Open API Specification: [http://<adresse-til-nibas>/api-docs](http://localhost:8080/api-docs)
* Swagger UI: [http://<adresse-til-nibas>/swagger-ui.html](http://localhost:8080/swagger-ui.html)

## Eksempel på deserialisering av en sosi-fil

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

## Import <a name="import"></a>

Sosi-importen nås på endpoint: http://HOST-URL/v1/import

Pga ytelse er det lurt å kjøre denne med følgende setting i application.yml. For kjøring i dev-miljøet må en også endre minnesetting i nibas-backend.tf til
3Gig.

```
open-in-view: false
```

Eksempel på kjøring med security påskrudd mot dev-miljø (må da sende med jwt):

```
curl http://nibas.dev.skip.statkart.no/v1/import -F file=@ADM_Enheter_Norge.sos -H "Authorization: Bearer JWT_TOKEN_LEGGES_VED_HER"
```

### Import-jobb for grunnkretser

Det er mulig å sende med et parameter fylker. Da vil en kun importere grunnkretsene for de angitte fylker. Se enum Fylkesnummer for mulige verdier. Hvis en
ikke angir fylkesparameter kjøres jobben for alle 14.000 grunnkretser. Før en kjører import for grunnkretser må en ha kjørt import for administrative enheter.
Eksempel på kjøring for fylkene Agder + Rogaland:

```
curl http://localhost:8080/v1/import -F file=@STAT_enheter_grunnkretser.sos -F fylker=42,11
```




