package edu.oregonstate.mist.locations.frontend.resources

import com.codahale.metrics.annotation.Timed
import edu.oregonstate.mist.api.AuthenticatedUser
import io.dropwizard.auth.Auth
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.WebApplicationException
import javax.ws.rs.client.Client
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response

@Path("/locations")
@Produces(MediaType.APPLICATION_JSON)
class LocationResource {
    private static final Logger LOGGER = LoggerFactory.getLogger(LocationResource.class)

    private final Map<String, String> locationConfiguration
    final Client client

    LocationResource(Map<String, String> locationConfiguration, Client client) {
        this.locationConfiguration = locationConfiguration
        this.client = client
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Timed
    @Path('{id: [0-9a-z]+}')
    Response getById(@PathParam('id') String id, @Auth AuthenticatedUser authenticatedUser) {
        def esUrl = locationConfiguration.get("esUrl")
        def esIndex = locationConfiguration.get("esIndex")
        def esType = locationConfiguration.get("estype")

        String esResponse = client.target("${esUrl}/${esIndex}/${esType}/${id}/_source")
                .request(MediaType.TEXT_PLAIN_TYPE)
                .get(String.class)

        if (!esResponse) {
            throw new WebApplicationException(Response.Status.NOT_FOUND)
        }

        Response.ok(esResponse).build()
    }

}
