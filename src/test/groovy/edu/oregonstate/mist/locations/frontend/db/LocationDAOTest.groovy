package edu.oregonstate.mist.locations.frontend.db

import groovy.transform.TypeChecked
import org.elasticsearch.action.search.SearchRequestBuilder
import org.junit.After
import org.junit.Before
import org.junit.Test

import static org.junit.Assert.*

/**
 * These tests check that the locationDAO constructs queries that look like
 * what we expect - in particular, we want them to match the manual REST queries
 * we had been using before.
 */
@TypeChecked
public class LocationDAOTest {
    Integer weekday = 1
    private ElasticSearchManager esManager
    private LocationDAO dao
    private SearchRequestBuilder request

    @Before
    void setUp() {
        def configuration = [
                esUrl  : "http://localhost:9300",
                esIndex: "locations",
                estype : "locations",
        ]

        esManager = new ElasticSearchManager(configuration.get("esUrl"))
        esManager.start()
        dao = new LocationDAO(configuration, esManager)
        request = dao.prepareLocationSearch()
    }

    @After
    void tearDown() {
        esManager.stop()
    }

    @Test
    void testSearchQuery() {
        request = dao.buildSearchRequest(request, "hello", null, null,
                null, null, null,
                null, null, null, null, 1, 10)
        assertEquals('''{
  "from" : 0,
  "size" : 10,
  "query" : {
    "bool" : {
      "must" : {
        "multi_match" : {
          "query" : "hello",
          "fields" : [ "attributes.name", "attributes.abbreviation", "attributes.synonyms" ]
        }
      }
    }
  },
  "sort" : [ {
    "_score" : { }
  } ]
}''', request.toString())
    }

    @Test
    void testSearchCampus() {
        request = dao.buildSearchRequest(request, "building", "corvallis", null,
                null, null, null,
                null, null, null, null, 1, 10)
        assertEquals('''{
  "from" : 0,
  "size" : 10,
  "query" : {
    "bool" : {
      "must" : [ {
        "match" : {
          "attributes.campus" : {
            "query" : "corvallis",
            "type" : "boolean"
          }
        }
      }, {
        "multi_match" : {
          "query" : "building",
          "fields" : [ "attributes.name", "attributes.abbreviation", "attributes.synonyms" ]
        }
      } ]
    }
  },
  "sort" : [ {
    "_score" : { }
  } ]
}''', request.toString())
    }

    @Test
    void testSearchTypeCulturalCenter() {
        request = dao.buildSearchRequest(request, "building", "", ["cultural-center"],
                null, null, null,
                null, null, null, null, 1, 10)
        assertEquals('''{
  "from" : 0,
  "size" : 10,
  "query" : {
    "bool" : {
      "must" : [ {
        "bool" : {
          "should" : {
            "match" : {
              "attributes.tags" : {
                "query" : "cultural-center",
                "type" : "boolean"
              }
            }
          }
        }
      }, {
        "multi_match" : {
          "query" : "building",
          "fields" : [ "attributes.name", "attributes.abbreviation", "attributes.synonyms" ]
        }
      } ]
    }
  },
  "sort" : [ {
    "_score" : { }
  } ]
}''', request.toString())
    }

    @Test
    void testSearchTypeDining() {
        request = dao.prepareLocationSearch()
        request = dao.buildSearchRequest(request, "building", "", ["dining"],
                null, null, null,
                null, null, null, null, 1, 10)
        assertEquals('''{
  "from" : 0,
  "size" : 10,
  "query" : {
    "bool" : {
      "must" : [ {
        "bool" : {
          "should" : {
            "match" : {
              "attributes.type" : {
                "query" : "dining",
                "type" : "boolean"
              }
            }
          }
        }
      }, {
        "multi_match" : {
          "query" : "building",
          "fields" : [ "attributes.name", "attributes.abbreviation", "attributes.synonyms" ]
        }
      } ]
    }
  },
  "sort" : [ {
    "_score" : { }
  } ]
}''', request.toString())
    }

    @Test
    void testSearchMultitype() {
        request = dao.prepareLocationSearch()
        request = dao.buildSearchRequest(request, "building", "", ["dining", "cultural-center"],
                null, null, null,
                null, null, null, null, 1, 10)
        assertEquals('''{
  "from" : 0,
  "size" : 10,
  "query" : {
    "bool" : {
      "must" : [ {
        "bool" : {
          "should" : [ {
            "match" : {
              "attributes.type" : {
                "query" : "dining",
                "type" : "boolean"
              }
            }
          }, {
            "match" : {
              "attributes.tags" : {
                "query" : "cultural-center",
                "type" : "boolean"
              }
            }
          } ]
        }
      }, {
        "multi_match" : {
          "query" : "building",
          "fields" : [ "attributes.name", "attributes.abbreviation", "attributes.synonyms" ]
        }
      } ]
    }
  },
  "sort" : [ {
    "_score" : { }
  } ]
}''', request.toString())
    }

    @Test
    void testSearchGeoLocation() {
        request = dao.prepareLocationSearch()
        request = dao.buildSearchRequest(request, "building", "", [],
                (Double) 42.39561, (Double) -71.13051, "2miles",
                null, null, null, null, 1, 10)
        assertEquals(request.toString(), '''{
  "from" : 0,
  "size" : 10,
  "query" : {
    "bool" : {
      "must" : {
        "multi_match" : {
          "query" : "building",
          "fields" : [ "attributes.name", "attributes.abbreviation", "attributes.synonyms" ]
        }
      },
      "filter" : {
        "geo_distance" : {
          "attributes.geoLocation" : [ -71.13051, 42.39561 ],
          "distance" : "2miles"
        }
      }
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
  }, {
    "_score" : { }
  } ]
}''')
    }

    @Test
    void testSearchIsOpen() {
        request = dao.prepareLocationSearch()
        request = dao.buildSearchRequest(request, "building", "", [],
                null, null, null,
                Boolean.TRUE, weekday, null, null, 1, 10)
        assertEquals(request.toString(), '''{
  "from" : 0,
  "size" : 10,
  "query" : {
    "bool" : {
      "must" : {
        "multi_match" : {
          "query" : "building",
          "fields" : [ "attributes.name", "attributes.abbreviation", "attributes.synonyms" ]
        }
      },
      "filter" : {
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
      }
    }
  },
  "sort" : [ {
    "_score" : { }
  } ]
}''')
    }

}
