import unittest
import sys
import json
from api_request import *
from configuration_load import *

class integration_tests(unittest.TestCase):

	# Tests that a good request returns a 200
	def test_success(self):
		self.assertEqual(good_request(url, access_token), 200)

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
		self.assertEqual(not_found_status_code(url, access_token), 404)

	# Tests that a 404 response contains correct JSON fields
	def test_not_found_results(self):
		response = not_found_json(url, access_token)
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
		self.assertTrue(check_ssl(ssl.PROTOCOL_TLSv1, url, access_token))

	# Tests that a call using SSLv2 is unsuccessful
	def test_ssl_v2(self):
		self.assertFalse(check_ssl(ssl.PROTOCOL_SSLv2, url, access_token))

	# Tests that a call using SSLv3 is unsuccessful
	def test_ssl_v3(self):
		self.assertFalse(check_ssl(ssl.PROTOCOL_SSLv3, url, access_token))

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

	unittest.main()