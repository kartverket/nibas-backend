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
eller på rot i mappen for prosjektet. Denne filen skal ligge i `.gitignore` og skal ikke sjekkes inn i prosjektet.

Filen skal ha følgende innhold:

```
pat=<PERSONLIG_PAT>
gh_username=<BRUKERNAVN_GITHUB>
```

Da vil nedlasting av artifakter (som aut-lib) fra Github fungere og en vil også kunne bruke Gradle Sync fra IntelliJ uten å bygge først. Da vil også
source-jaren bli lastet ned.

## Kjøre på lokal maskin <a name="lokal"></a>

Den letteste måten å kjøre det opp lokalt på egen maskin er ved å starte opp databasen og vault
via docker-compose. Om man kjører docker-compose med filen `docker-compose-local.yml` vil starte
vault og database i egne containere.

```html
docker compose --file docker-compose-local.yml up
```

Om man bruker intelliJ kan man da starte main-metoden i `NibasBackendApplication.kt`. Husk å sette
aktiv spring-profil til `localhost`. Dette gjøres under "Run/Debug Configurations".

Alternativt kan man også starte applikasjonen via terminalen:

```
./gradlew bootRun --args='--spring.profiles.active=localhost'
``` 

* Dersom du får en sertifikat-feilmelding mot vault under oppstart er det trolig relatert
  til https://stackoverflow.com/questions/60790118/java-kubernetes-client-sslhandshakeexception-extension-5-should-not-be-present - da må vi spesifisere at
  TLSv1.2 skal benyttes (propertyen TLSversion settes som system propertyen jdk.tls.client.protocols)

```
./gradlew bootRun -PTLSversion=TLSv1.2 --args='--spring.profiles.active=localhost'
```

Default så vil applikasjonen forsøke å bruke vault fra dev-miljøet. Men om du ønsker å bruke lokalt vault (kjørt opp med docker-compose) kan du overstyre dette
med å sette miljøvariabelen `VAULT_ADDR=http://localhost:8200`.

## Autentisering <a name="autentisering"></a>

Applikasjonen starter default med autentisering påskrudd. Dette betyr at alle API-endepunkt er sikret og trenger en Authorization-header for å kunne aksesseres.

### Disable autentisering ved lokal kjøring

Man kan disable sikkerhet ved lokal kjøring ved å bruke spring-profilen security-off. Man kan kjøre med flere profiler samtidig, så det er bare å legge den
til (komma-separert) til der du vanligvis spesifiserer spring-profilen (om det er på kommandolinja eller i intellij run config). Alle kall uten denne headeren
resulterer i en 401 Unauthorized. Om man kjører med profilen `import` er denne profilen lagt til i tillegg automatisk.

### Test av API utenfor klienten

For å teste et endepunkt (med autentisering påskrudd) uten å kalle det via nibas-frontend trenger man å legge på en Authorization-header på kallet. Man henter
dette enklest ved å gjøre følgende:

* gå til test (https://nibas.test.skip.statkart.no/) (bruk vpn eller vdi)
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

### Testbrukere i lokal Vault
Dersom du ønsker å teste med påskrudd sikkerhet lokalt, må det legges inn testbrukere i lokal vault.
Dette gjøres ved å legge inn følgende på sti `nibas/nibas-backend/nibas.authorized.users`: `b7f27cc2c77bceef60c7f7fdbbf6f3f9,c9bb6f93d076772dcaa47bf99bd4a4bc,0a5a25c693355ecb9119f7a5dfc31dfd,da7503ebff0141c38adb2656d730c472,c7c794d9483f5d807e996ff478d76db3`
Dette tilsvarer fødselsnummer for følgende testbrukere:
21079408678,21079408597,21079408406,21079408325,21079408244

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

`./gradlew -Dflyway.user=nibas -Dflyway.password=<se passord i vault> -Dflyway.url=jdbc:postgresql://localhost:5432/nibas -Dflyway.cleanDisabled="false" flywayClean`

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

Vi bruker EditorConfig for formatering (som IntelliJ default forstår).

## Import <a name="import"></a>

Sosi-import skal kjøres på lokal maskin mot gitte miljøer med kommandoer vist i eksempler under. Default kjøres import for alle fylker med import av
adm.enheter, grunnkretser og stemmekretser. En kan overstyre hvilke filer som skal brukes med konfig-properties. I tillegg kan en overstyre med:

* Property `nibas.import.fylkesnumre`: Spesifiserer en kommaseparert liste. Kun stemmekretser og grunnkretser i angitte fylker vil da importeres. Se
  [Fylkesnummer](src/main/kotlin/no/kartverket/nibas/sosi/sosiimport/Fylkesnummer.kt) for mulige verdier.
* Property `nibas.import.targets`: Spesifiserer en kommaseparert liste for å angi om kun grunnkretser, stemmekretser eller begge skal importeres.

Verdiene for url, username og password må hentes ut fra Vault for aktuelt miljø og sendes inn som Spring-properties slik som vist under. Url er på formatet
`jdbc:postgresql://localhost:5432/nibas`.
```
Eksempel på import mot enten lokal database, dev, test eller prod:
$ ./gradlew bootRun --args=' \
    --spring.profiles.active=import \
    --spring.datasource.url=URL_HER \
    --spring.datasource.username=USER_HER \ 
    --spring.datasource.password=PASSWORD_HER \
    '

Eksempel på import for bare 2 fylker (Agder + Rogaland):
$ ./gradlew bootRun --args=' \
    --spring.profiles.active=import \
    --spring.datasource.url=URL_HER \
    --spring.datasource.username=USER_HER \ 
    --spring.datasource.password=PASSWORD_HER \
    --nibas.import.fylkesnumre=11,42 \
    '
    
Eksempel på import med bare stemmekretser:
$ ./gradlew bootRun --args=' \
    --spring.profiles.active=import \
    --spring.datasource.url=URL_HER \
    --spring.datasource.username=USER_HER \ 
    --spring.datasource.password=PASSWORD_HER \
    --nibas.import.targets=stemmekrets \
    '
```
Dersom man heller vil kjøre det som en Spring boot-prosess, kan man spesifisere de samme propertiene som VM options:
```
-Dspring.datasource.url=URL_HER -Dspring.datasource.username=USER_HER -Dspring.datasource.password=PASSWORD_HER
```

Om du vil kjøre import og publisere events mot pubsub må følgende også gjøres i application-import.yml:

```application-import.yml
nibas:
  pubsub:
    enabled: true
```

Sett miljøvariabelen `GOOGLE_CLOUD_PROJECT` til korrekt nibas gcp project id. (Mot det miljø du vil importere til)
Autentiser deg mot GCP med kommandoen `gcloud auth login`

# Opprette database i miljø

For nibas-backend må vi (foreløpig) opprette applikasjonsbruker og database manuelt.
Root-bruker og connection-detaljer per miljø finnes i Vault under nøkkel `nibas-db-root`

Logg på med root-bruker på gjeldende database og kjør følgende sql-er:

```sql
CREATE ROLE nibas WITH LOGIN PASSWORD '<lag et sikkert passord>';
create database nibas with owner = nibas;
```

Opprett et innslag i Vault under nøkkel `nibas-db`. Dette skal ha følgende innhold:

```json
{
  "spring.datasource.password": "<passord>",
  "spring.datasource.url": "jdbc:postgresql://<server>:<port>/nibas",
  "spring.datasource.username": "nibas"
}
```




