package edu.oregonstate.mist.locations

import edu.oregonstate.mist.locations.frontend.core.Sample
import org.junit.Test
import static org.junit.Assert.*

class SampleTest {
    @Test
    public void testSample() {
        assertTrue(new Sample().message == 'hello world')
    }
}
