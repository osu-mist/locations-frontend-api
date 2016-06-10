package edu.oregonstate.mist.locations.frontend.db

import com.fasterxml.jackson.databind.ObjectMapper

/**
 * Handles HTTP requests against ElasticSearch. Operation supported are:
 * search and findById.
 */
class LocationDAO {
    private final Map<String, String> locationConfiguration

    LocationDAO(Map<String, String> locationConfiguration) {
        this.locationConfiguration = locationConfiguration
    }

    /**
     * Searches ES (elasticsearch) for locations matching "q" full text search within the given
     * campus and type.
     *
     * @param q                 Query text to use for full text search
     * @param campus            Campus to use to filter results
     * @param type              Type of location to filter results
     * @param pageNumber
     * @param pageSize
     *
     * @return json             JSON search results from ES
     */
    String search(String q, String campus, String type, Integer pageNumber, Integer pageSize) {
        ObjectMapper mapper = new ObjectMapper()

        // generate ES query to search for locations
        def esQuery = getESSearchQuery(q, campus, type, pageNumber, pageSize)
        String esQueryJson = mapper.writeValueAsString(esQuery)

        // get data from ES
        def url = new URL("${ESFullUrl}/_search")
        URLConnection connection = postRequest(url, esQueryJson)
        connection.content.text
    }

    /**
     * Return a single location object with the matching id
     *
     * @param id
     * @return
     */
    String getById(String id) {
        try {
            return "${ESFullUrl}/${id?.toLowerCase()}/_source".toURL().text
        } catch (FileNotFoundException e) {
            return null
        }
    }

    String getGatewayUrl() {
        locationConfiguration.get("gatewayUrl")
    }

    /**
     * Returns url of elastic search collection and type to search.
     *
     * @return
     */
    private GString getESFullUrl() {
        String esUrl = locationConfiguration.get("esUrl")
        String esIndex = locationConfiguration.get("esIndex")
        String esType = locationConfiguration.get("estype")

        "${esUrl}/${esIndex}/${esType}"
    }

    /**
     * Performs POST request.
     *
     * This groovy approach is used instead of jerseyClient because jersey client kept
     * throwing no response errors.
     *
     * @param url
     * @param esQueryJson
     * @return
     */
    private URLConnection postRequest(URL url, String esQueryJson) {
        def connection = url.openConnection()
        connection.setRequestMethod("POST")
        connection.doOutput = true

        def writer = new OutputStreamWriter(connection.outputStream)
        writer.write(esQueryJson)
        writer.flush()
        writer.close()
        connection.connect()
        connection
    }

    /**
     * Generate ElasticSearch query to list locations by campus, type and full text search.
     *
     * @param campus
     * @param type
     * @param q
     * @param pageNumber
     * @param pageSize
     * @param query
     * @return
     */
    private def getESSearchQuery(String q, String campus, String type, int pageNumber, int pageSize) {
        def esQuery = [
                "query": [
                    "filtered": [
                        "filter": [
                            "bool": [
                                "must": []
                            ]
                        ],
                        "query": [:]
                    ]
                ],
                "from" : (pageNumber - 1) * pageSize,
                "size" : pageSize
        ]

        if (campus) {
            esQuery.query.filtered.filter.bool.must += ["term": ["attributes.campus": campus]]
        }

        if (type) {
            esQuery.query.filtered.filter.bool.must += ["term": ["attributes.type": type]]
        }

        if (q) {
            esQuery.query.filtered.query = ["query_string": ["query": q]]
        } else {
            esQuery.query.filtered.query = [ "match_all": [:] ]
        }

        esQuery
    }
}
