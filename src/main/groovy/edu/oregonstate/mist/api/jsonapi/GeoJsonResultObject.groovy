package edu.oregonstate.mist.api.jsonapi

import com.fasterxml.jackson.annotation.JsonProperty

class Geometry {
    @JsonProperty("type")
    String type
}

class Geometries extends Geometry {
    @JsonProperty("geometries")
    def geometries
}

class GeoCooridinate extends Geometry {
    @JsonProperty("coordinates")
    List<Object> coordinates
}

class GeoJsonResultObject {
    @JsonProperty("type")
    String type
}

class GeoFeature extends GeoJsonResultObject {
    @JsonProperty("geometry")
    Geometry geometry
    @JsonProperty("properties")
    def properties
}

class GeoFeatureCollection extends GeoJsonResultObject {
    @JsonProperty("features")
    List<GeoFeature> features = []
}

