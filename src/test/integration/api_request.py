import json
import requests
from configuration_load import *

def getAccessToken(url, client_id, client_secret):
    post_data = {'client_id': client_id, 'client_secret': client_secret, 'grant_type': 'client_credentials'}
    request = requests.post(url, data=post_data)
    response = request.json()
    return 'Bearer ' + response["access_token"]

def goodRequest(url, access_token):
    query_params = {'q': 'Oxford'}
    headers = {'Authorization': access_token}
    request = requests.get(url, params=query_params, headers=headers)
    return request.status_code

def unauthRequest(url):
    query_params = {'q': 'Oxford'}
    request = requests.get(url, params=query_params)
    return request.status_code
    
def notFoundRequest(url, access_token):
    query_params = {'campus': 'Pluto'}
    headers = {'Authorization': access_token}
    request = requests.get(url, params=query_params, headers=headers)
    return request.status_code

def getStatusCode(test_code):
    url = get_url()
    access_token_url = get_access_token_url()
    client_id = get_client_id()
    client_secret = get_client_secret()

    if test_code == 200:
        access_token = getAccessToken(access_token_url, client_id, client_secret)
        response_code = goodRequest(url, access_token)

    if test_code == 401:
        response_code = unauthRequest(url)

    if test_code == 404:
        access_token = getAccessToken(access_token_url, client_id, client_secret)
        response_code = notFoundRequest(url, access_token)

    return response_code