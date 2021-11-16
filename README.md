# nibas-server-api

Spring Boot applikasjon som serverer informasjon om administrative grenser

# Bygge
`./gradlew assemble`

# Kjøre på lokal maskin
* Definer følgende miljøvariable:
  * VAULT_TOKEN (Logg inn på Vault, og vel "Copy Token" øverst til høyre under profil-menyen)
* Start opp nibas-db. Se [README.md](https://gitlab.statkart.no/nibas/nibas-db/-/blob/master/README.md) i nibas-db
* Kjør Flyway migrering i nibas-db. Se [README.md](https://gitlab.statkart.no/nibas/nibas-db/-/blob/master/README.md) i nibas-db

* Start nibas-server-api:
```
./gradlew bootRun --args='--spring.profiles.active=dev'
``` 
* Dersom du får en sertifikat-feilmelding mot vault under oppstart er det trolig relatert til https://stackoverflow.com/questions/60790118/java-kubernetes-client-sslhandshakeexception-extension-5-should-not-be-present - da må vi spesifisere at TLSv1.2 skal benyttes (propertyen TLSversion settes som system propertyen jdk.tls.client.protocols)
```
./gradlew bootRun -PTLSversion=TLSv1.2 --args='--spring.profiles.active=dev'
``` 


# Hente docker image fra harbor
`docker pull harbor.statkart.no/nibas/nibas-server-api:<versjonsnummer>`

# Starte docker image fra harbor
`docker run -d --name nibas-server-api -p 8080:8080 -e DB_USER=postgres -e DB_PASSWORD=<samme-passord-som-db-ble-startet-med> -e DB_CONNECTION_STRING=jdbc:postgresql://<ip-address>:5432/postgres harbor.statkart.no/nibas/nibas-server-api:<versjonsnummer>` 

# Bygge lokalt docker image
```
./gradlew assemble
docker build --build-arg project_version_arg=0.0.1-SNAPSHOT . -t nibas-server-api:0.0.1-SNAPSHOT
```

# Starte lokalt docker image
`docker run -d --name nibas-server-api -p 8080:8080 -e DB_USER=postgres -e DB_PASSWORD=<samme-passord-som-db-ble-startet-med> -e DB_CONNECTION_STRING=jdbc:postgresql://<ip-address>:5432/postgres nibas-server-api:0.0.1-SNAPSHOT`

# Ressurser
Gitt att <adresse-til-nibas> = localhost:8080
* Open API Specification: [http://<adresse-til-nibas>/api-docs](http://localhost:8080/api-docs)
* Swagger UI: [http://<adresse-til-nibas>/swagger-ui.html](http://localhost:8080/swagger-ui.html)
