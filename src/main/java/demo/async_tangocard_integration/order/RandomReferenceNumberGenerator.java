package demo.async_tangocard_integration.order;

import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang.StringUtils;

import java.util.Arrays;

public class RandomReferenceNumberGenerator implements ReferenceNumberGenerator {
    
    public String generate() {
        // Generate a random reference number that looks like 0000-0000-000000.
        // Normally you might need to check this against the database to ensure there are no collisions
        // but we are going to assume collisions are rare and won't mess anything up.
        return StringUtils.join(
            Arrays.asList(
                RandomStringUtils.random(4, false, true),
                RandomStringUtils.random(4, false, true),
                RandomStringUtils.random(6, false, true)
                ),
            "-"
        );
    }
}
