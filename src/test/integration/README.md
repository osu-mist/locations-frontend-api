# Locations Frontend API Integration Tests

This directory contains files that run integration tests against the locations frontend API. To run the tests, use Python. These libraries are required to run the integration tests:

* [json](https://docs.python.org/2/library/json.html)
* [requests](http://docs.python-requests.org/en/master/)
* [unittest](https://docs.python.org/2/library/unittest.html)
* [ssl](https://pypi.python.org/pypi/ssl/)
* [urllib2](https://docs.python.org/2/library/urllib2.html)

Use these commands to build and run the container. All you need installed is Docker.

    docker build -t locations-frontend-integration-tests .
    # Run the integration tests in *nix
    docker run -v "$PWD"/configuration.json:/usr/src/app/configuration.json locations-frontend-integration-tests
    # Run the integration tests in Windows
    docker run -v c:\path\to\configuration.json:/c:\usr\src\app\configuration.json locations-frontend-integration-tests

Successfully passing all the tests with the command above would output this result:

![success_test](images/successful-test.png)

Python Version: 2.7.10