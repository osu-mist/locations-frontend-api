import unittest
import sys
import json
from api_request import *
from configuration_load import *

class gateway_tests(unittest.TestCase):

    def test_services(self):
        buildings = get_buildings_with_services(services_url, access_token)
        query_params = {'page[size]': 500}

        for building_id in buildings:
            # Test that a building's service links back to the same building
            building_object = id_request(locations_url, access_token, building_id)
            for service in building_object['data']['relationships']['services']['data']:
                service_object = id_request(services_url, access_token, service['id'])
                parent_id = service_object['data']['relationships']['locations']['data'][0]['id']

                self.assertEqual(building_id, str(parent_id))

            # Test that the relationships object and the services endpoint have the same number of services
            request_url = locations_url + "/" + building_id + "/services"
            building_services = query_request(request_url, access_token, "get", query_params).json()
            self.assertEqual(len(building_object['data']['relationships']['services']['data']), len(building_services['data']))

    # Tests a single resource ID in different case styles
    def test_id(self):
        response = id_request(locations_url, access_token, single_resourse_id)
        self.assertIsNotNone(response["data"])
        self.assertEqual(response["data"]["id"], single_resourse_id)

        response = id_request(locations_url, access_token, single_resourse_id.upper())
        self.assertIsNotNone(response["data"])

        response = id_request(locations_url, access_token, single_resourse_id.lower())
        self.assertIsNotNone(response["data"])

    # Tests that different verbs return expected responses
    def test_verbs(self):
        query_params = {'q': 'Oxford'}

        self.assertEqual(query_request(locations_url, access_token, "get", query_params).status_code, 200)

        # DW not returning allowed methods headers, Apigee returning bad gateway
        if query_request(locations_url, access_token, "post", query_params).status_code == 502:
            self.skipTest('DW not returning allowed methods headers that Apigee is expecting')

        self.assertEqual(query_request(locations_url, access_token, "post", query_params).status_code, 405)
        self.assertEqual(query_request(locations_url, access_token, "put", query_params).status_code, 405)
        self.assertEqual(query_request(locations_url, access_token, "delete", query_params).status_code, 405)

    # Tests that certain parameters return expected number of results
    def test_results(self):
        all_dixon = query_request(locations_url, access_token, "get", {'q': 'Dixon'}).json()
        self.assertEqual(len(all_dixon['data']), 2)

        dining_dixon = query_request(locations_url, access_token, "get", {'q': 'Dixon', 'type': 'dining'}).json()
        self.assertEqual(len(dining_dixon['data']), 1)

        building_dixon = query_request(locations_url, access_token, "get", {'q': 'Dixon', 'type': 'building'}).json()
        self.assertEqual(len(building_dixon['data']), 1)

        # test search only from name + abbr
        building_library = query_request(locations_url, access_token, "get", {'q': 'library', 'campus': 'corvallis'}).json()
        self.assertEqual(len(building_library['data']), 1)

        building_library = query_request(locations_url, access_token, "get", {'q': 'vlib'}).json()
        self.assertEqual(len(building_library['data']), 1)

        # test filter
        dining_library = query_request(locations_url, access_token, "get", {'q': 'library', 'type': 'dining'}).json()
        self.assertEqual(len(dining_library['data']), 0)

        building_engineering = query_request(locations_url, access_token, "get",
            {'q': 'engineering', 'type': 'building', 'campus': 'corvallis'}).json()
        self.assertEqual(len(building_engineering['data']), 2)

    def test_geo_location(self):
        # test geo query
        building_library = query_request(locations_url, access_token, "get",
            {'lat': 44.565066, 'lon': -123.276147}).json()
        self.assertEqual(len(building_library['data']), 10)
        self.assertEqual(building_library['data'][0]['id'], "d409d908ecc6010a04a3b0387f063145")
        self.assertEqual(type(building_library['data'][0]['attributes']['latitude']), unicode)
        self.assertEqual(type(building_library['data'][0]['attributes']['longitude']), unicode)

        building_library = query_request(locations_url, access_token, "get",
            {'lat': 44.565066, 'lon': -123.276147, 'distance': 1, 'distanceUnit': 'yd'}).json()
        self.assertEqual(len(building_library['data']), 1)

        extensions = query_request(locations_url, access_token, "get",
            {'lat': 44.565066, 'lon': -123.276147, 'distance': 10, 'distanceUnit': 'mi',
            'campus':'extension'}).json()
        self.assertEqual(len(extensions['data']), 3)

        dining_java = query_request(locations_url, access_token, "get",
                        {'lat': 44.565066, 'lon': -123.276147, 'isopen': True, 'distanceUnit': 'yd'}).json()
        self.assertEqual(len(dining_java['data']), 1)

    def test_geometries(self):
        # MultiPolygon location
        building_magruder = query_request(locations_url, access_token, "get",
            {'q': 'magruder', 'type': 'building', 'campus': 'corvallis'}).json()
        magruder_geometry = building_magruder['data'][0]['attributes']['geometry']
        self.assertEqual(magruder_geometry['type'], "MultiPolygon")
        self.assertEqual(len(magruder_geometry['coordinates']), 4)
        self.assertEqual(len(magruder_geometry['coordinates'][0][0][0]), 2)
        self.assertEqual(type(magruder_geometry['coordinates'][0][0][0][0]), float)
        self.assertEqual(type(magruder_geometry['coordinates'][0][0][0][1]), float)
        # First and last coordinate pairs in a ring should be equal: https://tools.ietf.org/html/rfc7946#section-3.1.6
        self.assertEqual(magruder_geometry['coordinates'][0][0][0], magruder_geometry['coordinates'][0][0][-1])
        self.assertEqual(magruder_geometry['coordinates'][-1][0][0], magruder_geometry['coordinates'][-1][0][-1])

        # Polygon location
        building_mu = query_request(locations_url, access_token, "get",
            {'q': 'memorial', 'type': 'building', 'campus': 'corvallis'}).json()
        mu_geometry = building_mu['data'][0]['attributes']['geometry']
        self.assertEqual(mu_geometry['type'], "Polygon")
        self.assertEqual(len(mu_geometry['coordinates']), 1)
        self.assertEqual(mu_geometry['coordinates'][0][0], mu_geometry['coordinates'][0][-1])

    # Tests results of a query that should return only locations with gender inclusive restrooms
    def test_gender_inclusive_rr(self):
        gi_rr = query_request(locations_url, access_token, "get",
              {'giRestroom': 'true', 'page[size]': 5000}).json()

        for location in gi_rr['data']:
            attributes = location['attributes']
            self.assertGreater(attributes['giRestroomCount'], 0)
            self.assertIsNotNone(attributes['giRestroomLimit'])

    # Tests that a query with more than 10 results contains correct links
    def test_links(self):
        links = results_with_links(locations_url, access_token)
        self.assertIsNotNone(links["self"])
        self.assertIsNotNone(links["first"])
        self.assertIsNotNone(links["last"])
        self.assertIsNone(links["prev"])
        self.assertIsNotNone(links["next"])

    # Tests that a request with auth header returns a 401
    def test_unauth(self):
        self.assertEqual(unauth_request(locations_url), 401)

    # Tests that a nonexistent campus returns a 404
    def test_not_found(self):
        self.assertEqual(not_found_request(locations_url, access_token,
            {'q': 'Hello world', 'campus': 'Pluto'}).status_code, 404)
        self.assertEqual(not_found_request(locations_url, access_token,
            {'q': 'Hello world', 'type': 'invalid-type'}).status_code, 404)
        self.assertEqual(not_found_request(locations_url, access_token,
            {'q': 'Hello world', 'campus': 'Pluto', 'type': 'invalid-type'}).status_code, 404)

    # Tests that a 404 response contains correct JSON fields
    def test_not_found_results(self):
        response = not_found_request(locations_url, access_token, {'campus': 'Pluto'}).json()
        self.assertIsNotNone(response["status"])
        self.assertIsNotNone(response["developerMessage"])
        self.assertIsNotNone(response["userMessage"])
        self.assertIsNotNone(response["code"])
        self.assertIsNotNone(response["details"])

    # Tests that that a certain query returns no data
    def test_blank_result(self):
        self.assertTrue(blank_result(locations_url, access_token))

    # Tests that a request for all locations is successful
    def test_all_locations(self):
        query_params = {'page[number]': 1, 'page[size]': 5000}
        self.assertEqual(query_request(locations_url, access_token, "get", query_params).status_code, 200)

    # Tests that API response time is less than a value
    def test_response_time(self):
        self.assertLess(response_time(locations_url, access_token), 1)

    # Tests that a call using TLSv1 is successful
    def test_tls_v1(self):
        self.assertTrue(check_ssl(ssl.PROTOCOL_TLSv1, locations_url, access_token))

    # Tests that a call using SSLv2 is unsuccessful
    def test_ssl_v2(self):
        try:
            # openssl can be compiled without SSLv2 support, in which case
            # the PROTOCOL_SSLv2 constant is not available
            ssl.PROTOCOL_SSLv2
        except AttributeError:
            self.skipTest('SSLv2 support not available')
        self.assertFalse(check_ssl(ssl.PROTOCOL_SSLv2, locations_url, access_token))

    # Tests that a call using SSLv3 is unsuccessful
    def test_ssl_v3(self):
        try:
            # openssl can be compiled without SSLv3 support, in which case
            # the PROTOCOL_SSLv3 constant is not available
            ssl.PROTOCOL_SSLv3
        except AttributeError:
            self.skipTest('SSLv3 support not available')
        self.assertFalse(check_ssl(ssl.PROTOCOL_SSLv3, locations_url, access_token))

if __name__ == '__main__':
    options_tpl = ('-i', 'config_path')
    del_list = []

    for i,config_path in enumerate(sys.argv):
        if config_path in options_tpl:
            del_list.append(i)
            del_list.append(i+1)

    del_list.reverse()

    for i in del_list:
        del sys.argv[i]

    url = get_url(config_path)
    access_token = get_access_token(config_path)
    single_resourse_id = get_single_resource_id(config_path)

    locations_url = url + "/locations"
    services_url = url + "/services"

    unittest.main()
