# Locations Data Discrepancy Checking Script README

[dataDiffCheck.py](dataDiffCheck.py) is a small Python script that inspects [locations-api-frontend](https://github.com/osu-mist/locations-frontend-api) json data from its [/locations](../swagger.yaml#L12) endpoint between revisions to determine any data discrepancies.

It'll report any locations that no longer exist in the new data. (Peavy Hall, which was torn down, is an example of this)

It'll report locations that recieved a new key (rekeyed in Elastic Search) but still exists in the new data set. (Johnson Hall is an example of this)

The buildings that have both an old and new key will be outputed to a json file called [buildingsWithOldNewKeys.json](buildingsWithOldNewKeys.json).

Finally it'll report any new locations that weren't in the old data at all. (Tykeson Hall is an example of this)

## Invocation

It expects the json data files as command line arguments in this form.

`python dataDiffCheck.py oldJson.json newJson.json > report.txt`

The necessary json files can be aquired via these curl commands.

The latest data can be aquired from a local instance of the frontend + the elastic search container updated w/ the newest data (via the dataReset.sh script).

`curl -X GET 'https://localhost:8082/api/v0/locations?page%5Bsize%5D=10000&type=building'`

`curl -X GET 'https://oregonstateuniversity-dev.apigee.net/v1/locations?page%5Bsize%5D=10000&type=building'`

Relevant basic auth header / oauth access tokens are still necessary for the curl commands.