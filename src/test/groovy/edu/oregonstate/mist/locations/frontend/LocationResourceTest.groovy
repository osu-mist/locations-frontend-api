package edu.oregonstate.mist.locations.frontend

import edu.oregonstate.mist.locations.frontend.db.LocationDAO
import edu.oregonstate.mist.locations.frontend.resources.LocationResource
import groovy.mock.interceptor.Mockfor
import org.junit.Test

class LocationResourceTest {
    @Test
    public void testSanitize() {
        def mock = new MockFor(LocationDAO)


        assert LocationResource.sanitize("Valley[!#]Library") == "Valley    Library"
        assert !LocationResource.sanitize(null)
    }
}
