# nibas-backend

Spring Boot applikasjon som serverer informasjon om, og tilbyr forvaltning av, Norges administrative og statistiske grenser.

## Innholdsfortegnelse

-   [Oppsett Lokalt](#oppsett)
-   [Autentisering](#autentisering)
-   [Tester](#tester)
-   [Diverse tilleggskommandoer](#kommandoer)
-   [Ressurser](#ressurser)
-   [Bidra](#bidra)
-   [Sikkerhet](#sikkerhet)
-   [Lisens](#lisens)

# Oppsett Lokalt <a name="oppsett"></a>

For å sette opp applikasjonen lokalt trenger du følgende:

-   JDK 21
-   Docker
-   PostgreSQL (versjon 16 anbefales)

## Miljøvariabler

### Generere PAT

For å bygge prosjektet må du ha tilgang til den private Maven-pakken `kartverket/matrikkelapi-v1-wsclient`. Hvis du har tilgang, må du sette opp en PAT:

-   [Personal Access Token (classic)](https://github.com/settings/tokens) er personlig og legges til GitHub profil.
-   Generer nytt classic token.
-   Sett scope til: `read:packages`.
-   Husk å kopiere tokenet før man setter SSO og stegene under
-   Sett SSO til Kartverket

For mer informasjon, se [GitHub dokumentasjon](https://docs.github.com/en/authentication/keeping-your-account-and-data-secure/creating-a-personal-access-token).

### Legg inn i local.properties

Opprett filen `local.properties` i prosjektets rotmappe og legg inn tokenet du genererte:

```
GH_PACKAGES_PAT=<genererte token>
```

`local.properties` er gitignored så den vil ikke bli committet.
`GH_PACKAGES_PAT` er for henting av matrikkelkode via maven.

Alternativt kan du sette `GH_PACKAGES_PAT` som miljøvariabel.

### Matrikkel WFS

For at applikasjonen skal kunne koble til Matrikkelens WFS-tjeneste, må den konfigureres med riktig URL og påloggingsinformasjon.
Dette gjøres ved å sette de følgende miljøvariablene.

For lokal kjøring i IntelliJ er den enkleste metoden å legge dem til i din "Run/Debug Configuration" under feltet for "Environment variables":

-   `MATRIKKELEN_WFS_URL`=`<url>`
-   `MATRIKKELEN_USERNAME`=`<brukernavn>`
-   `MATRIKKELEN_PASSWORD`=`<passord>`

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
Du kan da importere en database-dump du har tilgang til.

For å importere en databasedump kan du bruke psql med kommandoen:

```bash
psql -f {DB_DUMP_FILENAME} -h localhost -U nibas -d nibas
```

### Feilsøking
- Sjekk postgres versjon 
    ```bash
    postgres --version
    ```
- Sjekk om postgres porten er i bruk 
    ```bash
    lsof -i :5432
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

### Feilsøking

- Ved feil eller manglende imports fra Matrikkel (`no.statkart...`):
    - Sjekk at `GH_PACKAGES_PAT` i **Run Configuration** er korrekt
    - Tokenet må være **Personal Access Token (classic)** med `read:packages`
    - Husk å **SSO-autorisere tokenet for Kartverket**
- Ved Matrikkel-kodelistefeil eller runtime-feil mot Matrikkel:
    - Dobbeltsjekk `MATRIKKELEN_WFS_URL`, `MATRIKKELEN_USERNAME` og `MATRIKKELEN_PASSWORD` i **Run Configuration**
- Hvis build fungerer i terminal, men IntelliJ viser røde imports eller rare feil:
    - Kjør **File → Invalidate Caches… → Invalidate and Restart**
- Etter endring av miljøvariabler eller token kjør:
    ```bash
    ./gradlew clean build --refresh-dependencies
    ```

## Integrasjon mot nibas-events

Om integrasjonen mot nibas-events skal fungere, må du gjøre følgende:

-   Miljøvariabel `nibas-events.host` må settes til aktuell nibas-events applikasjon. Denne skal inneholde host:port.
-   Dersom du kjører nibas-events, må det også finnes en miljøvariabel `api.key.publisher`. Den samme som nibas-events autentiserer mot.

## Kjøre mot database i annet miljø

Hvis du ønsker å kjøre mot en annen database enn localhost, må miljøvariablene `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME` og `SPRING_DATASOURCE_PASSWORD` settes til verdiene for databasen.

# Autentisering <a name="autentisering"></a>

Applikasjonen benytter seg av sikkerhet på plattformnivå. Dette betyr at det ikke behandles noen tokens for å autentisere eller autorisere brukere.

## Test av API med autentisering

For å teste et endepunkt med autentisering må kallet inneholde en gyldig `Authorization`-header. Åpne backend-URL-en i nettleseren og logg inn med en godkjent testbruker. Finn `Authorization`-headeren i et autentisert kall i nettleserens utviklerverktøy, og legg den til i verktøyet du bruker for å teste API-et. Headerverdien skal behandles som en hemmelighet og må ikke deles eller lagres i repoet.

# Tester <a name="tester"></a>

Kjør enhetstestene:

```bash
./gradlew test
```

Kjør integrasjonstestene:

```bash
./gradlew integrationTest
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
  -e SPRING_PROFILES_ACTIVE=localhost \
  -e SPRING_DATASOURCE_USERNAME=nibas \
  -e SPRING_DATASOURCE_PASSWORD=nibas \
  -e SPRING_DATASOURCE_URL=jdbc:postgresql://<database-host>:5432/nibas \
  nibas-backend:0.0.1-SNAPSHOT
```

# Ressurser <a name="ressurser"></a>

Gitt at <adresse-til-nibas> = localhost:8080

-   Open API Specification: [http://<adresse-til-nibas>/api-docs](http://localhost:8080/api-docs)
-   Swagger UI: [http://<adresse-til-nibas>/swagger-ui.html](http://localhost:8080/swagger-ui.html)

# Bidra <a name="bidra"></a>

Kjør testene før du oppretter en pull request. Ikke legg passord, tokens eller andre hemmeligheter i kode eller konfigurasjonsfiler.

# Sikkerhet <a name="sikkerhet"></a>

Se [SECURITY.md](./.github/SECURITY.md) for rapportering av sårbarheter.

# Lisens <a name="lisens"></a>

Prosjektet er lisensiert under [MIT-lisensen](./LICENSE).
