package edu.oregonstate.mist.locations.frontend

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import edu.oregonstate.mist.locations.frontend.db.LocationDAO
import io.dropwizard.testing.junit.DropwizardAppRule
import org.junit.BeforeClass
import org.junit.ClassRule
import org.junit.Test

class LocationDAOTest {
    private static LocationDAO locationDAO

    private ObjectMapper mapper = new ObjectMapper()

    private final String SINGLE_RESOURCE_ID = "a5041ecde8b53e54c7479e770825d7c1"

    @ClassRule
    public static final DropwizardAppRule<LocationsFrontendConfiguration> APPLICATION =
            new DropwizardAppRule<LocationsFrontendConfiguration>(
                    LocationsFrontEndApplication.class,
                    new File("configuration.yaml").absolutePath)

    @BeforeClass
    public static void setUpClass() {
        locationDAO = new LocationDAO(APPLICATION.configuration.locationsConfiguration)
    }

    @Test
    public void testGetById() {
        assert locationDAO.getById(SINGLE_RESOURCE_ID) != null
        assert locationDAO.getById("invalid-id") == null
        assert locationDAO.getById(null) == null
    }

    @Test
    public void testCaseInsensitiveId() {
        def locationJSON = locationDAO.getById(SINGLE_RESOURCE_ID)
        assert locationJSON == locationDAO.getById(SINGLE_RESOURCE_ID.toLowerCase())
        assert locationJSON == locationDAO.getById(SINGLE_RESOURCE_ID.toUpperCase())
    }

    @Test
    public void testInvalidSearch() {
        def invalidCampus = locationDAO.search("hello world", "invalid-campus", null, 1, 10)
        assert invalidCampus != null
        assertNoHits(invalidCampus)

        def invalidType = locationDAO.search("hello world", null, "invalid-type", 1, 10)
        assert invalidType != null
        assertNoHits(invalidType)

        def invalidFilters = locationDAO.search("hello world", "invalid-campus", "invalid-type", 1, 10)
        assert invalidFilters != null
        assertNoHits(invalidFilters)
    }

    @Test
    public void testSearch() {
        def allDixon = locationDAO.search("dixon", null, null, 1, 10)
        assert allDixon != null
        JsonNode actualObj = mapper.readTree(allDixon)
        def allDixonCount = actualObj.get("hits").get("hits").size()
        assert allDixonCount == 3

        def diningDixon = locationDAO.search("dixon", null, "dining", 1, 10)
        assert diningDixon != null
        actualObj = mapper.readTree(diningDixon)
        def diningDixonCount = actualObj.get("hits").get("hits").size()
        assert diningDixonCount == 1

        def buildingDixon = locationDAO.search("dixon", null, "building", 1, 10)
        assert buildingDixon != null
        actualObj = mapper.readTree(buildingDixon)
        def buildingDixonCount = actualObj.get("hits").get("hits").size()
        assert buildingDixonCount == 2

        assert allDixonCount == (diningDixonCount + buildingDixonCount)
    }

    private void assertNoHits(String invalidCampus) {
        JsonNode actualObj = mapper.readTree(invalidCampus)
        assert actualObj.get("hits").get("hits").size() == 0
    }
}
