package edu.oregonstate.mist.locations.frontend.resources

import com.codahale.metrics.annotation.Timed
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import edu.oregonstate.mist.api.Resource
import edu.oregonstate.mist.locations.frontend.db.LocationDAO
import edu.oregonstate.mist.api.jsonapi.ResultObject
import edu.oregonstate.mist.locations.frontend.mapper.LocationMapper
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import javax.annotation.security.PermitAll
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.Context
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.MultivaluedMap
import javax.ws.rs.core.Response
import javax.ws.rs.core.UriInfo
import java.util.regex.Pattern

@Path("locations")
@Produces(MediaType.APPLICATION_JSON)
@PermitAll
class LocationResource extends Resource {
    private static final Logger LOGGER = LoggerFactory.getLogger(LocationResource.class)

    public static final ArrayList<String> ALLOWED_CAMPUSES = ["corvallis", "extension"]
    public static final ArrayList<String> ALLOWED_TYPES = ["building", "dining", "cultural-centers"]
    public static final ArrayList<String> ALLOWED_UNITS = ["mi", "miles",
                                                           "yd", "yards",
                                                           "ft", "feet",
                                                           "in", "inch",
                                                           "km", "kilometers",
                                                           "m", "meters",
                                                           "cm", "centimeters",
                                                           "mm", "millimeters",
                                                           "NM", "nmi", "nauticalmiles"]

    /**
     * Default page number used in pagination
     * @todo: should this be coming from skeleton or configuration file?
     */
    public static final Integer DEFAULT_PAGE_NUMBER = 1

    /**
     * Default page size used in pagination
     * @todo: should this be coming from skeleton or configuration file?
     */
    public static final Integer DEFAULT_PAGE_SIZE = 10

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

    @Context
    UriInfo uriInfo

    @GET
    @Timed
    Response list(@QueryParam('q') String q,
                  @QueryParam('campus') String campus, @QueryParam('type') String type,
                  @QueryParam('lat') Double lat, @QueryParam('lon') Double lon,
                  @QueryParam('distance') Double distance,
                  @QueryParam('distanceUnit') String distanceUnit,
                  @QueryParam('isOpen') Boolean isOpen) {

        try {
            def trimmedQ = sanitize(q?.trim())
            def trimmedCampus = sanitize(campus?.trim()?.toLowerCase())
            def trimmedType = sanitize(type?.trim()?.toLowerCase())
            isOpen = isOpen == null ? false : isOpen
            distance = getDistance(distance)
            distanceUnit = getDistanceUnit(distanceUnit)

            // validate filtering parameters
            if (validateParameters(trimmedCampus, trimmedType, lat, lon, distanceUnit)) {
                return notFound().build()
            }

            String searchDistance = buildSearchDistance(distance, distanceUnit)
            String result = locationDAO.search(
                                trimmedQ, trimmedCampus, trimmedType,
                                lat, lon, searchDistance,
                                isOpen, pageNumber, pageSize)

            ResultObject resultObject = new ResultObject()
            resultObject.data = []

            // parse ES into JSON Node
            ObjectMapper mapper = new ObjectMapper() // can reuse, share globally
            JsonNode actualObj = mapper.readTree(result)

            def topLevelHits = actualObj.get("hits")
            topLevelHits.get("hits").asList().each {
                resultObject.data += LocationMapper.map(it)
            }

            setPaginationLinks(topLevelHits, q, type, campus, resultObject)

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
                                    ResultObject resultObject) {
        def totalHits = topLevelHits.get("total").asInt()
        // If no results were found, no need to add links
        if (!totalHits) {
            return
        }
        
        String baseResource = uriInfo.getMatchedURIs().get(uriInfo.getMatchedURIs().size() - 1)
        Integer pageNumber = getPageNumber()
        Integer pageSize = getPageSize()
        def urlParams = [
                "q"         : q,
                "type"      : type,
                "campus"    : campus,
                "pageSize"  : pageSize,
                "pageNumber": pageNumber
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
    Response getById(@PathParam('id') String id) {
        try {
            ResultObject resultObject = new ResultObject()
            String esResponse = locationDAO.getById(id)

            if (!esResponse) {
                return notFound().build()
            }

            ObjectMapper mapper = new ObjectMapper(); // can reuse, share globally
            resultObject.data = mapper.readValue(esResponse, Object.class)
            ok(resultObject).build()
        } catch (Exception e) {
            LOGGER.error("Exception while getting location by ID", e)
            internalServerError("Woot you found a bug for us to fix!").build()
        }

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

    /**
     * Returns the value for an array parameter in the GET string.
     *
     * The JSONAPI format reserves the page parameter for pagination.
     * This API uses page[size] and page[number].
     * This function allows us to get just value for a specific parameter in an array.
     *
     * @param key
     * @param index
     * @param queryParameters
     * @return
     */
    public static String getArrayParameter(String key, String index,
                                           MultivaluedMap<String, String> queryParameters) {
        // @todo: this function should probably be moved to the skeleton
        for (Map.Entry<String, List<String>> entry : queryParameters.entrySet()) {
            // not an array parameter
            if (!entry.key.contains("[") && !entry.key.contains("]")) {
                continue
            }

            int a = entry.key.indexOf('[')
            int b = entry.key.indexOf(']')

            if (entry.key.substring(0, a).equals(key)) {
                if (entry.key.substring(a + 1, b).equals(index)) {
                    return entry.value?.get(0)
                }
            }
        }

        null
    }

    public static String buildSearchDistance(Double distance, String distanceUnit) {
        distance.toString().concat(distanceUnit)
    }

    /**
     *  Returns the page number used by pagination. The value of: page[number] in the url.
     *
     * @return
     */
    Integer getPageNumber() {
        def pageNumber = getArrayParameter("page", "number", uriInfo.getQueryParameters())
        if (!pageNumber || !pageNumber.isInteger()) {
            return DEFAULT_PAGE_NUMBER
        }

        pageNumber.toInteger()
    }

    /**
     * Returns the page size used by pagination. The value of: page[size] in the url.
     *
     * @return
     */
    Integer getPageSize() {
        def pageSize = getArrayParameter("page", "size", uriInfo.getQueryParameters())
        if (!pageSize || !pageSize.isInteger()) {
            return DEFAULT_PAGE_SIZE
        }

        pageSize.toInteger()
    }
}
