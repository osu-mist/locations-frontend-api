package edu.oregonstate.mist.locations.frontend

import com.fasterxml.jackson.annotation.JsonProperty
import edu.oregonstate.mist.api.Configuration
import io.dropwizard.client.JerseyClientConfiguration

import javax.validation.Valid
import javax.validation.constraints.NotNull

class LocationsFrontendConfiguration extends Configuration {
    @JsonProperty('locations')
    @NotNull
    @Valid
    Map<String, String> locationsConfiguration

    @Valid
    @NotNull
    @JsonProperty("jerseyClient")
    JerseyClientConfiguration jerseyClientConfiguration = new JerseyClientConfiguration()

}
