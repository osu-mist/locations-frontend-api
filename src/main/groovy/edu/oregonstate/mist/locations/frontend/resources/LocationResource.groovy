package edu.oregonstate.mist.locations.frontend.resources

import com.codahale.metrics.annotation.Timed
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import edu.oregonstate.mist.api.AuthenticatedUser
import edu.oregonstate.mist.api.Resource
import edu.oregonstate.mist.locations.frontend.db.LocationDAO
import edu.oregonstate.mist.locations.frontend.jsonapi.ResourceObject
import edu.oregonstate.mist.locations.frontend.jsonapi.ResultObject
import io.dropwizard.auth.Auth
import org.slf4j.Logger
import org.slf4j.LoggerFactory

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

@Path("/locations")
@Produces(MediaType.APPLICATION_JSON)
class LocationResource extends Resource {
    private static final Logger LOGGER = LoggerFactory.getLogger(LocationResource.class)

    public static final ArrayList<String> ALLOWED_CAMPUSES = ["corvallis"]
    public static final ArrayList<String> ALLOWED_TYPES = ["building", "dining"]

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

    @Context
    UriInfo uriInfo

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

    LocationResource(LocationDAO locationDAO) {
        this.locationDAO = locationDAO
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Timed
    Response list(@QueryParam('q') String q, @QueryParam('campus') String campus, @QueryParam('type') String type,
                  @Auth AuthenticatedUser authenticatedUser) {
        def trimmedQ = sanitize(q?.trim())
        def trimmedCampus = sanitize(campus?.trim())
        def trimmedType = sanitize(type?.trim())

        // validate filtering parameters
        def invalidCampus = trimmedCampus && !ALLOWED_CAMPUSES.contains(trimmedCampus)
        def invalidType = trimmedType && !ALLOWED_TYPES.contains(trimmedType)
        if (invalidCampus || invalidType) {
            return notFound().build()
        }

        String result = locationDAO.search(trimmedQ, trimmedCampus, trimmedType, pageNumber, pageSize)

        ResultObject resultObject = new ResultObject()
        resultObject.data = []

        // parse ES into JSON Node
        ObjectMapper mapper = new ObjectMapper() // can reuse, share globally
        JsonNode actualObj = mapper.readTree(result)

        def topLevelHits = actualObj.get("hits")
        topLevelHits.get("hits").asList().each {
            def source = it.get("_source")
            ResourceObject resourceObject = new ResourceObject(
                    id: source.get("id").asText(),
                    type: source.get("type").asText(),
                    attributes: source.get("attributes")
            )
            resultObject.data += resourceObject
        }

        setPaginationLinks(topLevelHits, q, type, campus, resultObject)

        ok(resultObject).build()
    }

    /**
     * Add pagination links to the data search results.
     *
     * @param topLevelHits          First "hits" node in the json document
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

        Integer pageNumber = getPageNumber()
        Integer pageSize = getPageSize()
        def map = ["q": q, "type": type, "campus": campus, "pageSize": pageSize, "pageNumber": pageNumber]
        int lastPage = Math.ceil(totalHits / pageSize)

        resultObject.links["self"] = getPaginationUrl(map)
        map.pageNumber = 1
        resultObject.links["first"] = getPaginationUrl(map)
        map.pageNumber = lastPage
        resultObject.links["last"] = getPaginationUrl(map)

        if (pageNumber > DEFAULT_PAGE_NUMBER) {
            map.pageNumber = pageNumber - 1
            resultObject.links["prev"] = getPaginationUrl(map)
        } else {
            resultObject.links["prev"] = null
        }

        if (totalHits > (pageNumber * pageSize)) {
            map.pageNumber = pageNumber + 1
            resultObject.links["next"] = getPaginationUrl(map)
        } else {
            resultObject.links["next"] = null
        }
    }

    /**
     * Returns string url to use in pagination links.
     *
     * @param params
     * @return
     */
    private String getPaginationUrl(def params) {
        def uriAndPath = uriInfo.getBaseUri().toString() + uriInfo.getPath()
        def nonNullParams = params.clone()
        // convert pageVariable to page[variable]
        nonNullParams["page[number]"] = nonNullParams['pageNumber']
        nonNullParams["page[size]"] = nonNullParams['pageSize']
        nonNullParams.remove('pageSize')
        nonNullParams.remove('pageNumber')

        // remove empty GET parameters
        nonNullParams = nonNullParams.findAll { it.value } .collect { k, v -> "$k=$v" }

        uriAndPath + "?" + nonNullParams.join('&')
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Timed
    @Path('{id: [0-9a-z]+}')
    Response getById(@PathParam('id') String id, @Auth AuthenticatedUser authenticatedUser) {
        String esResponse  = locationDAO.getById(id)

        if (!esResponse) {
            return notFound().build()
        }

        ok(esResponse).build()
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
     * The JSONAPI format reserves the page parameter for pagination. This API uses page[size] and page[number].
     * This function allows us to get just value for a specific parameter in an array.
     *
     * @param key
     * @param index
     * @param queryParameters
     * @return
     */
    public static String getArrayParameter(String key, String index,  MultivaluedMap<String, String> queryParameters) {
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

    /**
     *  Returns the page number used by pagination. The value of: page[number] in the url.
     *
     * @return
     */
    private Integer getPageNumber() {
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
    private Integer getPageSize() {
        def pageSize = getArrayParameter("page", "size", uriInfo.getQueryParameters())
        if (!pageSize || !pageSize.isInteger()) {
            return DEFAULT_PAGE_SIZE
        }

        pageSize.toInteger()
    }
}
