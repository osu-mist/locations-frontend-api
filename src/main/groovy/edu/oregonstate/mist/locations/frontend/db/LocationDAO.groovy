package edu.oregonstate.mist.locations.frontend.db

import com.fasterxml.jackson.databind.ObjectMapper
import org.elasticsearch.action.get.GetRequest
import org.elasticsearch.action.get.GetResponse
import org.elasticsearch.action.search.SearchRequest
import org.elasticsearch.action.search.SearchRequestBuilder
import org.elasticsearch.action.search.SearchType
import org.elasticsearch.client.transport.TransportClient
import org.elasticsearch.common.geo.GeoDistance
import org.elasticsearch.common.transport.InetSocketTransportAddress
import org.elasticsearch.common.unit.DistanceUnit
import org.elasticsearch.index.query.QueryBuilders
import org.elasticsearch.search.sort.GeoDistanceSortBuilder
import org.elasticsearch.search.sort.SortBuilders
import org.elasticsearch.search.sort.SortOrder
import org.joda.time.DateTime
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Handles HTTP requests against ElasticSearch. Operation supported are:
 * search and findById.
 */
//@groovy.transform.TypeChecked
@groovy.transform.InheritConstructors
class LocationDAO {
    private static final Logger LOGGER = LoggerFactory.getLogger(LocationDAO.class)

    private final URL esUrl
    private final String esIndex
    private final String esType
    private final String esIndexService
    private final String esTypeService

    private TransportClient esClient // TODO should be managed

    LocationDAO(Map<String, String> locationConfiguration) {
        esUrl = new URL(locationConfiguration.get("esUrl"))
        esIndex = locationConfiguration.get("esIndex")
        esType = locationConfiguration.get("estype")
        esIndexService = locationConfiguration.get("esIndexService")
        esTypeService = locationConfiguration.get("estypeService")

        this.esClient = TransportClient.builder().build()
        this.esClient.addTransportAddress(
                new InetSocketTransportAddress(
                        new InetSocketAddress(esUrl.host, 9300)))
        // TODO: don't hardcode port
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
    String search(String q, String campus, String type, Double lat,
                  Double lon, String searchDistance, Boolean isOpen,
                  Boolean giRestroom, Integer pageNumber, Integer pageSize) {
        def esQuery = prepareLocationSearch()
        esQuery = buildSearchRequest(esQuery, q, campus, type, lat, lon, searchDistance,
                                     isOpen, giRestroom, pageNumber, pageSize)
        LOGGER.debug("elastic search query: " + esQuery.toString())

        def resp = esQuery.get()
        // TODO: think about error conditions

        return resp.toString()
    }

    /**
     * Performs a search / list against the services index.
     *
     * @param q
     * @param isOpen
     * @param pageNumber
     * @param pageSize
     * @return
     */
    String searchService(String q, Boolean isOpen, Integer pageNumber,
                         Integer pageSize) {

        // generate ES query to search for locations
        // TODO: test
        def esQuery = prepareServiceSearch()
        esQuery = buildSearchRequest(esQuery, q, null, null,
                                     null, null, null,
                                     isOpen, null, pageNumber, pageSize)

        LOGGER.debug("elastic search query: " + esQuery.toString())

        def resp = esQuery.get()

        resp.toString()
    }

    /**
     * Returns the related services mapped to a building / location
     *
     * @param locationId
     * @param pageNumber
     * @param pageSize
     * @return
     */
    String getRelatedServices(String locationId, Integer pageNumber, Integer pageSize) {
        ObjectMapper mapper = new ObjectMapper()

        // generate ES query to search for locations
        def esQuery = getESQueryRelatedServices(locationId, pageNumber, pageSize)

        LOGGER.debug("elastic search query: " + esQuery)

        String esQueryJson = mapper.writeValueAsString(esQuery)

        // get data from ES
        def url = new URL("${servicesESFullUrl}/_search")
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
        GetResponse response = esClient.prepareGet(esIndex, esType, id.toLowerCase())
                .execute().actionGet()
        response.sourceAsString
    }

    /**
     * Returns a single service
     *
     * @param id
     * @return
     */
    String getServiceById(String id) {
        GetResponse response = esClient.prepareGet(esIndexService, esTypeService, id.toLowerCase())
                .execute().actionGet()
        response.sourceAsString
    }

    /**
     * Returns url of elastic search collection and type to search.
     *
     * @return
     */
    private GString getESFullUrl(String esIndex, String esType) {
        "${esUrl}/${esIndex}/${esType}"
    }

    private GString getServicesESFullUrl() {
        getESFullUrl(esIndexService, esTypeService)
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

    private SearchRequestBuilder prepareLocationSearch() {
        def req = esClient.prepareSearch(esIndex)
        req.setTypes(esType)
        req
    }

    private SearchRequestBuilder prepareServiceSearch() {
        def req = esClient.prepareSearch(esIndexService)
        req.setTypes(esTypeService)
        req
    }

    /**
     * Generate ElasticSearch query to list locations by campus, type and full text search.
     * @param q             search for locations with this name
     * @param campus        restrict results to this campus (corvallis, cascade)
     * @param type          restrict results to this type (building, dining, cultural-center...)
     * @param lat           latitute for geo search
     * @param lon           longitude for geo search
     * @param searchDistance    restrict results to be at most this far from (lat,lon)
     * @param isOpen        only include dining locations which are open at the time of the search
     * @param giRestroom    only include building with gender inclusive restrooms
     * @param pageNumber    page number (1..)
     * @param pageSize      page size
     * @return
     */
    @groovy.transform.TypeChecked
    @groovy.transform.PackageScope // for testing
    SearchRequestBuilder buildSearchRequest(SearchRequestBuilder req,
                                            String q, String campus, String type,
                                            Double lat, Double lon, String searchDistance,
                                            Boolean isOpen,
                                            Boolean giRestroom, int pageNumber, int pageSize) {

        req.setFrom((pageNumber - 1) * pageSize)
        req.setSize(pageSize)

        def query = QueryBuilders.boolQuery()

        if (campus) {
            query.must(QueryBuilders.matchQuery("attributes.campus", campus))
        }

        if (type) {
            if (type == "cultural-center") {
                query.must(QueryBuilders.matchQuery("attributes.tags", type))
            } else {
                query.must(QueryBuilders.matchQuery("attributes.type", type))
            }
        }

        if (q) {
            // TODO: should this also search bldgID?
            query.filter(QueryBuilders.multiMatchQuery(q, "attributes.name", "attributes.abbreviation"))
        }

        if (lat && lon) {
            query.filter(QueryBuilders.geoDistanceQuery("attributes.geoLocation")
                        .distance(searchDistance)
                        .point(lat, lon))

            req.addSort(SortBuilders.geoDistanceSort("attributes.geoLocation")
                        .point(lat, lon)
                        .order(SortOrder.ASC)
                        .unit(DistanceUnit.KILOMETERS)
                        .geoDistance(GeoDistance.PLANE))
        }

        if (isOpen) {
            // TODO: weekday should be a function argument
            String weekday = Integer.toString(DateTime.now().getDayOfWeek())
            String path = "attributes.openHours." + weekday

            query.filter(
                    QueryBuilders.nestedQuery(path, QueryBuilders.boolQuery()
                        .filter(QueryBuilders.rangeQuery(path + ".start").lte("now"))
                        .filter(QueryBuilders.rangeQuery(path + ".end").gt("now"))))
        }

        if (giRestroom) {
            query.must(QueryBuilders.rangeQuery("attributes.giRestroomCount").gt(0))
        }

        req.setQuery(query)
        return req
    }

    private static def getESQueryRelatedServices(String locationId, int pageNumber, int pageSize) {
        [
            "query": [
                "bool": [
                    "must": [ "match": [ "attributes.locationId": locationId ]]
                ]
            ],
            "sort": [],
            "from": (pageNumber - 1) * pageSize,
            "size": pageSize
        ]
    }
}
