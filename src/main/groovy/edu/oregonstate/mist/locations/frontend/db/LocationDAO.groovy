package edu.oregonstate.mist.locations.frontend.db

import groovy.transform.InheritConstructors
import groovy.transform.PackageScope
import groovy.transform.TypeChecked
import org.elasticsearch.action.get.GetResponse
import org.elasticsearch.action.search.SearchRequestBuilder
import org.elasticsearch.client.Client
import org.elasticsearch.common.geo.GeoDistance
import org.elasticsearch.common.unit.DistanceUnit
import org.elasticsearch.index.query.QueryBuilders
import org.elasticsearch.search.sort.SortBuilders
import org.elasticsearch.search.sort.SortOrder
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Handles HTTP requests against ElasticSearch. Operation supported are:
 * search, searchService, getRelatedService, getById, and getServiceById.
 */
@TypeChecked
@InheritConstructors
class LocationDAO {
    private static final Logger LOGGER = LoggerFactory.getLogger(LocationDAO.class)

    private final String esIndex
    private final String esType
    private final String esIndexService
    private final String esTypeService

    private final ElasticSearchManager esManager

    LocationDAO(Map<String, String> locationConfiguration, ElasticSearchManager esManager) {
        this.esManager = esManager
        esIndex = locationConfiguration.get("esIndex")
        esType = locationConfiguration.get("estype")
        esIndexService = locationConfiguration.get("esIndexService")
        esTypeService = locationConfiguration.get("estypeService")
    }

    private Client getEsClient() {
        esManager.getClient()
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
    String search(String q, String campus, String type,
                  Double lat, Double lon, String searchDistance,
                  Boolean isOpen, Integer weekday, Boolean giRestroom,
                  Integer pageNumber, Integer pageSize) {
        def esQuery = prepareLocationSearch()
        esQuery = buildSearchRequest(esQuery, q, campus, type, lat, lon, searchDistance,
                                     isOpen, weekday, giRestroom, pageNumber, pageSize)
        LOGGER.debug("elastic search query: " + esQuery.toString())

        def resp = esQuery.get()
        // TODO: think about error conditions

        resp.toString()
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
    String searchService(String q, Boolean isOpen, Integer weekday,
                         Integer pageNumber, Integer pageSize) {

        // generate ES query to search for locations
        // TODO: test
        def esQuery = prepareServiceSearch()
        esQuery = buildSearchRequest(esQuery, q, null, null,
                                     null, null, null,
                                     isOpen, weekday,
                                     null, pageNumber, pageSize)

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

        // generate ES query to search for locations
        def esQuery = prepareServiceSearch()
        esQuery = buildRelatedServicesRequest(esQuery, locationId, pageNumber, pageSize)

        LOGGER.debug("elastic search query: " + esQuery.toString())

        def resp = esQuery.get()

        resp.toString()
    }


    /**
     * Return a single location object with the matching id
     *
     * @param id
     * @return
     */
    String getById(String id) {
        GetResponse response = esClient.prepareGet(esIndex, esType, id.toLowerCase()).get()
        response.sourceAsString
    }

    /**
     * Returns a single service
     *
     * @param id
     * @return
     */
    String getServiceById(String id) {
        GetResponse response =
                esClient.prepareGet(esIndexService, esTypeService, id.toLowerCase()).get()
        response.sourceAsString
    }

    @PackageScope
    SearchRequestBuilder prepareLocationSearch() {
        def req = esClient.prepareSearch(esIndex)
        req.setTypes(esType)
        req
    }

    @PackageScope
    SearchRequestBuilder prepareServiceSearch() {
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
     * @param weekday       if isOpen is true, weekday gives the current day of the week
     *                      (monday=1, sunday=7)
     * @param giRestroom    only include building with gender inclusive restrooms
     * @param pageNumber    page number (1..)
     * @param pageSize      page size
     * @return
     */
    @TypeChecked
    @PackageScope // for testing
    static SearchRequestBuilder buildSearchRequest(
            SearchRequestBuilder req,
            String q, String campus, String type,
            Double lat, Double lon, String searchDistance,
            Boolean isOpen, Integer weekday,
            Boolean giRestroom, int pageNumber, int pageSize
    ) {
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
            String path = "attributes.openHours." + weekday.toString()

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

    @PackageScope
    static SearchRequestBuilder buildRelatedServicesRequest(
            SearchRequestBuilder req, String locationId,
            Integer pageNumber, Integer pageSize
    ) {
        req.setFrom((pageNumber - 1) * pageSize)
        req.setSize(pageSize)

        def query = QueryBuilders.boolQuery()
        query.must(QueryBuilders.matchQuery("attributes.locationId", locationId))
        // TODO: it's not clear that the outer bool query is actually necessary

        req.setQuery(query)

        req
    }
}
