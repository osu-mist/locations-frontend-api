package edu.oregonstate.mist.locations.frontend

import edu.oregonstate.mist.locations.frontend.db.LocationDAO
import edu.oregonstate.mist.locations.frontend.resources.LocationResource
import groovy.mock.interceptor.MockFor
import io.dropwizard.testing.junit.DropwizardAppRule
import org.junit.Test
import org.junit.ClassRule

import javax.ws.rs.core.UriBuilder

class LocationResourceTest {
    static URI endpointUri = UriBuilder.fromPath('https://api.unit.test.edu/v1/').build()

    @ClassRule
    public static final DropwizardAppRule<LocationsFrontendConfiguration> APPLICATION =
            new DropwizardAppRule<LocationsFrontendConfiguration>(
                    LocationsFrontEndApplication.class,
                    new File("configuration.yaml").absolutePath)
    // Test: LocationResource.list()

    @Test
    public void testList() {
        def mock = new MockFor(LocationDAO)
        mock.demand.search() {
            String q, String campus, String type, Double lat,
            Double lon, String searchDistance, Boolean isOpen,
            Integer pageNumber, Integer pageSize ->
                '{"hits": {"total": 0, "hits": []}}'
        }
        def dao = mock.proxyInstance()
        def resource = new LocationResource(dao, endpointUri)
        resource.uriInfo = new MockUriInfo()

        // Test: no result
        def noResultRsp = resource.list('dixon', null,
                null, null, null, null, null, null)
        assert noResultRsp.status == 200
        assert noResultRsp.entity.links == [:]
        assert noResultRsp.entity.data == []

        // Test: invalid campus
        def invalidCampRes = resource.list('dixon', 'invalid',
                null, null, null, null, null, null)
        assert invalidCampRes.status == 404
        assert invalidCampRes.entity.developerMessage.contains("Not Found")
        assert invalidCampRes.entity.userMessage.contains("Not Found")
        assert invalidCampRes.entity.code == 1404

        mock.verify(dao)
    }

    // Test: LocationResource.getById(): valid ID
    @Test
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
    @Test
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
}
