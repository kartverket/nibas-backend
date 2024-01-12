# nibas-backend

Spring Boot applikasjon som serverer informasjon om administrative grenser.

## Innholdsfortegnelse

- [Oppsett](#oppsett)
- [Autentisering](#autentisering)
- [Diverse tilleggskommandoer](#kommandoer)
- [Ressurser](#ressurser)

# Oppsett <a name="oppsett"></a>

For å sette opp applikasjonen trenger du følgende:

- Java, versjon 17 skal fungere, men nyere versjoner kan få byggefeil
- PostgreSQL, versjon 16 er den nyligste versjonen vi har testet
- Docker, versjon 24 er den nyligste versjonen vi har testet
  
Du må også lage en `gradle.properties`-fil som skal ha følgende innhold:

```
pat=<PERSONLIG_PAT>
gh_username=<BRUKERNAVN_GITHUB>
```

Propertiene pat og gh_username må være tilstede for å kunne hente artifakter som aut-lib fra Github. Da vil nedlasting av artifakter (som aut-lib) fra Github fungere og en vil også kunne bruke Gradle Sync fra IntelliJ uten å bygge først. Da vil også source-jaren bli lastet ned.

## Kjøre databasen og vault

Dersom man ønsker å kjøre opp backenden utelukkende for utvikling av frontenden anbefales det å kjøre opp i terminalen for hvert av stegene som følger her. Men for å kunne debugge i IntelliJ så kreves det at man kjører opp backenden i IntelliJ, dermed bør man kjøre opp der dersom man driver med backend-utvikling.

Den letteste måten å kjøre det opp lokalt på egen maskin er ved å starte opp databasen og vault via docker-compose. Om man kjører docker-compose med filen `docker-compose-local.yml` vil starte vault og database i egne containere. Dette kan enten ved å høyreklikke og velge "Run" i IntelliJ eller kjøre følgende kommando i kommandolinjen.

```bash
docker compose --file docker-compose-local.yml up
```

### Opprette database i miljø

For nibas-backend må vi (foreløpig) opprette applikasjonsbruker og database manuelt.
Root-bruker og connection-detaljer per miljø finnes i Vault under nøkkel `nibas-db-root`

Logg på med root-bruker på gjeldende database og kjør følgende sql:

```sql
CREATE ROLE nibas WITH LOGIN PASSWORD '<lag et sikkert passord>';
create database nibas with owner = nibas;
```

### Importere data

Om dette er første gangen du setter opp applikasjonen vil databasen være tom. Du kan da importere en av database-dumpene som ligger i [google-cloud-bucketen](https://storage.cloud.google.com/utvikler-filer)

For å importere en databasedump kan du bruke psql med kommandoen:
```bash
psql -f {DB_DUMP_FILENAME} -h localhost -U nibas -d nibas
```

## Kjøre applikasjonen

Om man bruker intelliJ kan man da starte main-metoden i `NibasBackendApplication.kt`. Husk å sette aktiv spring-profil til `localhost,security-off`. Dette gjøres under "Run/Debug Configurations" og vil se slik ut i IntelliJ: 

![image](https://github.com/kartverket/nibas-backend/assets/39772759/6f6f8156-70d7-4d34-87df-8535d47980b7)

Alternativt kan man også starte applikasjonen via terminalen:

```
./gradlew bootRun --args='--spring.profiles.active=localhost'
``` 

* Dersom du får en sertifikat-feilmelding mot vault under oppstart er det trolig relatert til [denne](https://stackoverflow.com/questions/60790118/java-kubernetes-client-sslhandshakeexception-extension-5-should-not-be-present) saken - da må vi spesifisere at TLSv1.2 skal benyttes (propertyen TLSversion settes som system propertyen jdk.tls.client.protocols)

```
./gradlew bootRun -PTLSversion=TLSv1.2 --args='--spring.profiles.active=localhost'
```

Default så vil applikasjonen forsøke å bruke vault fra dev-miljøet. Men om du ønsker å bruke lokalt vault (kjørt opp med docker-compose) kan du overstyre dette med å sette miljøvariabelen `VAULT_ADDR=http://localhost:8200`.

Nå skal applikasjonen være klar til bruk og du kan sette opp [nibas-frontend](https://github.com/kartverket/nibas-frontend) dersom du ønsker å ha den kjørende lokalt også.

## Integrasjon mot nibas-events 
Om integrasjonen mot nibas-events skal fungere, må du gjøre følgende:

- Miljøvariabel `nibas-events.host` må settes til aktuell nibas-events applikasjon. Denne skal inneholde host:port.
- Dersom du kjører nibas-events med security, må det også finnes en miljøvariabel `api.key.publisher`. Den samme som nibas-events autentiserer mot.


# Autentisering <a name="autentisering"></a>

Applikasjonen starter default med autentisering påskrudd. Dette betyr at alle API-endepunkt er sikret og trenger en Authorization-header for å kunne aksesseres.

## Disable autentisering ved lokal kjøring

Man kan disable sikkerhet ved lokal kjøring ved å bruke spring-profilen security-off. Man kan kjøre med flere profiler samtidig, så det er bare å legge den
til (komma-separert) til der du vanligvis spesifiserer spring-profilen (om det er på kommandolinja eller i intellij run config). Alle kall uten denne headeren
resulterer i en 401 Unauthorized. Om man kjører med profilen `import` er denne profilen lagt til i tillegg automatisk.

## Test av API utenfor klienten

For å teste et endepunkt (med autentisering påskrudd) uten å kalle det via nibas-frontend trenger man å legge på en Authorization-header på kallet. Man henter
dette enklest ved å gjøre følgende:

* gå til test (https://nibas.test.skip.statkart.no/) (bruk vpn eller vdi)
* logg inn med en av brukerne definert på Testbrukere-siden (https://kartverket.atlassian.net/wiki/spaces/TNIBAS/pages/534282277/Testbrukere)
* inspiser det initielle kallet for å hente fylker (chrome dev tools eller lignende) og finn Authorization-headeren.
* kopier med verdien av denne headeren og legg det på som en ny header med nøkkel "Authorization" i det verktøyet du bruker for å teste API-et (eksempelvis
  postman)
* utfør rest-kallet

Voila! Du får tak i data fra API-et

## Testing av sikrede kontrollere fra kode

Alle tester som arver fra [NibasMvcTest](src/test/kotlin/no/kartverket/nibas/NibasMvcTest.kt) har skrudd av sikkerhet (med profilen security-off).  
Ønsker man å bruke NibasMvcTest, men allikevel skru på sikkerhet kan man override profilene som er satt opp i NibasMvcTest (se for
eksempel [WebSecurityConfigTest](src/test/kotlin/no/kartverket/nibas/config/WebSecurityConfigTest.kt)):

```kotlin
@ActiveProfiles("dev", inheritProfiles = false)
```

## Testbrukere i lokal Vault
Dersom du ønsker å teste med påskrudd sikkerhet lokalt, må det legges inn testbrukere i lokal vault.
Dette gjøres ved å legge inn følgende på sti `nibas/nibas-backend/nibas.authorized.users`: `42dfc6b0432747c8ed8aeea84562f201,4952921544649dddf0f20b4449ccaa5c,bd23d991ffd8b950373470ed4b27e965,5c3f2c0ce444a612bacd939912fccce9`
Dette tilsvarer fødselsnummer for følgende testbrukere:
11841497939,18929197670,05865396092,03894999895

# Diverse tilleggskommandoer <a name="kommandoer"></a>

Disse trenger man typisk ikke for normal bruk av applikasjonen, men kan være nyttige:

### Bygge lokalt docker image

```
./gradlew assemble
docker build --build-arg project_version_arg=0.0.1-SNAPSHOT . -t nibas-backend:0.0.1-SNAPSHOT
```

### Starte lokalt docker image

`docker run -d --name nibas-backend -p 8080:8080 -e DB_USER=postgres -e DB_PASSWORD=<samme-passord-som-db-ble-startet-med> -e DB_CONNECTION_STRING=jdbc:postgresql://<ip-address>:5432/postgres nibas-backend:0.0.1-SNAPSHOT`

# Ressurser <a name="ressurser"></a>

Gitt at <adresse-til-nibas> = localhost:8080

* Open API Specification: [http://<adresse-til-nibas>/api-docs](http://localhost:8080/api-docs)
* Swagger UI: [http://<adresse-til-nibas>/swagger-ui.html](http://localhost:8080/swagger-ui.html)
