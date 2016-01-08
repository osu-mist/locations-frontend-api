package edu.oregonstate.mist.locations.frontend.resources

import com.codahale.metrics.annotation.Timed
import com.fasterxml.jackson.databind.ObjectMapper
import edu.oregonstate.mist.api.AuthenticatedUser
import edu.oregonstate.mist.api.Resource
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

    @Context
    UriInfo uriInfo

    private final Map<String, String> locationConfiguration

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

    LocationResource(Map<String, String> locationConfiguration) {
        this.locationConfiguration = locationConfiguration
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Timed
    Response list(@QueryParam('q') String q, @QueryParam('campus') String campus, @QueryParam('type') String type,
                  @Auth AuthenticatedUser authenticatedUser) {
        def pageNumber = getPageNumber()
        def pageSize = getPageSize()
        def trimmedQ = sanitize(q?.trim())
        def trimmedCampus = sanitize(campus?.trim())
        def trimmedType = sanitize(type?.trim())

        // validate filtering parameters
        def invalidCampus = trimmedCampus && !ALLOWED_CAMPUSES.contains(trimmedCampus)
        def invalidType = trimmedType && !ALLOWED_TYPES.contains(trimmedType)
        if (invalidCampus || invalidType) {
            return notFound().build()
        }

        // generate ES query to search for locations
        def query = [ "match_all": [:] ]
        def esFullUrl = getESFullUrl()
        def esQuery = getESSearchQuery(trimmedCampus, trimmedType, trimmedQ, pageNumber, pageSize, query)
        ObjectMapper mapper = new ObjectMapper(); // can reuse, share globally
        String esQueryJson = mapper.writeValueAsString(esQuery)

        // get data from ES
        def url = new URL(esFullUrl + "/_search")
        URLConnection connection = postRequest(url, esQueryJson)

        ok(connection.content.text).build()
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Timed
    @Path('{id}')
    Response getById(@PathParam('id') String id, @Auth AuthenticatedUser authenticatedUser) {
        def esFullUrl = getESFullUrl()
        String esResponse = new URL(esFullUrl + "/${id}/_source").getText()

        if (!esResponse) {
            return notFound().build()
        }

        ok(esResponse).build()
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
     * @param trimmedCampus
     * @param trimmedType
     * @param trimmedQ
     * @param pageNumber
     * @param pageSize
     * @param query
     * @return
     */
    private def getESSearchQuery(String trimmedCampus, String trimmedType, String trimmedQ, int pageNumber,
                                 int pageSize, def query) {
        def esQuery = [
                "query": [
                    "filtered": [
                        "filter": [
                            "bool": [
                                "must": []
                            ]
                        ],
                        "query" : [:]
                    ]
                ],
                "from" : (pageNumber - 1) * pageSize,
                "size" : pageSize
        ]

        if (trimmedCampus) {
            esQuery.query.filtered.filter.bool.must += ["term": ["attributes.campus": trimmedCampus]]
        }

        if (trimmedType) {
            esQuery.query.filtered.filter.bool.must += ["term": ["attributes.type": trimmedType]]
        }

        if (trimmedQ) {
            query = ["query_string": ["query": trimmedQ]]
        }

        esQuery.query.filtered.query = query
        esQuery
    }

    /**
     * Sanitizes the search query string by replacing illegal characters with spaces.
     *
     * @param searchQuery
     * @return sanitized search query
     */
    private static String sanitize(String searchQuery) {
        if (!searchQuery) {
            return ""
        }

        illegalCharacterPattern.matcher(searchQuery).replaceAll(' ')
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

    private Integer getPageNumber() {
        def pageNumber = getArrayParameter("page", "number", uriInfo.getQueryParameters())
        if (!pageNumber || !pageNumber.isInteger()) {
            return DEFAULT_PAGE_NUMBER
        }

        pageNumber.toInteger()
    }

    /**
     * Returns the
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
