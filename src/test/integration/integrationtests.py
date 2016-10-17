import unittest
import sys
import json
from api_request import *
from configuration_load import *

class gateway_tests(unittest.TestCase):

    # Tests a single resource ID in different case styles
    def test_id(self):
        response = id_request(url, access_token, single_resourse_id)
        self.assertIsNotNone(response["data"])
        self.assertEqual(response["data"]["id"], single_resourse_id)

        response = id_request(url, access_token, single_resourse_id.upper())
        self.assertIsNotNone(response["data"])

        response = id_request(url, access_token, single_resourse_id.lower())
        self.assertIsNotNone(response["data"])

    # Tests that different verbs return expected responses
    def test_verbs(self):
        query_params = {'q': 'Oxford'}

        self.assertEqual(query_request(url, access_token, "get", query_params).status_code, 200)
        self.assertEqual(query_request(url, access_token, "post", query_params).status_code, 405)
        self.assertEqual(query_request(url, access_token, "put", query_params).status_code, 405)
        self.assertEqual(query_request(url, access_token, "delete", query_params).status_code, 405)

    # Tests that certain parameters return expected number of results
    def test_results(self):
        all_dixon = query_request(url, access_token, "get", {'q': 'Dixon'}).json()
        self.assertEqual(len(all_dixon['data']), 3)

        dining_dixon = query_request(url, access_token, "get", {'q': 'Dixon', 'type': 'dining'}).json()
        self.assertEqual(len(dining_dixon['data']), 1)

        building_dixon = query_request(url, access_token, "get", {'q': 'Dixon', 'type': 'building'}).json()
        self.assertEqual(len(building_dixon['data']), 2)

    # Tests that a query with more than 10 results contains correct links
    def test_links(self):
        links = results_with_links(url, access_token)
        self.assertIsNotNone(links["self"])
        self.assertIsNotNone(links["first"])
        self.assertIsNotNone(links["last"])
        self.assertIsNone(links["prev"])
        self.assertIsNotNone(links["next"])

    # Tests that a request with auth header returns a 401
    def test_unauth(self):
        self.assertEqual(unauth_request(url), 401)

    # Tests that a nonexistent campus returns a 404
    def test_not_found(self):
        self.assertEqual(not_found_request(url, access_token, 
            {'q': 'Hello world', 'campus': 'Pluto'}).status_code, 404)
        self.assertEqual(not_found_request(url, access_token, 
            {'q': 'Hello world', 'type': 'invalid-type'}).status_code, 404)
        self.assertEqual(not_found_request(url, access_token, 
            {'q': 'Hello world', 'campus': 'Pluto', 'type': 'invalid-type'}).status_code, 404)

    # Tests that a 404 response contains correct JSON fields
    def test_not_found_results(self):
        response = not_found_request(url, access_token, {'campus': 'Pluto'}).json()
        self.assertIsNotNone(response["status"])
        self.assertIsNotNone(response["developerMessage"])
        self.assertIsNotNone(response["userMessage"])
        self.assertIsNotNone(response["code"])
        self.assertIsNotNone(response["details"])
    # Tests that that a certain query returns no data
    def test_blank_result(self):
        self.assertTrue(blank_result(url, access_token))

    # Tests that API response time is less than a value
    def test_response_time(self):
        self.assertLess(response_time(url, access_token), 1)

    # Tests that a call using TLSv1 is successful
    def test_tls_v1(self):
        self.assertTrue(check_ssl(ssl.PROTOCOL_TLSv1, url))

    # Tests that a call using SSLv2 is unsuccessful
    def test_ssl_v2(self):
        try:
            # openssl can be compiled without SSLv2 support, in which case
            # the PROTOCOL_SSLv2 constant is not available
            ssl.PROTOCOL_SSLv2
        except AttributeError:
            self.skipTest('SSLv2 support not available')
        self.assertFalse(check_ssl(ssl.PROTOCOL_SSLv2, url))

    # Tests that a call using SSLv3 is unsuccessful
    def test_ssl_v3(self):
        try:
            # openssl can be compiled without SSLv3 support, in which case
            # the PROTOCOL_SSLv3 constant is not available
            ssl.PROTOCOL_SSLv3
        except AttributeError:
            self.skipTest('SSLv3 support not available')
        self.assertFalse(check_ssl(ssl.PROTOCOL_SSLv3, url))

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

    unittest.main()