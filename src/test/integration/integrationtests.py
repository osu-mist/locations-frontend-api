import unittest
from api_request import *

class integration_tests(unittest.TestCase):

	def test_success(self):
		self.assertEqual(good_request(), 200)

	def test_unauth(self):
		self.assertEqual(unauth_request(), 401)

	def test_not_found(self):
		self.assertEqual(not_found_request(), 404)

	def test_blank_result(self):
		self.assertTrue(blank_result())

	def test_response_time(self):
		self.assertLess(response_time(), .75)

if __name__ == '__main__':
    unittest.main()