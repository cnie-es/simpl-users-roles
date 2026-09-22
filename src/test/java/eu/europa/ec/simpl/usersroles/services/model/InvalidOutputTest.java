package eu.europa.ec.simpl.usersroles.services.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class InvalidOutputTest {

    @Test
    void testNotAssignableToRoleToString() {
        InvalidOutput.NotAssignableToRole output = new InvalidOutput.NotAssignableToRole("CODE1");
        assertEquals("Identity attribute with code [ CODE1 ] is not assignable to a role", output.toString());
    }

    @Test
    void testNotAssignedToParticipantToString() {
        InvalidOutput.NotAssignedToParticipant output = new InvalidOutput.NotAssignedToParticipant("CODE2");
        assertEquals("Identity attribute with code [ CODE2 ] is not assigned to a participant", output.toString());
    }

    @Test
    void testNotEnabledToString() {
        InvalidOutput.NotEnabled output = new InvalidOutput.NotEnabled("CODE3");
        assertEquals("Identity attribute with code [ CODE3 ] is not enabled", output.toString());
    }

    @Test
    void testNotFoundToString() {
        InvalidOutput.NotFound output = new InvalidOutput.NotFound("CODE4");
        assertEquals("Identity attribute with code [ CODE4 ] is not found", output.toString());
    }
}
