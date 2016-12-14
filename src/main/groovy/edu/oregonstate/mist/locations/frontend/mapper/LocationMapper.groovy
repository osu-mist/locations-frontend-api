package edu.oregonstate.mist.locations.frontend.mapper

import com.fasterxml.jackson.databind.JsonNode
import edu.oregonstate.mist.locations.frontend.core.Attributes
import edu.oregonstate.mist.locations.frontend.core.DayOpenHours
import edu.oregonstate.mist.locations.frontend.jsonapi.ResourceObject
import org.joda.time.DateTime

class LocationMapper {

    public ResourceObject map(JsonNode hit) {
        def source = hit.get("_source")
        def resourceObject = new ResourceObject(
                id:         getField(source, "id"),
                type:       getField(source, "type"),
                attributes: getAttributes(source.get("attributes"), hit.get('sort')),
                links:      source.get("links")
        )
        resourceObject
    }

    /**
     * Get attributes from JsonNode
     * @param attr
     * @param sort
     * @return
     */
    private static Attributes getAttributes(JsonNode attr, JsonNode sort) {
        Attributes attributes = new Attributes(
                name:           getField(attr, "name"),
                abbreviation:   getField(attr, "abbreviation"),
                latitude:       attr.get("geoLocation").get("lat").asText(null),
                longitude:      attr.get("geoLocation").get("lon").asText(null),
                summary:        getField(attr, "summary"),
                description:    getField(attr, "description"),
                address:        getField(attr, "address"),
                city:           getField(attr, "city"),
                state:          getField(attr, "state"),
                zip:            getField(attr, "zip"),
                county:         getField(attr, "county"),
                telephone:      getField(attr, "telephone"),
                fax:            getField(attr, "fax"),
                thumbnails:     getListOfString(attr.get("thumbnails")),
                images:         getListOfString(attr.get("images")),
                departments:    getListOfString(attr.get("departments")),
                website:        getField(attr, "website"),
                sqft:           attr.get("sqft").asInt(),
                calendar:       getField(attr, "calendar"),
                campus:         getField(attr, "campus"),
                type:           getField(attr, "type"),
                openHours:      getOpenHours(attr.get("openHours")),
                distance:       sort == null? null: sort.get(0).asDouble()
        )

        attributes
    }

    /**
     * Convert [JsonNode] to [String]
     * @param node
     * @return
     */
    private static List<String> getListOfString(JsonNode node) {
        List<String> result = node?.asList().collect {
            it.asText(null)
        }
        result
    }

    /**
     * Get OpenHours from JsonNode
     * @param node
     * @return
     */
    private static Map<Integer, List<DayOpenHours>> getOpenHours (JsonNode weekHours) {
        Map<Integer, List<DayOpenHours>> openHours = new HashMap<Integer, List<DayOpenHours>>()
        if ( weekHours != null ) {
            (1..7).each {
                openHours.put(it, getHoursList(weekHours, it))
            }

        }

        openHours
    }

    /**
     * Convert [JsonNode] to [DayOpenHour]
     * @param node
     * @return
     */
    private static List<DayOpenHours> getHoursList(JsonNode weekHours, Integer weekday) {
        def dayHour = weekHours.get(Integer.toString(weekday))
        List<DayOpenHours> hoursList = dayHour?.asList().collect() {
            new DayOpenHours(
                    start: (new DateTime(it.get("start").asText())).toDate(),
                    end: (new DateTime(it.get("end").asText())).toDate()
            )
        }
        hoursList
    }

    /**
     * Retrieve data from JsonNode
     * @param node
     * @param fieldName
     * @return
     */
    private static getField(JsonNode node, String fieldName) {
        node.get(fieldName).asText(null)
    }

}
