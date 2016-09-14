package edu.oregonstate.mist.locations.frontend

import edu.oregonstate.mist.api.AuthenticatedUser
import edu.oregonstate.mist.locations.frontend.db.LocationDAO
import edu.oregonstate.mist.locations.frontend.resources.LocationResource
import groovy.mock.interceptor.MockFor
import org.junit.Test

class LocationResourceTest {
    static def user = new AuthenticatedUser('nobody')

    @Test
    public void testNoResults() {
        def mock = new MockFor(LocationDAO)
        mock.demand.search() {
            String q, String campus, String type, Integer pageNumber, Integer pageSize ->
                '{"hits": {"total": 0, "hits": []}}'
        }

        def dao = mock.proxyInstance()
        def resource = new LocationResource(dao)
        resource.uriInfo = new MockUriInfo()

        def response = resource.list('dixon', null, null, user)
        assert response.status == 200
        assert response.entity.links == [:]
        assert response.entity.data == []

        mock.verify(dao)
    }

    @Test
    public void testSanitize() {
        assert LocationResource.sanitize("Valley[!#]Library") == "Valley    Library"
        assert !LocationResource.sanitize(null)
    }
}
