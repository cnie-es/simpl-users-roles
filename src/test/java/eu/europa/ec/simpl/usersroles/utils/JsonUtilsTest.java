package eu.europa.ec.simpl.usersroles.utils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class JsonUtilsTest {

    static Stream<Arguments> testToJsonStringGivenValidStringWillReturnJsonString() {
        return Stream.of(
                arguments("simple-value", "\"simple-value\""),
                arguments("line-1\nline-2", "\"line-1\\nline-2\""),
                arguments("special-chars \\,\",", "\"special-chars \\\\,\\\",\""),
                arguments("", "\"\""),
                arguments(null, null));
    }

    @ParameterizedTest
    @MethodSource
    void testToJsonStringGivenValidStringWillReturnJsonString(String value, String expected) {
        var result = JsonUtils.toJsonString(value);
        assertThat(result).as("the output").isEqualTo(expected);
    }
}
