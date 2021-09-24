# nibas-server-api

Spring Boot applikasjon som serverer informasjon om administrative grenser

# Bygge
`./gradlew assemble`

# Kjøre på lokal maskin
1. Start opp nibas-db. Se [README.md](https://gitlab.statkart.no/nibas/nibas-db/-/blob/master/README.md) i nibas-db
2. Kjør Flyway migrering i nibas-db. Se [README.md](https://gitlab.statkart.no/nibas/nibas-db/-/blob/master/README.md) i nibas-db
3. Finn ip-addressen til nibas-db:
   ```
   # Finn container-id til nibas-db
   docker ps 
   
   # Finn ip-addressen til nibas-db
   docker inspect <container-id> | grep IPAddress
   ```
4. Start nibas-server-api:
`./gradlew bootRun --args='--spring.datasource.url=jdbc:postgresql://localhost:5432/postgres --spring.datasource.username=postgres --spring.datasource.password=<samme-passord-som-db-ble-startet-med>'` 

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