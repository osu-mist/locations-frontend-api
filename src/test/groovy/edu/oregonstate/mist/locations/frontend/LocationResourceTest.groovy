package edu.oregonstate.mist.locations.frontend

import com.fasterxml.jackson.databind.JsonNode
import edu.oregonstate.mist.api.jsonapi.ResultObject
import edu.oregonstate.mist.locations.frontend.db.LocationDAO
import edu.oregonstate.mist.locations.frontend.resources.LocationResource
import groovy.mock.interceptor.MockFor
import io.dropwizard.testing.junit.DropwizardAppRule
import org.junit.ClassRule
import org.junit.Test

import javax.ws.rs.core.Response
import javax.ws.rs.core.UriBuilder

class LocationResourceTest {
    static URI endpointUri = UriBuilder.fromPath('https://api.unit.test.edu/v1/').build()

    @ClassRule
    public static final DropwizardAppRule<LocationsFrontendConfiguration> APPLICATION =
            new DropwizardAppRule<LocationsFrontendConfiguration>(
                    LocationsFrontEndApplication.class,
                    new File("configuration.yaml").absolutePath)
    // Test: LocationResource.list()

//    @Test
    public void testList() {
        def mock = new MockFor(LocationDAO)
        mock.demand.search() {
            String q, String campus, String type, Double lat,
            Double lon, String searchDistance, Boolean isOpen, Integer weekday,
            Boolean giRestroom, String parkingZoneGroup, Integer pageNumber, Integer pageSize ->
                '{"hits": {"total": 0, "hits": []}}'
        }
        def dao = mock.proxyInstance()
        def resource = new LocationResource(dao, endpointUri)
        resource.uriInfo = new MockUriInfo()

        // Test: no result
        def noResultRsp = resource.list('dixon', null,
                null, null, null, null, null, null, false, null)
        assert noResultRsp.status == 200
        assert noResultRsp.entity.links == [:]
        assert noResultRsp.entity.data == []

        // Test: invalid campus
        def invalidCampRes = resource.list('dixon', 'invalid',
                null, null, null, null, null, null, null, null)
        assert invalidCampRes.status == 404
        assert invalidCampRes.entity.developerMessage.contains("Not Found")
        assert invalidCampRes.entity.userMessage.contains("Not Found")
        assert invalidCampRes.entity.code == 1404

        mock.verify(dao)
    }

    // Test: LocationResource.getById(): valid ID
//    @Test
    public void testValidId() {
        def mock = new MockFor(LocationDAO)
        mock.demand.getById() {
            String id -> '{"id":"","type":"locations","attributes":{}}'
        }
        def dao = mock.proxyInstance()
        def resource = new LocationResource(dao, endpointUri)
        resource.uriInfo = new MockUriInfo()

        def validIdRes = resource.getById('valid-id')
        assert validIdRes.status == 200
        validIdRes.entity.links == [:]
        validIdRes.entity.data == '{"id":"","type":"locations","attributes":{}}'

        mock.verify(dao)
    }

    // Test: LocationResource.getById(): invalid ID
//    @Test
    public void testInvalidId() {
        def mock = new MockFor(LocationDAO)
        mock.demand.getById() {
            String id -> null
        }
        def dao = mock.proxyInstance()
        def resource = new LocationResource(dao, endpointUri)
        resource.uriInfo = new MockUriInfo()

        def invalidIdRes = resource.getById(null)
        assert invalidIdRes.status == 404
        assert invalidIdRes.entity.developerMessage.contains("Not Found")
        assert invalidIdRes.entity.userMessage.contains("Not Found")
        assert invalidIdRes.entity.code == 1404

        mock.verify(dao)
    }

    // Test: LocationResource.sanitize()
    @Test
    public void testSanitize() {
        String legalStr = 'abc_ABC-123.@'
        String illegalStr = ' ~!#$%^&*()=+[]{}|\\\'\";:<>?/'
        String mixStr = ' a~b!c_#A$B%C^-&1*2(3).=@+'

        assert LocationResource.sanitize(legalStr) == legalStr
        assert LocationResource.sanitize(illegalStr).replaceAll(" ", "") == ""
        assert LocationResource.sanitize(mixStr).replaceAll(" ", "") == legalStr
        assert !LocationResource.sanitize(null)
    }

//    @Test
    public void testListEndpointURLParams() {
        String esStubData = new File(
                "src/test/groovy/edu/oregonstate/mist/locations/frontend/esMockData.json").text

        def jsonNodeMock = new MockFor(JsonNode)
        jsonNodeMock.demand.get() {
            1
        }
        def usable = jsonNodeMock.proxyInstance()
        assert(usable.get("total") == 1)

        def mock = new MockFor(LocationDAO)
        mock.demand.search() {
            String q, String campus, String type, Double lat,
            Double lon, String searchDistance, Boolean isOpen, Integer weekday,
            Boolean giRestroom, List<String> parkingZoneGroup,
            Integer pageNumber, Integer pageSize -> esStubData
        }
        def dao = mock.proxyInstance()
        def resource = new LocationResource(dao, endpointUri)
        resource.uriInfo = new MockUriInfo()
        //This mocking is to ensure LocationsResource.groovy#L177 passes\

        def expectedParams = [
                'q'                 : 'dixon',
                'campus'            : "corvallis",
                'type'              : "building",
                'lat'               : 44.55,
                'lon'               : 77.77,
                'distance'          : 2.0,
                'distanceUnit'      : "mi",
                'isOpen'            : true,
                'giRestroom'        : true,
                'parkingZoneGroup'  : ['A2', 'C']
        ]

        Response res = resource.list(
                (String) expectedParams['q'],
                (String) expectedParams['campus'],
                (String) expectedParams['type'],
                (Double) expectedParams['lat'],
                (Double) expectedParams['lon'],
                (Double) expectedParams['distance'],
                (String) expectedParams['distanceUnit'],
                (Boolean) expectedParams['isOpen'],
                (Boolean) expectedParams['giRestroom'],
                (List<String>) expectedParams['parkingZoneGroup'])
        ResultObject resObj = res.entity
        String selfLinks = resObj.links["self"]

        def params = selfLinks.substring(selfLinks.indexOf('?') + 1).split('&')
        def actualParams = [:]
        params.each {
            String p = URLDecoder.decode(it, "UTF-8")
            def keyValPair = p.split('=')

            actualParams[keyValPair[0]] = keyValPair[1]
        }

        expectedParams.each { param, value ->
            if (value instanceof List<String>) {
                assert actualParams[param] == value[-1]
            } else {
                assert actualParams[param] == value.toString()
            }
        }
    }
}
