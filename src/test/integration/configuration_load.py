import json

config_data_file = open('configuration.json')
config_data = json.load(config_data_file)

base_url = config_data["hostname"] + config_data["version"] + config_data["api"]
access_token_url = base_url + config_data["token_endpoint"]

def get_url():
	return base_url + config_data["api_endpoint"]

def get_access_token_url():
	return base_url + config_data["token_endpoint"]

def get_client_id():
	return config_data["client_id"]

def get_client_secret():
	return config_data["client_secret"]