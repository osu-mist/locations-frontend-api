import json
import requests
import urllib2
from configuration_load import *


def good_request():
    query_params = {'q': 'Oxford'}
    headers = {'Authorization': get_access_token()}
    request = requests.get(get_url(), params=query_params, headers=headers)
    return request.status_code

def unauth_request():
    query_params = {'q': 'Oxford'}
    request = requests.get(get_url(), params=query_params)
    return request.status_code
    
def not_found_request():
    query_params = {'campus': 'Pluto'}
    headers = {'Authorization': get_access_token()}
    request = requests.get(get_url(), params=query_params, headers=headers)
    return request.status_code

def blank_result():
    query_params = {'q': 'nosuchbuilding'}
    headers = {'Authorization': get_access_token()}
    request = requests.get(get_url(), params=query_params, headers=headers)
    response = request.json()
    
    if response["data"] == []:
        return True
    else:
        return False

def response_time():
    query_params = {'q': 'Oxford'}
    headers = {'Authorization': get_access_token()}
    request = requests.get(get_url(), params=query_params, headers=headers)
    response_time = request.elapsed.total_seconds()
    
    print "API response time: ", response_time
    return response_time
