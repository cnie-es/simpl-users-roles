package eu.europa.ec.simpl.usersroles.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class JsonUtils {

    private static final ObjectMapper om = new ObjectMapper();

    private JsonUtils() {
        // no-op
    }

    /**
     * Converts the given String into a JSON string literal.
     * <p>
     * For example, the input {@code hello} will be serialized as {@code "hello"}.
     * <p>
     * Note: null input returns null. An exception is thrown only if a serious
     * unexpected issue occurs with the JSON library (should never happen).
     *
     * @param value the String to convert to JSON
     * @return the JSON string literal representing the input, or null if input is null
     */
    @SneakyThrows(JsonProcessingException.class)
    public static String toJsonString(String value) {
        log.trace("Processing input value: {}", value);
        if (value == null) {
            return null;
        }
        return om.writeValueAsString(value);
    }
}
