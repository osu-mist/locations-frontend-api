package edu.oregonstate.mist.locations.frontend.db;

import org.junit.Test;

import static org.junit.Assert.*;

@groovy.transform.TypeChecked
public class LocationDAOTest {
    @Test
    void testSearch() {
        def configuration = [
                esUrl  : "http://localhost:9200",
                esIndex: "locations",
                estype : "locations",
        ]
        Integer weekday = 1
        def dao = new LocationDAO(configuration)
        def request = dao.prepareLocationSearch()
        request = dao.buildSearchRequest(request, "hello", null, null, null, null, null, null, null, null, 1, 10)
        assertEquals('''{
  "from" : 0,
  "size" : 10,
  "query" : {
    "bool" : {
      "filter" : {
        "multi_match" : {
          "query" : "hello",
          "fields" : [ "attributes.name", "attributes.abbreviation" ]
        }
      }
    }
  }
}''', request.toString())

        request = dao.prepareLocationSearch()
        request = dao.buildSearchRequest(request, "building", "corvallis", null, null, null, null, null, null, null, 1, 10)
        assertEquals('''{
  "from" : 0,
  "size" : 10,
  "query" : {
    "bool" : {
      "must" : {
        "match" : {
          "attributes.campus" : {
            "query" : "corvallis",
            "type" : "boolean"
          }
        }
      },
      "filter" : {
        "multi_match" : {
          "query" : "building",
          "fields" : [ "attributes.name", "attributes.abbreviation" ]
        }
      }
    }
  }
}''', request.toString())

        request = dao.prepareLocationSearch()
        request = dao.buildSearchRequest(request, "building", "", "cultural-center", null, null, null, null, null, null, 1, 10)
        assertEquals('''{
  "from" : 0,
  "size" : 10,
  "query" : {
    "bool" : {
      "must" : {
        "match" : {
          "attributes.tags" : {
            "query" : "cultural-center",
            "type" : "boolean"
          }
        }
      },
      "filter" : {
        "multi_match" : {
          "query" : "building",
          "fields" : [ "attributes.name", "attributes.abbreviation" ]
        }
      }
    }
  }
}''', request.toString())

        request = dao.prepareLocationSearch()
        request = dao.buildSearchRequest(request, "building", "", "dining", null, null, null, null, null, null, 1, 10)
        assertEquals('''{
  "from" : 0,
  "size" : 10,
  "query" : {
    "bool" : {
      "must" : {
        "match" : {
          "attributes.type" : {
            "query" : "dining",
            "type" : "boolean"
          }
        }
      },
      "filter" : {
        "multi_match" : {
          "query" : "building",
          "fields" : [ "attributes.name", "attributes.abbreviation" ]
        }
      }
    }
  }
}''', request.toString())

        request = dao.prepareLocationSearch()
        request = dao.buildSearchRequest(request, "building", "", "", (Double) 42.39561, (Double) -71.13051, "2miles", null, null, null, 1, 10)
        assertEquals(request.toString(), '''{
  "from" : 0,
  "size" : 10,
  "query" : {
    "bool" : {
      "filter" : [ {
        "multi_match" : {
          "query" : "building",
          "fields" : [ "attributes.name", "attributes.abbreviation" ]
        }
      }, {
        "geo_distance" : {
          "attributes.geoLocation" : [ -71.13051, 42.39561 ],
          "distance" : "2miles"
        }
      } ]
    }
  },
  "sort" : [ {
    "_geo_distance" : {
      "attributes.geoLocation" : [ {
        "lat" : 42.39561,
        "lon" : -71.13051
      } ],
      "unit" : "km",
      "distance_type" : "plane"
    }
  } ]
}''')

        request = dao.prepareLocationSearch()
        request = dao.buildSearchRequest(request, "building", "", "", null, null, null, Boolean.TRUE, weekday, null, 1, 10)
        assertEquals(request.toString(), '''{
  "from" : 0,
  "size" : 10,
  "query" : {
    "bool" : {
      "filter" : [ {
        "multi_match" : {
          "query" : "building",
          "fields" : [ "attributes.name", "attributes.abbreviation" ]
        }
      }, {
        "nested" : {
          "query" : {
            "bool" : {
              "filter" : [ {
                "range" : {
                  "attributes.openHours.1.start" : {
                    "from" : null,
                    "to" : "now",
                    "include_lower" : true,
                    "include_upper" : true
                  }
                }
              }, {
                "range" : {
                  "attributes.openHours.1.end" : {
                    "from" : "now",
                    "to" : null,
                    "include_lower" : false,
                    "include_upper" : true
                  }
                }
              } ]
            }
          },
          "path" : "attributes.openHours.1"
        }
      } ]
    }
  }
}''')
    }

}