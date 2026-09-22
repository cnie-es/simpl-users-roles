package eu.europa.ec.simpl.usersroles.interceptors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

@ExtendWith(MockitoExtension.class)
class TierOneTokenPropagatorInterceptorTest {

    @Mock
    private HttpServletRequest currentRequest;

    @InjectMocks
    private TierOneTokenPropagatorInterceptor interceptor;

    @Test
    void getTest() {
        try (var requestContextHolder = mockStatic(RequestContextHolder.class)) {
            var requestAttributes = mock(RequestAttributes.class);
            given(RequestContextHolder.getRequestAttributes()).willReturn(requestAttributes);
            assertDoesNotThrow(() -> interceptor.get());
        }
    }
}
