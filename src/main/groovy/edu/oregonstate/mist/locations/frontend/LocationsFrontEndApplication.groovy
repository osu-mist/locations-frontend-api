package edu.oregonstate.mist.locations.frontend

import de.thomaskrille.dropwizard_template_config.TemplateConfigBundle
import edu.oregonstate.mist.api.BuildInfoManager
import edu.oregonstate.mist.api.Configuration
import edu.oregonstate.mist.api.Resource
import edu.oregonstate.mist.api.InfoResource
import edu.oregonstate.mist.api.AuthenticatedUser
import edu.oregonstate.mist.api.BasicAuthenticator
import edu.oregonstate.mist.locations.frontend.db.LocationDAO
import edu.oregonstate.mist.locations.frontend.health.ElasticSearchHealthCheck
import edu.oregonstate.mist.locations.frontend.resources.LocationResource
import edu.oregonstate.mist.api.PrettyPrintResponseFilter
import edu.oregonstate.mist.api.jsonapi.GenericExceptionMapper
import edu.oregonstate.mist.api.jsonapi.NotFoundExceptionMapper
import edu.oregonstate.mist.locations.frontend.resources.ServiceResource
import io.dropwizard.Application
import io.dropwizard.auth.AuthDynamicFeature
import io.dropwizard.auth.AuthValueFactoryProvider
import io.dropwizard.auth.basic.BasicCredentialAuthFilter
import io.dropwizard.jersey.errors.LoggingExceptionMapper
import io.dropwizard.setup.Bootstrap
import io.dropwizard.setup.Environment
import javax.ws.rs.WebApplicationException

/**
 * Main application class.
 */
class LocationsFrontEndApplication extends Application<LocationsFrontendConfiguration> {
    /**
     * Initializes application bootstrap.
     *
     * @param bootstrap
     */
    @Override
    public void initialize(Bootstrap<Configuration> bootstrap) {
        bootstrap.addBundle(new TemplateConfigBundle())
    }

    /**
     * Registers lifecycle managers and Jersey exception mappers
     * and container response filters
     *
     * @param environment
     * @param buildInfoManager
     */
    protected void registerAppManagerLogic(Environment environment,
                                           BuildInfoManager buildInfoManager) {

        environment.lifecycle().manage(buildInfoManager)

        environment.jersey().register(new NotFoundExceptionMapper())
        environment.jersey().register(new GenericExceptionMapper())
        environment.jersey().register(new LoggingExceptionMapper<WebApplicationException>(){})
        environment.jersey().register(new PrettyPrintResponseFilter())
    }

    /**
     * Parses command-line arguments and runs the application.
     *
     * @param configuration
     * @param environment
     */
    @Override
    public void run(LocationsFrontendConfiguration configuration, Environment environment) {
        Resource.loadProperties()
        BuildInfoManager buildInfoManager = new BuildInfoManager()
        environment.jersey().register(new InfoResource(buildInfoManager.getInfo()))

        LocationDAO locationDAO = new LocationDAO(configuration.locationsConfiguration)

        environment.jersey().register(new LocationResource(
                locationDAO, configuration.api.endpointUri))
        environment.jersey().register(new ServiceResource(
                locationDAO, configuration.api.endpointUri))
        ElasticSearchHealthCheck healthCheck =
                new ElasticSearchHealthCheck(configuration.locationsConfiguration)
        environment.healthChecks().register("elasticSearchCluster", healthCheck)

        registerAppManagerLogic(environment, buildInfoManager)

        environment.jersey().register(new AuthDynamicFeature(
                new BasicCredentialAuthFilter.Builder<AuthenticatedUser>()
                    .setAuthenticator(new BasicAuthenticator(configuration.getCredentialsList()))
                    .setRealm('LocationFrontEndApplication')
                    .buildAuthFilter()
        ))
        environment.jersey().register(new AuthValueFactoryProvider.Binder
                <AuthenticatedUser>(AuthenticatedUser.class))
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
