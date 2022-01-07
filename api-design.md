# Fylker

## GET http://localhost:8080/v1/fylker

```json
[
  {
    "id": 1,
    "navn": [
      {
        "navn": "Vestfold og Telemark",
        "spraak": "nor"
      }
    ],
    "href": "http://localhost:8080/v1/fylker/1"
  },
  {
    "id": 2,
    "navn": [
      {
        "navn": "Agder",
        "spraak": "nor"
      }
    ],
    "href": "http://localhost:8080/v1/fylker/2"
  }
]
```

## GET http://localhost:8080/v1/fylker/1

```json
{
  "id": 1,
  "administrativenhetnavn": [
    {
      "navn": "Vestfold og Telemark",
      "spraak": "nor"
    }
  ],
  "lokalid": "172780",
  "navnerom": "NO.KARTVERKET.AdmEnheterNorge",
  "fylkesnummer": {
      "id": "3906fc69-056d-4a89-8874-9d13b7af1092",
      "kodeverdi": 2301
  },
  "samiskforvaltningsomraade": false,
  "oppdateringsdato": "2020-02-07",
  "features": {
    "type": "FeatureCollection",
    "features": [
      {
        "type": "Feature",
        "properties": {
          "type": "Posisjon"
        },
        "geometry": {
          "type": "Point",
          "coordinates": [
            293037.47000000003,
            7030801.73
          ]
        }
      },
      {
        "type": "Feature",
        "properties": {
          "type": "Flate"
        },
        "geometry": {
          "type": "Polygon",
          "coordinates": [
            [
              [
                292666.43,
                7030892.79
              ],
              [
                292745.13,
                7030727.47
              ],
              [
                292745.13,
                7030727.47
              ]
            ]
          ]
        }
      }
    ]
  }
}
```

## GET http://localhost:8080/v1/fylker/1/grenser

```json
{
  "type": "FeatureCollection",
  "features": [
    {
      "type": "Feature",
      "properties": {
        "kontekstType": "FYLKE",
        "kontekstId": 1,
        "id": 1000,
        "type": "Fylkesgrense",
        "lokalid": "12345",
        "navnerom": "NO.KARTVERKET.AdmEnheterNorge"
      },
      "geometry": {
        "type": "LineString",
        "coordinates": [
          [
            120007.01000000001,
            6563801.95
          ],
          [
            120000.36,
            6563774.33
          ]
        ]
      }
    },
    {
      "type": "Feature",
      "properties": {
        "id": 3922,
        "kontekstType": "FYLKE",
        "kontekstId": 1,
        "type": "Riksgrense",
        "lokalid": "234455",
        "navnerom": "NO.KARTVERKET.AdmEnheterNorge"
      },
      "geometry": {
        "type": "LineString",
        "coordinates": [
          [
            120007.01000000001,
            6563801.95
          ],
          [
            120000.36,
            6563774.33
          ]
        ]
      }
    }
  ]
}
``` 

# Kommuner

## GET http://localhost:8080/v1/kommuner

```json
[
  {
    "id": 1,
    "administrativenhetnavn": [
      {
        "navn": "Malvik",
        "spraak": "nor"
      }
    ],
    "href": "http://localhost:8080/v1/kommuner/1"
  },
  {
    "id": 2,
    "administrativenhetnavn": [
      {
        "navn": "Bergen",
        "spraak": "nor"
      }
    ],
    "href": "http://localhost:8080/v1/kommuner/2"
  }
]
```

## GET http://localhost:8080/v1/kommuner/1

```json
{
  "id": 1,
  "administrativenhetnavn": [
    {
      "navn": "Malvik",
      "spraak": "nor"
    }
  ],
  "lokalid": "172780",
  "navnerom": "NO.KARTVERKET.AdmEnheterNorge",
  "kommunenummer": {
      "id": "3906fc69-056d-4a89-8874-9d13b7af1092",
      "kodeverdi": 2301
  },
  "samiskforvaltningsomraade": false,
  "oppdateringsdato": "2020-02-07",
  "features": {
    "type": "FeatureCollection",
    "features": [
      {
        "type": "Feature",
        "properties": {
          "navn": "Posisjon"
        },
        "geometry": {
          "type": "Point",
          "coordinates": [
            293037.47000000003,
            7030801.73
          ]
        }
      },
      {
        "type": "Feature",
        "properties": {
          "navn": "Flate"
        },
        "geometry": {
          "type": "Polygon",
          "coordinates": [
            [
              [
                292666.43,
                7030892.79
              ],
              [
                292745.13,
                7030727.47
              ],
              [
                292745.13,
                7030727.47
              ]
            ]
          ]
        }
      }
    ]
  }
}
```

## GET http://localhost:8080/v1/kommuner/1/grenser

```json
{
  "type": "FeatureCollection",
  "features": [
    {
      "type": "Feature",
      "properties": {
        "id": 2344,
        "kontekstType": "FYLKE",
        "kontekstId": 1,
        "type": "Kommunegrense",
        "lokalid": "12345",
        "navnerom": "NO.KARTVERKET.AdmEnheterNorge"
      },
      "geometry": {
        "type": "LineString",
        "coordinates": [
          [
            120007.01000000001,
            6563801.95
          ],
          [
            120000.36,
            6563774.33
          ]
        ]
      }
    },
    {
      "type": "Feature",
      "properties": {
        "id": 3922,
        "type": "Riksgrense",
        "lokalid": "234455",
        "navnerom": "NO.KARTVERKET.AdmEnheterNorge"
      },
      "geometry": {
        "type": "LineString",
        "coordinates": [
          [
            120007.01000000001,
            6563801.95
          ],
          [
            120000.36,
            6563774.33
          ]
        ]
      }
    }
  ]
}
``` 

## GET http://localhost:8080/v1/kommuner/1/historikk
```json
[
  {
    "id": 1,
    "href": "http://localhost:8080/v1/kommuner/1/historikk/1"
  },
  {
    "id": 2,
    "href": "http://localhost:8080/v1/kommuner/1/historikk/2"
  }
]
```
## GET http://localhost:8080/v1/kommuner/1/historikk/1

# Grenser
# Hente grense
### GET http://localhost:8080/v1/grenser/kommunegrenser/1
### GET http://localhost:8080/v1/grenser/kommunegrenser/1/historikk
### GET http://localhost:8080/v1/grenser/kommunegrenser/1/historikk/1

# Hente historikk på grense

## Oppdatere grenser
### POST http://localhost:8080/v1/grenser/
```json
{
  "type": "FeatureCollection",
  "features": [
    {
      "type": "Feature",
      "properties": {
        "id": 2344,
        "kontekstType": "FYLKE",
        "kontekstId": 1,
        "type": "Kommunegrense",
        "lokalid": "12345",
        "navnerom": "NO.KARTVERKET.AdmEnheterNorge"
      },
      "geometry": {
        "type": "LineString",
        "coordinates": [
          [
            120007.01000000001,
            6563801.95
          ],
          [
            120000.36,
            6563774.33
          ]
        ]
      }
    },
    {
      "type": "Feature",
      "properties": {
        "id": 3922,
        "type": "Riksgrense",
        "lokalid": "234455",
        "navnerom": "NO.KARTVERKET.AdmEnheterNorge"
      },
      "geometry": {
        "type": "LineString",
        "coordinates": [
          [
            120007.01000000001,
            6563801.95
          ],
          [
            120000.36,
            6563774.33
          ]
        ]
      }
    }
  ]
}
```