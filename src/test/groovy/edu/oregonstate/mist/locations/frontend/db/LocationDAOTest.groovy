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
                null, null, null, null, null, null, null, 1, 10)
        final String COMPARE_STRING = stripSpace("""{
          "from": 0,
          "size": 10,
          "query": {
            "bool": {
              "must": [
                {
                  "multi_match": {
                    "query": "hello",
                    "fields": [
                      "attributes.abbreviation^1.0",
                      "attributes.name^1.0",
                      "attributes.synonyms^1.0"
                    ],
                    "type": "best_fields",
                    "operator": "OR",
                    "slop": 0,
                    "prefix_length": 0,
                    "max_expansions": 50,
                    "zero_terms_query": "NONE",
                    "auto_generate_synonyms_phrase_query": true,
                    "fuzzy_transpositions": true,
                    "boost": 1.0
                  }
                }
              ],
              "adjust_pure_negative": true,
              "boost": 1.0
            }
          },
          "sort": [
            {
              "_score": {
                "order": "desc"
              }
            }
          ]
        }""")
        assertEquals(COMPARE_STRING, request.toString())
    }

    @Test
    void testSearchCampus() {
        request = dao.buildSearchRequest(request, "building", "corvallis", null,
                null, null, null,
                null, null, null, null, null, null,null, 1, 10)
        final String COMPARE_STRING = stripSpace("""{
          "from": 0,
          "size": 10,
          "query": {
            "bool": {
              "must": [
                {
                  "match": {
                    "attributes.campus": {
                      "query": "corvallis",
                      "operator": "OR",
                      "prefix_length": 0,
                      "max_expansions": 50,
                      "fuzzy_transpositions": true,
                      "lenient": false,
                      "zero_terms_query": "NONE",
                      "auto_generate_synonyms_phrase_query": true,
                      "boost": 1.0
                    }
                  }
                },
                {
                  "multi_match": {
                    "query": "building",
                    "fields": [
                      "attributes.abbreviation^1.0",
                      "attributes.name^1.0",
                      "attributes.synonyms^1.0"
                    ],
                    "type": "best_fields",
                    "operator": "OR",
                    "slop": 0,
                    "prefix_length": 0,
                    "max_expansions": 50,
                    "zero_terms_query": "NONE",
                    "auto_generate_synonyms_phrase_query": true,
                    "fuzzy_transpositions": true,
                    "boost": 1.0
                  }
                }
              ],
              "adjust_pure_negative": true,
              "boost": 1.0
            }
          },
          "sort": [
            {
              "_score": {
                "order": "desc"
              }
            }
          ]
        }""")
        assertEquals(COMPARE_STRING, request.toString())
    }

    @Test
    void testSearchTypeCulturalCenter() {
        request = dao.buildSearchRequest(request, "building", "", ["cultural-center"],
                null, null, null,
                null, null, null, null, null, null, null, 1, 10)
        final String COMPARE_STRING = stripSpace("""{
          "from": 0,
          "size": 10,
          "query": {
            "bool": {
              "must": [
                {
                  "bool": {
                    "should": [
                      {
                        "match": {
                          "attributes.tags": {
                            "query": "cultural-center",
                            "operator": "OR",
                            "prefix_length": 0,
                            "max_expansions": 50,
                            "fuzzy_transpositions": true,
                            "lenient": false,
                            "zero_terms_query": "NONE",
                            "auto_generate_synonyms_phrase_query": true,
                            "boost": 1.0
                          }
                        }
                      }
                    ],
                    "adjust_pure_negative": true,
                    "boost": 1.0
                  }
                },
                {
                  "multi_match": {
                    "query": "building",
                    "fields": [
                      "attributes.abbreviation^1.0",
                      "attributes.name^1.0",
                      "attributes.synonyms^1.0"
                    ],
                    "type": "best_fields",
                    "operator": "OR",
                    "slop": 0,
                    "prefix_length": 0,
                    "max_expansions": 50,
                    "zero_terms_query": "NONE",
                    "auto_generate_synonyms_phrase_query": true,
                    "fuzzy_transpositions": true,
                    "boost": 1.0
                  }
                }
              ],
              "adjust_pure_negative": true,
              "boost": 1.0
            }
          },
          "sort": [
            {
              "_score": {
                "order": "desc"
              }
            }
          ]
        }""")
        assertEquals(COMPARE_STRING, request.toString())
    }

    @Test
    void testSearchTypeDining() {
        request = dao.prepareLocationSearch()
        request = dao.buildSearchRequest(request, "building", "", ["dining"],
                null, null, null,
                null, null, null, null, null, null, null, 1, 10)
        final String COMPARE_STRING = stripSpace("""{
          "from": 0,
          "size": 10,
          "query": {
            "bool": {
              "must": [
                {
                  "bool": {
                    "should": [
                      {
                        "match": {
                          "attributes.type": {
                            "query": "dining",
                            "operator": "OR",
                            "prefix_length": 0,
                            "max_expansions": 50,
                            "fuzzy_transpositions": true,
                            "lenient": false,
                            "zero_terms_query": "NONE",
                            "auto_generate_synonyms_phrase_query": true,
                            "boost": 1.0
                          }
                        }
                      }
                    ],
                    "adjust_pure_negative": true,
                    "boost": 1.0
                  }
                },
                {
                  "multi_match": {
                    "query": "building",
                    "fields": [
                      "attributes.abbreviation^1.0",
                      "attributes.name^1.0",
                      "attributes.synonyms^1.0"
                    ],
                    "type": "best_fields",
                    "operator": "OR",
                    "slop": 0,
                    "prefix_length": 0,
                    "max_expansions": 50,
                    "zero_terms_query": "NONE",
                    "auto_generate_synonyms_phrase_query": true,
                    "fuzzy_transpositions": true,
                    "boost": 1.0
                  }
                }
              ],
              "adjust_pure_negative": true,
              "boost": 1.0
            }
          },
          "sort": [
            {
              "_score": {
                "order": "desc"
              }
            }
          ]
        }""")
        assertEquals(COMPARE_STRING, request.toString())
    }

    @Test
    void testSearchMultitype() {
        request = dao.prepareLocationSearch()
        request = dao.buildSearchRequest(request, "building", "", ["dining", "cultural-center"],
                null, null, null,
                null, null, null, null, null, null, null, 1, 10)
        final String COMPARE_STRING = stripSpace("""{
          "from": 0,
          "size": 10,
          "query": {
            "bool": {
              "must": [
                {
                  "bool": {
                    "should": [
                      {
                        "match": {
                          "attributes.type": {
                            "query": "dining",
                            "operator": "OR",
                            "prefix_length": 0,
                            "max_expansions": 50,
                            "fuzzy_transpositions": true,
                            "lenient": false,
                            "zero_terms_query": "NONE",
                            "auto_generate_synonyms_phrase_query": true,
                            "boost": 1.0
                          }
                        }
                      },
                      {
                        "match": {
                          "attributes.tags": {
                            "query": "cultural-center",
                            "operator": "OR",
                            "prefix_length": 0,
                            "max_expansions": 50,
                            "fuzzy_transpositions": true,
                            "lenient": false,
                            "zero_terms_query": "NONE",
                            "auto_generate_synonyms_phrase_query": true,
                            "boost": 1.0
                          }
                        }
                      }
                    ],
                    "adjust_pure_negative": true,
                    "boost": 1.0
                  }
                },
                {
                  "multi_match": {
                    "query": "building",
                    "fields": [
                      "attributes.abbreviation^1.0",
                      "attributes.name^1.0",
                      "attributes.synonyms^1.0"
                    ],
                    "type": "best_fields",
                    "operator": "OR",
                    "slop": 0,
                    "prefix_length": 0,
                    "max_expansions": 50,
                    "zero_terms_query": "NONE",
                    "auto_generate_synonyms_phrase_query": true,
                    "fuzzy_transpositions": true,
                    "boost": 1.0
                  }
                }
              ],
              "adjust_pure_negative": true,
              "boost": 1.0
            }
          },
          "sort": [
            {
              "_score": {
                "order": "desc"
              }
            }
          ]
        }""")
        assertEquals(COMPARE_STRING, request.toString())
    }

    @Test
    void testSearchGeoLocation() {
        request = dao.prepareLocationSearch()
        request = dao.buildSearchRequest(request, "building", "", [],
                (Double) 42.39561, (Double) -71.13051, "2miles",
                null, null, null, null, null, null, null, 1, 10)
        final String COMPARE_STRING = stripSpace("""{
          "from": 0,
          "size": 10,
          "query": {
            "bool": {
              "must": [
                {
                  "multi_match": {
                    "query": "building",
                    "fields": [
                      "attributes.abbreviation^1.0",
                      "attributes.name^1.0",
                      "attributes.synonyms^1.0"
                    ],
                    "type": "best_fields",
                    "operator": "OR",
                    "slop": 0,
                    "prefix_length": 0,
                    "max_expansions": 50,
                    "zero_terms_query": "NONE",
                    "auto_generate_synonyms_phrase_query": true,
                    "fuzzy_transpositions": true,
                    "boost": 1.0
                  }
                }
              ],
              "filter": [
                {
                  "geo_distance": {
                    "attributes.geoLocation": [
                      -71.13051,
                      42.39561
                    ],
                    "distance": 3218.688,
                    "distance_type": "arc",
                    "validation_method": "STRICT",
                    "ignore_unmapped": false,
                    "boost": 1.0
                  }
                }
              ],
              "adjust_pure_negative": true,
              "boost": 1.0
            }
          },
          "sort": [
            {
              "_geo_distance": {
                "attributes.geoLocation": [
                  {
                    "lat": 42.39561,
                    "lon": -71.13051
                  }
                ],
                "unit": "km",
                "distance_type": "plane",
                "order": "asc",
                "validation_method": "STRICT"
              }
            },
            {
              "_score": {
                "order": "desc"
              }
            }
          ]
        }""")
        assertEquals(COMPARE_STRING, request.toString())
    }

    @Test
    void testSearchIsOpen() {
        request = dao.prepareLocationSearch()
        request = dao.buildSearchRequest(request, "building", "", [],
                null, null, null,
                Boolean.TRUE, weekday, null, null, null, null, null, 1, 10)
        final String COMPARE_STRING = stripSpace("""{
          "from": 0,
          "size": 10,
          "query": {
            "bool": {
              "must": [
                {
                  "multi_match": {
                    "query": "building",
                    "fields": [
                      "attributes.abbreviation^1.0",
                      "attributes.name^1.0",
                      "attributes.synonyms^1.0"
                    ],
                    "type": "best_fields",
                    "operator": "OR",
                    "slop": 0,
                    "prefix_length": 0,
                    "max_expansions": 50,
                    "zero_terms_query": "NONE",
                    "auto_generate_synonyms_phrase_query": true,
                    "fuzzy_transpositions": true,
                    "boost": 1.0
                  }
                }
              ],
              "filter": [
                {
                  "nested": {
                    "query": {
                      "bool": {
                        "filter": [
                          {
                            "range": {
                              "attributes.openHours.1.start": {
                                "from": null,
                                "to": "now",
                                "include_lower": true,
                                "include_upper": true,
                                "boost": 1.0
                              }
                            }
                          },
                          {
                            "range": {
                              "attributes.openHours.1.end": {
                                "from": "now",
                                "to": null,
                                "include_lower": false,
                                "include_upper": true,
                                "boost": 1.0
                              }
                            }
                          }
                        ],
                        "adjust_pure_negative": true,
                        "boost": 1.0
                      }
                    },
                    "path": "attributes.openHours.1",
                    "ignore_unmapped": false,
                    "score_mode": "avg",
                    "boost": 1.0
                  }
                }
              ],
              "adjust_pure_negative": true,
              "boost": 1.0
            }
          },
          "sort": [
            {
              "_score": {
                "order": "desc"
              }
            }
          ]
        }""")
        assertEquals(COMPARE_STRING, request.toString())
    }

    @Test
    void testSearchParkingSpaces() {
        request = dao.prepareLocationSearch()
        request = dao.buildSearchRequest(request, null, null, [],
            null, null, null,
            Boolean.TRUE, weekday, null, null, 1, 1, 1, 1, 10)
        final String COMPARE_STRING = stripSpace("""{
          "from": 0,
          "size": 10,
          "query": {
            "bool": {
              "must": [
                {
                  "range": {
                    "attributes.adaParkingSpaceCount": {
                      "from": 1,
                      "to": null,
                      "include_lower": true,
                      "include_upper": true,
                      "boost": 1.0
                    }
                  }
                },
                {
                  "range": {
                    "attributes.motorcycleParkingSpaceCount": {
                      "from": 1,
                      "to": null,
                      "include_lower": true,
                      "include_upper": true,
                      "boost": 1.0
                    }
                  }
                },
                {
                  "range": {
                    "attributes.evParkingSpaceCount": {
                      "from": 1,
                      "to": null,
                      "include_lower": true,
                      "include_upper": true,
                      "boost": 1.0
                    }
                  }
                }
              ],
              "filter": [
                {
                  "nested": {
                    "query": {
                      "bool": {
                        "filter": [
                          {
                            "range": {
                              "attributes.openHours.1.start": {
                                "from": null,
                                "to": "now",
                                "include_lower": true,
                                "include_upper": true,
                                "boost": 1.0
                              }
                            }
                          },
                          {
                            "range": {
                              "attributes.openHours.1.end": {
                                "from": "now",
                                "to": null,
                                "include_lower": false,
                                "include_upper": true,
                                "boost": 1.0
                              }
                            }
                          }
                        ],
                        "adjust_pure_negative": true,
                        "boost": 1.0
                      }
                    },
                    "path": "attributes.openHours.1",
                    "ignore_unmapped": false,
                    "score_mode": "avg",
                    "boost": 1.0
                  }
                }
              ],
              "adjust_pure_negative": true,
              "boost": 1.0
            }
          },
          "sort": [
            {
              "_score": {
                "order": "desc"
              }
            }
          ]
        }""")
        println(request.toString())
        assertEquals(COMPARE_STRING, request.toString())
    }

    private static String stripSpace(String str) {
        str.replaceAll("\\s", "")
    }
}
