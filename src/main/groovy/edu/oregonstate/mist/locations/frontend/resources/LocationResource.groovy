package edu.oregonstate.mist.locations.frontend.resources

import com.codahale.metrics.annotation.Timed
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import edu.oregonstate.mist.api.Resource
import edu.oregonstate.mist.api.jsonapi.GeoCooridinate
import edu.oregonstate.mist.api.jsonapi.GeoFeature
import edu.oregonstate.mist.api.jsonapi.GeoFeatureCollection
import edu.oregonstate.mist.api.jsonapi.Geometries
import edu.oregonstate.mist.api.jsonapi.ResourceObject
import edu.oregonstate.mist.locations.frontend.db.LocationDAO
import edu.oregonstate.mist.api.jsonapi.ResultObject
import edu.oregonstate.mist.locations.frontend.mapper.LocationMapper
import org.joda.time.DateTime
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import javax.annotation.security.PermitAll
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response
import java.util.regex.Pattern

@Path("locations")
@Produces(MediaType.APPLICATION_JSON)
@PermitAll
class LocationResource extends Resource {
    private static final Logger LOGGER = LoggerFactory.getLogger(LocationResource.class)

    public static final ArrayList<String> ALLOWED_CAMPUSES = ["corvallis", "extension",
                                                              "cascades", "hmsc", "other"]

    public static final ArrayList<String> ALLOWED_TYPES = ["building", "dining",
                                                           "cultural-center", "parking", "other"]

    public static final ArrayList<String> ALLOWED_UNITS = ["mi", "miles",
                                                           "yd", "yards",
                                                           "ft", "feet",
                                                           "in", "inch",
                                                           "km", "kilometers",
                                                           "m", "meters",
                                                           "cm", "centimeters",
                                                           "mm", "millimeters",
                                                           "NM", "nmi", "nauticalmiles"]

    private final LocationDAO locationDAO

    private static final Pattern illegalCharacterPattern = Pattern.compile(
            '''(?x)       # this extended regex defines
               (?!        # any character that is not
                  [
                   a-zA-Z # a letter,
                   0-9    # a number,
                   -      # a hyphen,
                   _      # an underscore,
                   \\.    # a period, or
                   @      # an at sign
                  ])
               .          # to be an illegal character.
            ''')

    LocationResource(LocationDAO locationDAO, URI endpointUri) {
        this.locationDAO = locationDAO
        this.endpointUri = endpointUri
    }

    @GET
    @Timed
    Response list(@QueryParam('q') String q,
                  @QueryParam('campus') String campus, @QueryParam('type') String type,
                  @QueryParam('lat') Double lat, @QueryParam('lon') Double lon,
                  @QueryParam('distance') Double distance,
                  @QueryParam('distanceUnit') String distanceUnit,
                  @QueryParam('isOpen') Boolean isOpen,
                  @QueryParam('giRestroom') Boolean giRestroom,
                  @QueryParam('parkingZoneGroup') List<String> parkingZoneGroup,
                  @QueryParam('geojson') Boolean geojson) {

        try {
            if (maxPageSizeExceeded()) {
                return pageSizeExceededError().build()
            }

            def trimmedQ = sanitize(q?.trim())
            def trimmedCampus = sanitize(campus?.trim()?.toLowerCase())
            def trimmedType = sanitize(type?.trim()?.toLowerCase())
            isOpen = isOpen == null ? false : isOpen
            giRestroom = giRestroom == null ? false : giRestroom

            String searchDistance = null
            if(lat && lon) {
                distance = getDistance(distance)
                distanceUnit = getDistanceUnit(distanceUnit)
                searchDistance = buildSearchDistance(distance, distanceUnit)
            }

            // validate filtering parameters
            if (validateParameters(trimmedCampus, trimmedType, lat, lon, distanceUnit)) {
                return notFound().build()
            }

            Integer weekday = DateTime.now().getDayOfWeek()
            String result = locationDAO.search(
                                trimmedQ, trimmedCampus, trimmedType,
                                lat, lon, searchDistance,
                                isOpen, weekday, giRestroom, parkingZoneGroup,
                                pageNumber, pageSize)

            ResultObject resultObject = new ResultObject()
            resultObject.data = []

            // parse ES into JSON Node
            ObjectMapper mapper = new ObjectMapper() // can reuse, share globally
            JsonNode actualObj = mapper.readTree(result)

            def topLevelHits = actualObj.get("hits")
            topLevelHits.get("hits").asList().each {
                resultObject.data += LocationMapper.map(it)
            }

            setPaginationLinks(topLevelHits, q, type, campus,
                    lat, lon, distance, distanceUnit,
                    isOpen, giRestroom, parkingZoneGroup, resultObject)

            if (geojson) {
                def geojsonResultObject = toGeoJson(resultObject)
                return ok(geojsonResultObject).build()
            }

            ok(resultObject).build()
        } catch (Exception e) {
            LOGGER.error("Exception while getting locations.", e)
            internalServerError("Woot you found a bug for us to fix!").build()
        }

    }

    private static Double getDistance(Double distance) {
        distance ?: 2
    }

    /**
     * Returns the distanceUnit if not null. Otherwise, provides the default value.
     *
     * @param distanceUnit
     * @return
     */
    private static String getDistanceUnit(String distanceUnit) {
        distanceUnit?.trim()?.toLowerCase() ?: "miles"
    }

    /**
     * Validates search parameters.
     *
     * @param trimmedCampus
     * @param trimmedType
     * @param lat
     * @param lon
     * @param trimmedUnit
     * @return
     */
    private static boolean validateParameters(String trimmedCampus, String trimmedType, Double lat,
                                       Double lon, String trimmedUnit) {
        def invalidCampus = trimmedCampus && !ALLOWED_CAMPUSES.contains(trimmedCampus)
        def invalidType = trimmedType && !ALLOWED_TYPES.contains(trimmedType)
        def invalidLocation = (lat == null && lon != null) || (lat != null && lon == null)
        def invalidUnit = trimmedUnit && !ALLOWED_UNITS.contains(trimmedUnit)
        invalidCampus || invalidType || invalidLocation || invalidUnit
    }

    /**
     * Add pagination links to the data search results.
     *
     * @param topLevelHits First "hits" node in the json document
     * @param q
     * @param type
     * @param campus
     * @param resultObject
     */
    private void setPaginationLinks(JsonNode topLevelHits, String q, String type, String campus,
                                    Double lat, Double lon, Double distance, String distanceUnit,
                                    Boolean isOpen, Boolean giRestroom,
                                    List<String> parkingZoneGroup, ResultObject resultObject) {

        def totalHits = topLevelHits.get("total").asInt()
        // If no results were found, no need to add links
        if (!totalHits) {
            return
        }

        String baseResource = uriInfo.getMatchedURIs().get(uriInfo.getMatchedURIs().size() - 1)
        Integer pageNumber = getPageNumber()
        Integer pageSize = getPageSize()
        def urlParams = [
                "q"                 : q,
                "type"              : type,
                "campus"            : campus,
                "lat"               : lat,
                "lon"               : lon,
                "distance"          : distance,
                "distanceUnit"      : distanceUnit,
                "isOpen"            : isOpen,
                "giRestroom"        : giRestroom,
                "parkingZoneGroup"  : parkingZoneGroup,
                "pageSize"          : pageSize,
                "pageNumber"        : pageNumber
        ]

        int lastPage = Math.ceil(totalHits / pageSize)
        resultObject.links["self"] = getPaginationUrl(urlParams, baseResource)
        urlParams.pageNumber = 1
        resultObject.links["first"] = getPaginationUrl(urlParams, baseResource)
        urlParams.pageNumber = lastPage
        resultObject.links["last"] = getPaginationUrl(urlParams, baseResource)

        if (pageNumber > DEFAULT_PAGE_NUMBER) {
            urlParams.pageNumber = pageNumber - 1
            resultObject.links["prev"] = getPaginationUrl(urlParams, baseResource)
        } else {
            resultObject.links["prev"] = null
        }

        if (totalHits > (pageNumber * pageSize)) {
            urlParams.pageNumber = pageNumber + 1
            resultObject.links["next"] = getPaginationUrl(urlParams, baseResource)
        } else {
            resultObject.links["next"] = null
        }
    }

    @GET
    @Timed
    @Path('{id: [0-9a-zA-Z]+}')
    Response getById(@PathParam('id') String id,
                     @QueryParam('geojson') Boolean geojson) {
        try {
            String esResponse = locationDAO.getById(id)
            if (!esResponse) {
                return notFound().build()
            }

            ResultObject resultObject = new ResultObject()
            resultObject.data = LocationMapper.map(esResponse)

            if (geojson) {
                def geojsonResultObject = toGeoJson(resultObject)
                return ok(geojsonResultObject).build()
            }

            ok(resultObject).build()
        } catch (Exception e) {
            LOGGER.error("Exception while getting location by ID", e)
            internalServerError("Woot you found a bug for us to fix!").build()
        }

    }

    @GET
    @Timed
    @Path('{id: [0-9a-zA-Z]+}/services')
    Response getRelatedServices(@PathParam('id') String id) {
        try {
            String result = locationDAO.getRelatedServices(id, pageNumber, pageSize)
            if (!result) {
                return notFound().build()
            }

            ResultObject resultObject = new ResultObject()
            resultObject.data = []

            // parse ES into JSON Node
            ObjectMapper mapper = new ObjectMapper() // can reuse, share globally
            JsonNode actualObj = mapper.readTree(result)

            def topLevelHits = actualObj.get("hits")
            topLevelHits.get("hits").asList().each {
                resultObject.data += LocationMapper.map(it)
            }

            //@todo: in the future we may add pagination. For now, let's keep it simple

            ok(resultObject).build()
        } catch (Exception e) {
            LOGGER.error("Exception while getting location by ID", e)
            internalServerError("Woot you found a bug for us to fix!").build()
        }

    }

    /**
     * Turn resultObject to GeoJSON format.
     *
     * @param resultObject
     * @return
     */
    private static toGeoJson(ResultObject resultObject) {
        def geojsonResultObject
        def ro = resultObject?.data

        if (ro instanceof List) {
            geojsonResultObject = new GeoFeatureCollection(type: "FeatureCollection")
            ro.each {
                geojsonResultObject?.features += adjustGeoFeature(it)
            }
        } else {
            geojsonResultObject = adjustGeoFeature(ro)
        }

        geojsonResultObject
    }

    /**
     * Adjust single resultObject to a Feature object.
     *
     * @param ro
     * @return
     */
    private static GeoFeature adjustGeoFeature(ResourceObject ro) {
        def geojsonResultObject = new GeoFeature(type: "Feature")

        def geoPolygon = null
        def geoPoint = null
        def geometry

        // adjust geoPolygon
        if (ro?.attributes?.geometry?.type && ro?.attributes?.geometry?.coordinates) {
            geoPolygon = ro?.attributes?.geometry
        }

        // adjust geoPoint
        if (ro?.attributes?.longitude && ro?.attributes?.latitude) {
            geoPoint = new GeoCooridinate(
                type: "Point",
                coordinates: [
                    ro?.attributes?.longitude?.toFloat(),
                    ro?.attributes?.latitude?.toFloat()
                ]
            )
        }

        if (geoPolygon && geoPoint) {
            geometry = new Geometries()
            geometry?.type = "GeometryCollection"
            geometry?.geometries = [geoPolygon, geoPoint]
        } else if (!(geoPolygon || geoPoint)) {
            geometry = null
        } else {
            def coordinates = [geoPolygon, geoPoint].find { it }
            geometry = new GeoCooridinate()
            geometry?.type = coordinates?.type
            geometry?.coordinates = coordinates?.coordinates
        }

        ["geometry", "longitude", "latitude"].each {
            ro?.attributes?.remove(it)
        }

        geojsonResultObject?.geometry = geometry
        geojsonResultObject?.properties = ro

        geojsonResultObject
    }

    /**
     * Sanitizes the search query string by replacing illegal characters with spaces.
     *
     * @param searchQuery
     * @return sanitized search query
     */
    private static String sanitize(String searchQuery) {
        if (!searchQuery) {
            return null
        }

        illegalCharacterPattern?.matcher(searchQuery)?.replaceAll(' ')
    }

    public static String buildSearchDistance(Double distance, String distanceUnit) {
        distance?.toString()?.concat(distanceUnit)
    }
}
