package eu.europa.ec.simpl.usersroles.services.output;

import org.springframework.http.HttpStatus;

public record KeyCloakOutcome(HttpStatus httpStatus, String error) {}
