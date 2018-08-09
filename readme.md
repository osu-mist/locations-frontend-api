# Locations API

## Dropwizard and JDK

Current implementations of the Dropwizard and JDK.

* [Dropwizard 1.0.2](http://www.dropwizard.io/1.0.2/docs/)
* [JDK 8](https://jdk8.java.net/)

## Skeleton

This API is based on the web-api-skeleton. For more documentation on the skeleton and the framework, see the github repo: https://github.com/osu-mist/web-api-skeleton

## Prerequisites

* Install [Elasticsearch](https://www.elastic.co/) or run Elasticsearch in a docker container:

  ```
  $ docker run \
  -p 9200:9200 \
  -p 9300:9300 \
  docker.elastic.co/elasticsearch/elasticsearch:6.2.4
  ```

* [Post index templates](https://github.com/osu-mist/locations-api#post-to-elasticsearch) to Elasticsearch.

* Use [location-api](https://github.com/osu-mist/locations-api) to fetch `locations-combined.json` and `services.json` by calling `/locations/combined` and `/locations/services`. You can also get these files from the workspaces of the following Jenkins jobs:

  * [Locations API Backend ES Update (api-dev)](http://act-jenkins.ucsadm.oregonstate.edu:8080/view/APIs/job/apis%20Locations%20API%20Backend%20ES%20Update%20%28api-dev%29)
  * [Locations API Backend ES Update (api-prod)](http://act-jenkins.ucsadm.oregonstate.edu:8080/view/APIs/job/apis%20Locations%20API%20Backend%20ES%20Update%20%28api-prod%29)

* Post binary data to Elasticsearch by using [ES Manager](https://github.com/osu-mist/es-manager):

  ```
  $ python3 esmanager.py -i locations -t locations locations.json
  $ python3 esmanager.py -i services -t services services.json
  ```

## Generate Keys

HTTPS is required for Web APIs in development and production. Use keytool(1) to generate public and private keys.

Generate key pair and keystore:

```
$ keytool \
  -genkeypair \
  -dname "CN=Jane Doe, OU=Enterprise Computing Services, O=Oregon State University, L=Corvallis, S=Oregon, C=US" \
  -ext "san=dns:localhost,ip:127.0.0.1" \
  -alias doej \
  -keyalg RSA \
  -keysize 2048 \
  -sigalg SHA256withRSA \
  -validity 365 \
  -keystore doej.keystore
```

Export certificate to file:

```
$ keytool \
  -exportcert \
  -rfc \
  -alias "doej" \
  -keystore doej.keystore \
  -file doej.pem
```

Import certificate into truststore:

```
$ keytool \
  -importcert \
  -alias "doej" \
  -file doej.pem \
  -keystore doej.truststore
```

## Gradle

This project uses the build automation tool Gradle. Use the [Gradle Wrapper](https://docs.gradle.org/current/userguide/gradle_wrapper.html) to download and install it automatically:

```
$ ./gradlew
```

The Gradle wrapper installs Gradle in the directory `~/.gradle`. To add it to your `$PATH`, add the following line to `~/.bashrc`:

```
$ export PATH=$PATH:/home/user/.gradle/wrapper/dists/gradle-2.4-all/WRAPPER_GENERATED_HASH/gradle-2.4/bin
```

The changes will take effect once you restart the terminal or `source ~/.bashrc`.

## Tasks

List all tasks runnable from root project:

```
$ gradle tasks
```

## IntelliJ IDEA

Generate IntelliJ IDEA project:

```
$ gradle idea
```

Open with `File` -> `Open Project`.

## Configure

Copy [configuration-example.yaml](configuration-example.yaml) to `configuration.yaml`. Modify as necessary, being careful to avoid committing sensitive data.

Please refer to [Location Frontend API](https://wiki.library.oregonstate.edu/confluence/display/CO/Location+Frontend+API) for `locations` session configuration

## Build

Build the project:

```
$ gradle build
```

JARs [will be saved](https://github.com/johnrengelman/shadow#using-the-default-plugin-task) into the directory `build/libs/`.

## Run

Run the project:

```
$ gradle run
```

## Contrib Files

Any code that contains intellectual property from a vendor should be stored in Github Enterprise instead of public Github. Make the name of the contrib repo in Github Enterprise follow this format using archivesBaseName in gradle.properties.

```
archivesBaseName-contrib
```

Set the value of getContribFiles to yes in gradle.properties.

```
getContribFiles=yes
```

Also set the value of contribCommit to the SHA1 of the desired commit to be used from the contrib repository.

```
contribCommit={SHA1}
```

Files in a Github Enterprise repo will be copied to this directory upon building the application.

```
$ gradle build
```

Contrib files are copied to:

```
/src/main/groovy/edu/oregonstate/mist/contrib/
```

## Base a New Project off the Skeleton

Clone the skeleton:

```
$ git clone --origin skeleton git@github.com:osu-mist/web-api-skeleton.git my-api
$ cd my-api
```

Rename the webapiskeleton package and SkeletonApplication class:

```
$ git mv src/main/groovy/edu/oregonstate/mist/webapiskeleton src/main/groovy/edu/oregonstate/mist/myapi
$ vim src/main/groovy/edu/oregonstate/mist/myapi/SkeletonApplication.class
```

Update gradle.properties with your package name and main class.

Replace swagger.yaml with your own API specification.

Update configuration-example.yaml as appropriate for your application.

Update the resource examples at the end of this readme.

## Base an Existing Project off the Skeleton

Add the skeleton as a remote:

```
$ git remote add skeleton git@github.com:osu-mist/web-api-skeleton.git
$ git fetch skeleton
```

Merge the skeleton into your codebase:

```
$ git checkout feature/abc-123-branch
$ git merge skeleton/master
...
$ git commit -v
```

## Incorporate Updates from the Skeleton

Fetch updates from the skeleton:

```
$ git fetch skeleton
```

Merge the updates into your codebase as before.
Note that changes to CodeNarc configuration may introduce build failures.

```
$ git checkout feature/abc-124-branch
$ git merge skeleton/master
...
$ git commit -v
```

## Resources

The Web API definition is contained in the [Swagger specification](swagger.yaml).

### GET /locations/{id}

This resource returns the information for a given building:

  $ curl https://localhost:8088/api/v0/locations/bacf847d8bf54ee7b6359b5f45751217 --cacert doej.pem --user "username:password"

```json
{
  "links": {},
  "data": {
  "id": "bacf847d8bf54ee7b6359b5f45751217",
  "type": "locations",
  "attributes": {
    "name": "Indoor Target Range",
    "tags": [],
    "openHours": {},
    "type": "building",
    "abbreviation": "ITR",
    "geometry": {
    "type": "Polygon",
    "coordinates": [
      [
      [
        -123.275167,
        44.563115
      ],
      [
        -123.275348,
        44.563116
      ],
      [
        -123.275348,
        44.563032
      ],
      [
        -123.275353,
        44.563032
      ],
      [
        -123.275353,
        44.56298
      ],
      [
        -123.275349,
        44.56298
      ],
      [
        -123.275349,
        44.562924
      ],
      [
        -123.275344,
        44.562924
      ],
      [
        -123.275344,
        44.562925
      ],
      [
        -123.27527,
        44.562925
      ],
      [
        -123.27527,
        44.562924
      ],
      [
        -123.275263,
        44.562924
      ],
      [
        -123.275263,
        44.562925
      ],
      [
        -123.275221,
        44.562924
      ],
      [
        -123.275221,
        44.562924
      ],
      [
        -123.275215,
        44.562924
      ],
      [
        -123.275215,
        44.562924
      ],
      [
        -123.275168,
        44.562924
      ],
      [
        -123.275168,
        44.562924
      ],
      [
        -123.275163,
        44.562924
      ],
      [
        -123.275162,
        44.562972
      ],
      [
        -123.275168,
        44.562972
      ],
      [
        -123.275168,
        44.562979
      ],
      [
        -123.275162,
        44.562979
      ],
      [
        -123.275162,
        44.563031
      ],
      [
        -123.275168,
        44.563031
      ],
      [
        -123.275167,
        44.563115
      ]
      ]
    ]
    },
    "summary": null,
    "description": null,
    "address": null,
    "city": null,
    "state": null,
    "zip": null,
    "county": null,
    "telephone": null,
    "fax": null,
    "thumbnails": [],
    "images": [],
    "departments": null,
    "website": null,
    "sqft": null,
    "calendar": null,
    "campus": "corvallis",
    "giRestroomCount": 0,
    "giRestroomLimit": null,
    "giRestroomLocations": null,
    "latitude": "44.56302",
    "longitude": "-123.2753"
  },
  "links": {
    "self": "https://api.oregonstate.edu/v1/locations/bacf847d8bf54ee7b6359b5f45751217"
  },
  "relationships": null
  }
}
```

## GeoJSON

Locations API provides [GeoJSON](https://tools.ietf.org/html/rfc7946) format for `/locations` and `/locations/{id}` endpoints with the parameter `geojson=true`.

### /locations?geojson=true

This resource returns a GeoJSON object with `"type": "FeatureCollection"` no matter how many locations are retrieved:

```json
{
  "type": "FeatureCollection",
  "features": [
    {
      "type": "Feature",
      "geometry": {...},
      "properties": {...}
    },
    {
      "type": "Feature",
      "geometry": {...},
      "properties": {...}
    }
  ]
}
```

Note that for each GeoJSON object with `"type": "Feature"` under the member `"features"` stands for the every retrieved location.

### /locations/{id}?geojson=true

This resource returns a GeoJSON object with `"type": "Feature"` since it will only return one single object. However, there are three different cases which might be happened:

1. **Point && Polygon**

  Most of locations contain both `Point` (latitude and longitude) and `Polygon` attributes. If both attributes exist, they will be wrapped as a `type: GeometryCollection` GeoJSON object as following:

  ```json
  {
    "type": "FeatureCollection",
    "features": [
      {
        "type": "Feature",
        "geometry": {
          "type": "GeometryCollection",
          "geometries": [
            {
              "type": "Polygon",
              "coordinates": [...]
            },
            {
              "type": "Point",
              "coordinates": [...]
            }
          ]
        },
        "properties": {
          "id": "b018683aa0e551280d1422301f8fb249",
          "type": "locations",
          "attributes": {
            "name": "Austin Hall",
            "tags": [],
            "openHours": {},
            "type": "building",
            "abbreviation": "Aust",
            "summary": null,
            "description": "",
            "address": "2751 SW Jefferson Way",
            "city": "CORVALLIS",
            "state": "OR",
            "zip": "97331",
            "county": null,
            "telephone": null,
            "fax": null,
            "thumbnails": [
              "http://map.dev.acquia.cws.oregonstate.edu/sites/map.oregonstate.edu/files/styles/thumbnail/public/locations/austin.jpg"
            ],
            "images": [
              "http://map.dev.acquia.cws.oregonstate.edu/sites/map.oregonstate.edu/files/locations/austin.jpg"
            ],
            "departments": [],
            "website": "http://map.dev.acquia.cws.oregonstate.edu/?id=b018683aa0e551280d1422301f8fb249",
            "sqft": null,
            "calendar": null,
            "campus": "Corvallis",
            "giRestroomCount": 2,
            "giRestroomLimit": false,
            "giRestroomLocations": "0191, 0193",
            "synonyms": [
              "College of Business"
            ],
            "bldgID": "0090",
            "parkingZoneGroup": null,
            "propID": null
          },
          "links": {
            "self": "https://api.oregonstate.edu/v1/locations/b018683aa0e551280d1422301f8fb249"
          },
          "relationships": null
        }
      }
    ]
  }
  ```

2. **Point || Polygon**

  If only one of those exist, then the `"geometry"` field will only have the existing one:

  ```json
  {
    "type": "Feature",
    "geometry": {
      "type": "Polygon",
      "coordinates": [...]
    },
    "properties": {
      "id": "937eada7c23344d68d0d6fc5ce906cdf",
      "type": "locations",
      "attributes": {
        "name": "KERR ADMINISTRATION WEST LOT",
        "tags": [],
        "openHours": {},
        "type": "parking",
        "abbreviation": null,
        "summary": null,
        "description": null,
        "address": null,
        "city": null,
        "state": null,
        "zip": null,
        "county": null,
        "telephone": null,
        "fax": null,
        "thumbnails": [],
        "images": [],
        "departments": [],
        "website": null,
        "sqft": null,
        "calendar": null,
        "campus": "corvallis",
        "giRestroomCount": null,
        "giRestroomLimit": null,
        "giRestroomLocations": null,
        "synonyms": [],
        "bldgID": null,
        "parkingZoneGroup": "Short-term Lot",
        "propID": "3263"
      },
      "links": {
        "self": "https://api.oregonstate.edu/v1/locations/937eada7c23344d68d0d6fc5ce906cdf"
      },
      "relationships": null
    }
  }
  ```

  or

  ```json
  {
    "type": "Feature",
    "geometry": {
      "type": "Point",
      "coordinates": [...]
    },
    "properties": {
      "id": "dd90d825dc2f8b5bb5b72b3d41a46d87",
      "type": "locations",
      "attributes": {
        "name": "Boardwalk Cafe",
        "tags": [],
        "openHours": {
          "1": [
            {
              "start": "2017-11-07T01:00:00Z",
              "end": "2017-11-07T04:00:00Z"
            },
            {
              "start": "2017-11-06T19:00:00Z",
              "end": "2017-11-06T22:00:00Z"
            },
            {
              "start": "2017-11-06T15:00:00Z",
              "end": "2017-11-06T18:00:00Z"
            }
          ],
          "2": [
            {
              "start": "2017-11-08T01:00:00Z",
              "end": "2017-11-08T04:00:00Z"
            },
            {
              "start": "2017-11-07T19:00:00Z",
              "end": "2017-11-07T22:00:00Z"
            },
            {
              "start": "2017-11-07T15:00:00Z",
              "end": "2017-11-07T18:00:00Z"
            }
          ],
          "3": [
            {
              "start": "2017-11-09T01:00:00Z",
              "end": "2017-11-09T04:00:00Z"
            },
            {
              "start": "2017-11-08T19:00:00Z",
              "end": "2017-11-08T22:00:00Z"
            },
            {
              "start": "2017-11-08T15:00:00Z",
              "end": "2017-11-08T18:00:00Z"
            }
          ],
          "4": [
            {
              "start": "2017-11-03T00:00:00Z",
              "end": "2017-11-03T03:00:00Z"
            },
            {
              "start": "2017-11-02T18:00:00Z",
              "end": "2017-11-02T21:00:00Z"
            },
            {
              "start": "2017-11-02T14:00:00Z",
              "end": "2017-11-02T17:00:00Z"
            }
          ],
          "5": [
            {
              "start": "2017-11-04T00:00:00Z",
              "end": "2017-11-04T03:00:00Z"
            },
            {
              "start": "2017-11-03T18:00:00Z",
              "end": "2017-11-03T21:00:00Z"
            },
            {
              "start": "2017-11-03T14:00:00Z",
              "end": "2017-11-03T17:00:00Z"
            }
          ],
          "6": [
            {
              "start": "2017-11-04T16:00:00Z",
              "end": "2017-11-05T03:00:00Z"
            }
          ],
          "7": [
            {
              "start": "2017-11-05T17:00:00Z",
              "end": "2017-11-06T04:00:00Z"
            }
          ]
        },
        "type": "dining",
        "abbreviation": null,
        "summary": "Zone: McNary Dining",
        "description": "",
        "address": "1300 SW Jefferson Avenue (McNary Dining)",
        "city": null,
        "state": null,
        "zip": null,
        "county": null,
        "telephone": null,
        "fax": null,
        "thumbnails": [],
        "images": [],
        "departments": [],
        "website": "http://map.dev.acquia.cws.oregonstate.edu/?id=dd90d825dc2f8b5bb5b72b3d41a46d87",
        "sqft": null,
        "calendar": null,
        "campus": "corvallis",
        "giRestroomCount": null,
        "giRestroomLimit": null,
        "giRestroomLocations": null,
        "synonyms": [],
        "bldgID": null,
        "parkingZoneGroup": null,
        "propID": null
      },
      "links": {
        "self": "https://api.oregonstate.edu/v1/locations/dd90d825dc2f8b5bb5b72b3d41a46d87"
      },
      "relationships": null
    }
  }
  ```

3. **!Point && !Polygon**

  If none of them exists, then the `"geometry"` field will be `null` object:

  ```json
  {
    "type": "Feature",
    "geometry": null,
    "properties": {
      "id": "5d3231555780488ab8d22e764bae5805",
      "type": "locations",
      "attributes": {
        "name": "Sinnhuber Aquatic Research Lab",
        "tags": [],
        "openHours": {},
        "type": "building",
        "abbreviation": null,
        "summary": null,
        "description": null,
        "address": "28645 E. HWY 34",
        "city": "CORVALLIS",
        "state": "OR",
        "zip": "97333",
        "county": null,
        "telephone": null,
        "fax": null,
        "thumbnails": [],
        "images": [],
        "departments": [],
        "website": null,
        "sqft": null,
        "calendar": null,
        "campus": "Corvallis",
        "giRestroomCount": 0,
        "giRestroomLimit": null,
        "giRestroomLocations": null,
        "synonyms": [],
        "bldgID": "0491",
        "parkingZoneGroup": null,
        "propID": null
      },
      "links": {
        "self": "https://api.oregonstate.edu/v1/locations/5d3231555780488ab8d22e764bae5805"
      },
      "relationships": null
    }
  }
  ```
