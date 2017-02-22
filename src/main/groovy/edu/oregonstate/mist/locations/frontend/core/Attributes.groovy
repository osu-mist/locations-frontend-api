package edu.oregonstate.mist.locations.frontend.core

class Attributes {
    String name
    String abbreviation
    String latitude
    String longitude
    String summary
    String description
    String address
    String city
    String state
    String zip
    String county
    String telephone
    String fax
    List<String> thumbnails
    List<String> images
    List<String> departments
    String website
    Integer sqft
    String calendar
    String campus
    String type // used for searching. values: building, dining.
    Map<Integer, List<DayOpenHours>> openHours = new HashMap<Integer, List<DayOpenHours>>()
    Double distance
}