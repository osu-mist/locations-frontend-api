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
                id: source.get("id").asText(null),
                type: source.get("type").asText(null),
                attributes: getAttributes(source.get("attributes"), hit.get('sort')),
                links: source.get("links")
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
                name: attr.get("name").asText(null),
                abbreviation: attr.get("abbreviation").asText(null),
                latitude: attr.get("geoLocation").get("lat").asText(null),
                longitude: attr.get("geoLocation").get("lon").asText(null),
                summary: attr.get("summary").asText(null),
                description: attr.get("description").asText(null),
                address: attr.get("address").asText(null),
                city: attr.get("city").asText(null),
                state: attr.get("state").asText(null),
                zip: attr.get("zip").asText(null),
                county: attr.get("county").asText(null),
                telephone: attr.get("telephone").asText(null),
                fax: attr.get("fax").asText(null),
                thumbnails: getListOfString(attr.get("thumbnails")),
                images:  getListOfString(attr.get("images")),
                departments:  getListOfString(attr.get("departments")),
                website: attr.get("website").asText(null),
                sqft: attr.get("sqft").asInt(),
                calendar: attr.get("calendar").asText(null),
                campus: attr.get("campus").asText(null),
                type: attr.get("type").asText(null),
                openHours: getOpenHours(attr.get("openHours")),
                distance: sort.get(0).asDouble()
        )

        attributes
    }

    /**
     * Convert [JsonNode] to [String]
     * @param node
     * @return
     */
    private static List<String> getListOfString(JsonNode node) {

        List<String> result = null

        if ( node != null ) {
            node.asList().each {
                result.add(it.asText(null))
            }
        }
        result
    }

    /**
     * Get OpenHours from JsonNode
     * @param node
     * @return
     */
    private static Map<Integer, List<DayOpenHours>> getOpenHours (JsonNode node) {
        Map<Integer, List<DayOpenHours>> openHours = new HashMap<Integer, List<DayOpenHours>>()
        if ( node != null ) {
            (1..7).each {
                openHours.put(it, getHoursList(node.get(Integer.toString(it))))
            }
        }

        openHours
    }

    /**
     * Convert [JsonNode] to [DayOpenHour]
     * @param node
     * @return
     */
    private static List<DayOpenHours> getHoursList(JsonNode node) {
        List<DayOpenHours> hoursList = new ArrayList<DayOpenHours>()

        if ( node != null ) {
            node.asList().each {
                hoursList.add(new DayOpenHours(
                        start: (new DateTime(it.get("start").asText())).toDate(),
                        end: (new DateTime(it.get("end").asText())).toDate()
                ))
            }
        }

        hoursList
    }

}
