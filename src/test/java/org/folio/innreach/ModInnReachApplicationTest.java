package org.folio.innreach;

import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertThrows;

class ModInnReachApplicationTest {

    @Test
    void exceptionOnMissingSystemUserPassword() {
        var e = assertThrows(IllegalArgumentException.class, () -> ModInnReachApplication.main(null));
        assertThat(e.getMessage(), containsString(ModInnReachApplication.SYSTEM_USER_PASSWORD));
    }

}
