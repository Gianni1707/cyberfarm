package com.cyberfarm.energy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

class TeamsTest {
    /** A team mod's API hands back hidden implementation classes: the call goes through the public interface. */
    @Test
    void callsReachMethodsOfHiddenClassesThroughTheirInterfaces() throws ReflectiveOperationException {
        List<String> hidden = Collections.unmodifiableList(List.of("a", "b"));
        assertEquals(2, Teams.call(hidden, "size", new Class<?>[0]));
        assertEquals("b", Teams.call(hidden, "get", new Class<?>[] {int.class}, 1));
        assertThrows(NoSuchMethodException.class, () -> Teams.call(hidden, "noSuchMethod", new Class<?>[0]));
    }
}
