import unittest
from api_request import * 

class integration_tests(unittest.TestCase):

	def test_OK(self):
		self.assertEqual(getStatusCode(200), 200)

	def test_unauth(self):
		self.assertEqual(getStatusCode(401), 401)

	def test_not_found(self):
		self.assertEqual(getStatusCode(404), 404)
if __name__ == '__main__':
    unittest.main()