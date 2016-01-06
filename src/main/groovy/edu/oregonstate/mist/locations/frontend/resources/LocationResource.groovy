package edu.oregonstate.mist.locations.frontend.resources

import com.codahale.metrics.annotation.Timed
import com.fasterxml.jackson.databind.ObjectMapper
import edu.oregonstate.mist.api.AuthenticatedUser
import io.dropwizard.auth.Auth
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.WebApplicationException
import javax.ws.rs.client.Client
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response

@Path("/locations")
@Produces(MediaType.APPLICATION_JSON)
class LocationResource {
    private static final Logger LOGGER = LoggerFactory.getLogger(LocationResource.class)

    private final Map<String, String> locationConfiguration

    LocationResource(Map<String, String> locationConfiguration) {
        this.locationConfiguration = locationConfiguration
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Timed
    Response list(@QueryParam('q') String q, @QueryParam('campus') String campus, @QueryParam('type') String type,
                  @Auth AuthenticatedUser authenticatedUser) {
        //@todo: sanitize input
        //@todo: validate campus & type
        //@todo: pagination
        def trimmedQ = q?.trim()
        def trimmedCampus = campus?.trim()
        def trimmedType = type?.trim()
        def query = [ "match_all": [:] ]
        def esFullUrl = getESFullUrl()

        def esQuery = getESSearchQuery(trimmedCampus, trimmedType, trimmedQ, query)
        ObjectMapper mapper = new ObjectMapper(); // can reuse, share globally
        String esQueryJson = mapper.writeValueAsString(esQuery)

        def url = new URL(esFullUrl + "/_search")
        URLConnection connection = postRequest(url, esQueryJson)

        def esReponse = connection.content.text

        Response.ok(esReponse).build()
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Timed
    @Path('{id}')
    Response getById(@PathParam('id') String id, @Auth AuthenticatedUser authenticatedUser) {
        def esFullUrl = getESFullUrl()
        String esResponse = new URL(esFullUrl + "/${id}/_source").getText()

        if (!esResponse) {
            throw new WebApplicationException(Response.Status.NOT_FOUND)
        }

        Response.ok(esResponse).build()
    }

    /**
     * Returns url of elastic search collection and type to search.
     *
     * @return
     */
    private GString getESFullUrl() {
        def esUrl = locationConfiguration.get("esUrl")
        def esIndex = locationConfiguration.get("esIndex")
        def esType = locationConfiguration.get("estype")
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
     * @param query
     * @return
     */
    private def getESSearchQuery(String trimmedCampus, String trimmedType, String trimmedQ, def query) {
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
                "from" : 0,
                "size" : 10
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
}
