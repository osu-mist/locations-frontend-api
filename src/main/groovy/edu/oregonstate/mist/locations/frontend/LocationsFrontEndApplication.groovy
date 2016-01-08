package edu.oregonstate.mist.locations.frontend

import edu.oregonstate.mist.api.Configuration
import edu.oregonstate.mist.api.Resource
import edu.oregonstate.mist.api.InfoResource
import edu.oregonstate.mist.api.AuthenticatedUser
import edu.oregonstate.mist.api.BasicAuthenticator
import edu.oregonstate.mist.locations.frontend.db.LocationDAO
import edu.oregonstate.mist.locations.frontend.resources.LocationResource
import edu.oregonstate.mist.locations.frontend.resources.SampleResource
import io.dropwizard.Application
import io.dropwizard.setup.Bootstrap
import io.dropwizard.setup.Environment
import io.dropwizard.auth.AuthFactory
import io.dropwizard.auth.basic.BasicAuthFactory

import javax.ws.rs.client.Client

/**
 * Main application class.
 */
class LocationsFrontEndApplication extends Application<LocationConfiguration> {
    /**
     * Initializes application bootstrap.
     *
     * @param bootstrap
     */
    @Override
    public void initialize(Bootstrap<Configuration> bootstrap) {}

    /**
     * Parses command-line arguments and runs the application.
     *
     * @param configuration
     * @param environment
     */
    @Override
    public void run(LocationConfiguration configuration, Environment environment) {
        Resource.loadProperties('resource.properties')
        final LocationDAO locationDAO = new LocationDAO(configuration.locationsConfiguration)

        environment.jersey().register(new SampleResource())
        environment.jersey().register(new InfoResource())
        environment.jersey().register(new LocationResource(locationDAO))

        environment.jersey().register(
                AuthFactory.binder(
                        new BasicAuthFactory<AuthenticatedUser>(
                                new BasicAuthenticator(configuration.getCredentialsList()),
                                'LocationsFrontEndApplication',
                                AuthenticatedUser.class)))
    }

    /**
     * Instantiates the application class with command-line arguments.
     *
     * @param arguments
     * @throws Exception
     */
    public static void main(String[] arguments) throws Exception {
        new LocationsFrontEndApplication().run(arguments)
    }
}
