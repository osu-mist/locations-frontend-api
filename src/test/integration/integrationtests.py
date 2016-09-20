import unittest
from api_request import *

class integration_tests(unittest.TestCase):

	# Tests that a good request returns a 200
	def test_success(self):
		self.assertEqual(good_request(), 200)

	# Tests that a request with auth header returns a 401
	def test_unauth(self):
		self.assertEqual(unauth_request(), 401)

	# Tests that a nonexistent campus returns a 404
	def test_not_found(self):
		self.assertEqual(not_found_request(), 404)

	# Tests that that a certain query returns no data
	def test_blank_result(self):
		self.assertTrue(blank_result())

	# Tests that API response time is less than a value
	def test_response_time(self):
		self.assertLess(response_time(), 1)

	# Tests that a call using TLSv1 is successful
	def test_tls_v1(self):
		self.assertTrue(check_ssl(ssl.PROTOCOL_TLSv1))

	# Tests that a call using SSLv2 is unsuccessful
	def test_ssl_v2(self):
		self.assertFalse(check_ssl(ssl.PROTOCOL_SSLv2))

	# Tests that a call using SSLv3 is unsuccessful
	def test_ssl_v3(self):
		self.assertFalse(check_ssl(ssl.PROTOCOL_SSLv3))

if __name__ == '__main__':
    unittest.main()