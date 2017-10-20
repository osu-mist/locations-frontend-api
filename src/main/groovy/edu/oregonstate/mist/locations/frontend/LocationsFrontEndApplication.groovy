package edu.oregonstate.mist.locations.frontend

import edu.oregonstate.mist.locations.frontend.db.LocationDAO
import edu.oregonstate.mist.locations.frontend.health.ElasticSearchHealthCheck
import edu.oregonstate.mist.locations.frontend.db.ElasticSearchManager
import edu.oregonstate.mist.locations.frontend.resources.LocationResource
import edu.oregonstate.mist.locations.frontend.resources.ServiceResource
import edu.oregonstate.mist.api.Application
import groovy.transform.TypeChecked
import io.dropwizard.setup.Environment

/**
 * Main application class.
 */
@TypeChecked
class LocationsFrontEndApplication extends Application<LocationsFrontendConfiguration> {
    /**
     * Parses command-line arguments and runs the application.
     *
     * @param configuration
     * @param environment
     */
    @Override
    public void run(LocationsFrontendConfiguration configuration, Environment environment) {
        this.setup(configuration, environment)

        String esUrl = configuration.locationsConfiguration.get("esUrl")
        def esManager = new ElasticSearchManager(esUrl)
        environment.lifecycle().manage(esManager)

        LocationDAO locationDAO = new LocationDAO(configuration.locationsConfiguration, esManager)

        def endpointUri = configuration.api.endpointUri
        environment.jersey().register(new LocationResource(locationDAO, endpointUri))
        environment.jersey().register(new ServiceResource(locationDAO, endpointUri))

        ElasticSearchHealthCheck healthCheck =
                new ElasticSearchHealthCheck(esManager.client, configuration.locationsConfiguration)
        environment.healthChecks().register("elasticSearchCluster", healthCheck)
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
