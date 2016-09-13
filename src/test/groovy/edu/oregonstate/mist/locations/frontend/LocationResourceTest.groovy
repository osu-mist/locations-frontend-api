package edu.oregonstate.mist.locations.frontend

import edu.oregonstate.mist.locations.frontend.db.LocationDAO
import edu.oregonstate.mist.locations.frontend.resources.LocationResource
import io.dropwizard.testing.junit.ResourceTestRule
import org.junit.ClassRule
import org.junit.Test
import static org.mockito.Mockito.*

class LocationResourceTest {

    private static final LocationDAO locationDAO = mock(LocationDAO.class)

    @ClassRule
    public static final ResourceTestRule locationResource = ResourceTestRule.builder()
            .addResource(new LocationResource(locationDAO))
            .build()

    @Test
    public void testSanitize() {
        assert LocationResource.sanitize("Valley[!#]Library") == "Valley    Library"
        assert !LocationResource.sanitize(null)
    }
}
