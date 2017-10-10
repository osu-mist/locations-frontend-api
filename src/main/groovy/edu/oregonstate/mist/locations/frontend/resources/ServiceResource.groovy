package edu.oregonstate.mist.locations.frontend.resources

import com.codahale.metrics.annotation.Timed
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import edu.oregonstate.mist.api.Resource
import edu.oregonstate.mist.api.jsonapi.ResultObject
import edu.oregonstate.mist.locations.frontend.db.LocationDAO
import edu.oregonstate.mist.locations.frontend.mapper.LocationMapper
import org.joda.time.DateTime
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import javax.annotation.security.PermitAll
import javax.ws.rs.*
import javax.ws.rs.core.*
import java.util.regex.Pattern

@Path("services")
@Produces(MediaType.APPLICATION_JSON)
@PermitAll
class ServiceResource extends Resource {
    private static final Logger LOGGER = LoggerFactory.getLogger(ServiceResource.class)

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

    ServiceResource(LocationDAO locationDAO, URI endpointUri) {
        this.locationDAO = locationDAO
        this.endpointUri = endpointUri
    }

    @Context
    UriInfo uriInfo

    @GET
    @Timed
    Response list(@QueryParam('q') String q,
                  @QueryParam('isOpen') Boolean isOpen) {

        try {
            def trimmedQ = sanitize(q?.trim())
            isOpen = isOpen == null ? false : isOpen

            Integer weekday = DateTime.now().getDayOfWeek()
            String result = locationDAO.searchService(trimmedQ, isOpen, weekday, pageNumber, pageSize)

            ResultObject resultObject = new ResultObject()
            resultObject.data = []

            // parse ES into JSON Node
            ObjectMapper mapper = new ObjectMapper() // can reuse, share globally
            JsonNode actualObj = mapper.readTree(result)

            def topLevelHits = actualObj.get("hits")
            topLevelHits.get("hits").asList().each {
                resultObject.data += LocationMapper.map(it)
            }

            setPaginationLinks(topLevelHits, q, null, resultObject)

            ok(resultObject).build()
        } catch (Exception e) {
            LOGGER.error("Exception while getting locations.", e)
            internalServerError("Woot you found a bug for us to fix!").build()
        }

    }

    /**
     * Add pagination links to the data search results.
     *
     * @param topLevelHits First "hits" node in the json document
     * @param q
     * @param type
     * @param resultObject
     */
    //@todo: reuse
    private void setPaginationLinks(JsonNode topLevelHits, String q, String type,
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
        //@todo: very similar logic???
        try {
            String esResponse = locationDAO.getServiceById(id)
            if (!esResponse) {
                return notFound().build()
            }

            ResultObject resultObject = new ResultObject()
            resultObject.data = LocationMapper.map(esResponse)

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
        //@todo:not dry
        if (!searchQuery) {
            return null
        }

        illegalCharacterPattern?.matcher(searchQuery)?.replaceAll(' ')
    }
}
