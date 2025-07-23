# nibas-backend

Spring Boot applikasjon som serverer informasjon om, og tilbyr forvaltning av, Norges administrative og statistiske grenser.

## Innholdsfortegnelse

- [Oppsett Lokalt](#oppsett)
- [Autentisering](#autentisering)
- [Diverse tilleggskommandoer](#kommandoer)
- [Ressurser](#ressurser)

# Oppsett Lokalt <a name="oppsett"></a>

For å sette opp applikasjonen lokalt trenger du følgende:

- JDK 21
- Docker
- PostgreSQL (versjon 14 anbefales)

## Miljøvariabler


### Generere PAT
For å kunne sjekke ut kode fra Nibas, må du ha satt opp en PAT
* [Personal Access Token (classic)](https://github.com/settings/tokens) er personlig og legges til GitHub profil.
* Generer nytt classic token.
* Sett scope til: `read:packages`.
* Husk å kopiere tokenet før man setter SSO og stegene under
* Sett SSO til Kartverket

For mer informasjon, se [GitHub dokumentasjon](https://docs.github.com/en/authentication/keeping-your-account-and-data-secure/creating-a-personal-access-token).

### Legg inn i gradle.properties
Rename `gradle.properties.template` til `gradle.properties` og legg inn tokenet du genererte i stedet for `<genererte token>`.

```
GH_PACKAGES_PAT=<genererte token>
```
`gradle.properties` er gitignored så den vil ikke bli committet.
`GH_PACKAGES_PAT` er for henting av matrikkelkode via maven.

### Matrikkel WFS

For at applikasjonen skal kunne koble til Matrikkelens WFS-tjeneste, må den konfigureres med riktig URL og påloggingsinformasjon. 
Dette gjøres ved å sette de følgende miljøvariablene.
Verdiene finner du i Google Secret Manager (se [hvordan](#secrets) lenger ned).

For lokal kjøring i IntelliJ er den enkleste metoden å legge dem til i din "Run/Debug Configuration" under feltet for "Environment variables":

*   `MATRIKKELEN_WFS_URL`=`<url-fra-secret-manager>`
*   `MATRIKKELEN_USERNAME`=`<brukernavn-fra-secret-manager>`
*   `MATRIKKELEN_PASSWORD`=`<passord-fra-secret-manager>`

Hvis du heller starter applikasjonen fra terminalen, kan du eksportere variablene før du kjører `bootRun`-kommandoen.

## Databasen lokalt

Man kan enten kjøre database med docker eller bare lokalt.

### Kjør med docker

Den letteste måten å kjøre det opp lokalt på egen maskin er ved å starte opp databasen via docker-compose.
Om man kjører docker-compose med filen `docker-compose-local.yml` vil den starte database i egen container. 
Dette kan gjøres enten ved å høyreklikke og velge "Run" i IntelliJ eller kjøre følgende kommando i kommandolinjen.

```bash
docker compose --file docker-compose-local.yml up
```

### Opprette database lokalt

For nibas-backend må vi opprette applikasjonsbruker og database manuelt.

```bash
# Logg på med postgres bruker
sudo -u postgres psql
```

Lag bruker og database.
Dette må matche det som er satt i 'application.yml'
```sql
CREATE ROLE nibas WITH LOGIN PASSWORD 'nibas';
create database nibas with owner = nibas;
```

### Importere data

Om dette er første gangen du setter opp applikasjonen vil databasen være tom. 
Du kan da importere en av database-dumpene som ligger i [google-cloud-bucketen](https://storage.cloud.google.com/utvikler-filer)

For å importere en databasedump kan du bruke psql med kommandoen:
```bash
psql -f {DB_DUMP_FILENAME} -h localhost -U nibas -d nibas
```

## Kjøre applikasjonen

### IntelliJ

I IntelliJ kan man starte main-metoden i `NibasBackendApplication.kt`. 
Husk å sette aktiv spring-profil til `localhost`. 
Dette gjøres under "Run/Debug Configurations" og vil se slik ut i IntelliJ:

![image](https://github.com/kartverket/nibas-backend/assets/39772759/6f6f8156-70d7-4d34-87df-8535d47980b7)


### Terminal

Alternativt kan man også starte applikasjonen via terminalen:

```bash
./gradlew bootRun --args='--spring.profiles.active=localhost'
```

```bash
./gradlew bootRun -PTLSversion=TLSv1.2 --args='--spring.profiles.active=localhost'
```

Nå skal applikasjonen være klar til bruk og du kan sette opp [nibas-frontend](https://github.com/kartverket/nibas-frontend) dersom du ønsker å ha den kjørende lokalt også.

## Integrasjon mot nibas-events

Om integrasjonen mot nibas-events skal fungere, må du gjøre følgende:

- Miljøvariabel `nibas-events.host` må settes til aktuell nibas-events applikasjon. Denne skal inneholde host:port.
- Dersom du kjører nibas-events, må det også finnes en miljøvariabel `api.key.publisher`. Den samme som nibas-events autentiserer mot.

## Finne fram secrets i Google Secret Manager <a name="secrets"></a>

Hvis du vil se secrets som er lagret for hvert miljø, så logg inn på Google Cloud Console (`console.cloud.google.com`), velg prosjekt etter hvilket miljø du vil se på (`nibas-dev` eller `nibas-prod`) og søk etter Secret Manager i søkefeltet i toppen.
Du trenger rollen `secretmanager.version.access` for å få lov til å trykke på en secret og `View secret value` under `Actions`.
For å få denne tilgangen kan du gå på `jit.skip.kartverket.no`, velge prosjekt (som er det samme som prosjektnavnet i Google Cloud Console), og be om rollen `secretmanager.admin` i en kort tidsperiode. Denne rollen inkluderer `secretmanager.version.access`, så du får sett de secretene du trenger.

## Kjøre mot database i annet miljø

Hvis du ønsker å kjøre mot en annen database enn localhost, kan du sette miljøvariablene `spring.datasource.url`, `spring.datasource.username` og `spring.datasource.password` til riktig verdi fra ønsket miljø i Google Secret Manager
(disse skrives med bindestrek i stedet for punktum inne i GSM, f.eks. `nibas-db-username` i stedet for `nibas.db.username`).

Disse kan settes i IntelliJ under "Run/Debug Configurations" eller ved å bruke Googles `gcloud`-kommandoer.

# Autentisering <a name="autentisering"></a>

Applikasjonen starter default med autentisering påskrudd.
Dette betyr at alle API-endepunkt er sikret og trenger en Authorization-header for å kunne aksesseres.
Med default-oppsett så vil man kun kunne logge inn med brukeren `05865396092` lokalt.
Men kan fint legge til flere brukere om man ønsker det ved å legge de til i miljøvariabelen `nibas.authorized.users`. 
Her legger man en kommeseparert liste med MD5-hashen av fødselsnummer man ønsker at skal være autoriserte for tjenesten.

## Disable autentisering ved lokal kjøring

Man kan disable sikkerhet ved lokal kjøring ved å bruke spring-profilen security-off. 
Man kan kjøre med flere profiler samtidig, så det er bare å legge den til (komma-separert) til der du vanligvis spesifiserer spring-profilen (om det er på kommandolinja eller i intellij run config). 
Alle kall uten denne headeren resulterer i en 401 Unauthorized.

Eksempel med `security-off` profil

```bash
./gradlew bootRun --args='--spring.profiles.active=localhost,security-off'
```

## Test av API utenfor klienten

For å teste et endepunkt (med autentisering påskrudd) uten å kalle det via nibas-frontend trenger man å legge på en Authorization-header på kallet. 
Man henter dette enklest ved å gjøre følgende:

* gå til dev: https://nibas-main.atkv3-dev.kartverket-intern.cloud/
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

# Diverse tilleggskommandoer <a name="kommandoer"></a>

Disse trenger man typisk ikke for normal bruk av applikasjonen, men kan være nyttige:

### Bygge lokalt docker image

```bash
./gradlew assemble
docker build --build-arg project_version_arg=0.0.1-SNAPSHOT . -t nibas-backend:0.0.1-SNAPSHOT
```

### Starte lokalt docker image

```bash
docker run -d --name nibas-backend -p 8080:8080 \
  -e DB_USER=postgres \
  -e DB_PASSWORD=<samme-passord-som-db-ble-startet-med> \
  -e DB_CONNECTION_STRING=jdbc:postgresql://<ip-address>:5432/postgres \
  nibas-backend:0.0.1-SNAPSHOT
```

# Ressurser <a name="ressurser"></a>

Gitt at <adresse-til-nibas> = localhost:8080

* Open API Specification: [http://<adresse-til-nibas>/api-docs](http://localhost:8080/api-docs)
* Swagger UI: [http://<adresse-til-nibas>/swagger-ui.html](http://localhost:8080/swagger-ui.html)
