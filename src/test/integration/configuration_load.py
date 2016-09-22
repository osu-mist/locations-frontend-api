import json
import requests

def get_config_data(config_path):
	config_data_file = open(config_path)
	return json.load(config_data_file)

def get_url(config_path):
	config_data = get_config_data(config_path)
	return config_data["hostname"] + config_data["version"] + config_data["api"]

def get_access_token(config_path):
	config_data = get_config_data(config_path)
	access_token_url = get_url(config_path) + config_data["token_endpoint"]
	client_id = config_data["client_id"]
	client_secret = config_data["client_secret"]
	post_data = {'client_id': client_id, 'client_secret': client_secret, 'grant_type': 'client_credentials'}
	request = requests.post(access_token_url, data=post_data)
	response = request.json()
	return 'Bearer ' + response["access_token"]
