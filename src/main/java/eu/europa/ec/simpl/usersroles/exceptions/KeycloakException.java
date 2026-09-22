package eu.europa.ec.simpl.usersroles.exceptions;

import eu.europa.ec.simpl.common.exceptions.StatusException;
import eu.europa.ec.simpl.usersroles.dto.KeycloakErrorDTO;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.core.Response;
import java.util.Optional;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpStatus;

@Log4j2
public class KeycloakException extends StatusException {
    public KeycloakException(Response response, Exception e) {
        super(
                getErrorStatusOrThrow(response),
                Optional.ofNullable(readErrorDTO(response))
                        .map(KeycloakErrorDTO::getErrorMessage)
                        .orElse("Unknown error"),
                e);
    }

    public KeycloakException(Response response) {
        this(response, null);
    }

    private static KeycloakErrorDTO readErrorDTO(Response response) {
        try {
            return response.readEntity(KeycloakErrorDTO.class);
        } catch (ProcessingException e) {
            log.warn("Unable to read the entity", e);
            return null;
        }
    }

    private static HttpStatus getErrorStatusOrThrow(Response response) {
        var status = HttpStatus.valueOf(response.getStatus());
        if (!status.isError()) {
            throw new IllegalArgumentException(
                    "Response with status %d is not an error response".formatted(response.getStatus()));
        }
        return status;
    }
}
