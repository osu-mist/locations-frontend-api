# Locations API.

## Skeleton

This API is based on the web-api-skeleton. For more documentation on the skeleton and the framework, see the github repo: https://github.com/osu-mist/web-api-skeleton

## Prerequisites

+ Install [elasticsearch](https://www.elastic.co/)
+ Use [location-api](https://github.com/osu-mist/locations-api) to fetch `locations-dining.json`, `locations-extension.json` and `locations-arcgis.json` by calling `/locations/dining`, `/locations/extension` and `/locations/arcgis`
+ Post binary data to elasticsearch

```bash
curl -s -XPOST localhost:9200/locations/locations/_bulk --data-binary "@path-to-locations-arcgis.json"; echo
curl -s -XPOST localhost:9200/locations/locations/_bulk --data-binary "@path-to-locations-extension.json"; echo
curl -s -XPOST localhost:9200/locations/locations/_bulk --data-binary "@path-to-locations-dining.json"; echo
```

## Configure


Copy [configuration-example.yaml](configuration-example.yaml) to `configuration.yaml`. Modify as necessary, being careful to avoid committing sensitive data.

Please refer to [Location Frontend API](https://wiki.library.oregonstate.edu/confluence/display/CO/Location+Frontend+API) for `locations` session configuration

Build the project:

    $ gradle build

JARs [will be saved](https://github.com/johnrengelman/shadow#using-the-default-plugin-task) into the directory `build/libs/`.

## Run

Run the project:

    $ gradle run

## Resources

The Web API definition is contained in the [Swagger specification](swagger.yaml).


### GET /locations/{id}

This resource returns the information for a given building:

    $ curl https://localhost:8088/api/v0/locations/a5041ecde8b53e54c7479e770825d7c1 --cacert doej.pem --user "username:password"

```json
{
    "links":{},
    "data":{"id":"a5041ecde8b53e54c7479e770825d7c1",
            "type":"locations",
            "attributes":{"name":"Indoor Target Range",
                          "abbreviation":"ITR",
                          "latitude":"44.56302",
                          "longitude":"-123.2753",
                          "summary":null,
                          "description":null,
                          "address":null,
                          "city":null,
                          "state":null,
                          "zip":null,
                          "county":null,
                          "telephone":null,
                          "fax":null,
                          "thumbnails":null,
                          "images":null,
                          "departments":null,
                          "website":null,
                          "sqft":null,
                          "calendar":null,
                          "campus":"corvallis",
                          "type":"building","openHours":{}},
            "links":{"self":"https://api.oregonstate.edu/v1/locations/a5041ecde8b53e54c7479e770825d7c1"}}
}
```
