package edu.oregonstate.mist.api.jsonapi

import com.fasterxml.jackson.annotation.JsonProperty

class Geometry {
    @JsonProperty("type")
    String type
}

class Geometries extends Geometry {
    @JsonProperty("geometries")
    List<Geometry> geometries
}

class CooridinateGeometry extends Geometry {
    @JsonProperty("coordinates")
    List<Object> coordinates
}

class GeoJsonResultObject {
    @JsonProperty("type")
    String type
    @JsonProperty("geometry")
    Geometry geometry
    @JsonProperty("properties")
    Object properties
}
