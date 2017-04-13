package edu.oregonstate.mist.locations.frontend.mapper

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import edu.oregonstate.mist.api.jsonapi.ResourceObject

class LocationMapper {

    public static ResourceObject map(JsonNode hit) {
        def hitSource = hit.get("_source").toString()
        buildResourceObject(hitSource, hit)
    }

    public static map(String hitSource) {
        buildResourceObject(hitSource)
    }

    private static buildResourceObject(String hitSource, JsonNode hit = null) {
        ResourceObject ro = new ObjectMapper().readValue(hitSource, ResourceObject.class)
        adjustLocationsResource(ro, hit)

        ro
    }

    /**
     * Modify the json object from ElasticSearch to the API specification.
     *
     * @param attr
     * @param sort
     * @return
     */
    private static void adjustLocationsResource(ResourceObject ro, JsonNode hit) {
        // setup the individual latitude, longitude and remove ES geoLocation object
        ro?.attributes?.latitude = ro?.attributes?.geoLocation?.lat
        ro?.attributes?.longitude = ro?.attributes?.geoLocation?.lon

        // add the sort ES metadata to attributes
        ro?.attributes?.distance = hit?.get('sort')?.get(0)?.asDouble()

        // remove attributes not part of the api spec
        ro?.attributes?.remove("geoLocation")
        ro?.attributes?.remove("parent")
    }
}
