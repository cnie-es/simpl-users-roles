package eu.europa.ec.simpl.usersroles.interceptors;

import eu.europa.ec.simpl.client.core.suppliers.AuthorizationHeaderSupplier;
import eu.europa.ec.simpl.common.aspects.Masked;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Optional;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;

/**
 * <p>TierOneTokenPropagatorInterceptor class.</p>
 *
 */
@Component
public class TierOneTokenPropagatorInterceptor implements AuthorizationHeaderSupplier {

    private final HttpServletRequest currentRequest;

    /**
     * <p>Constructor for TierOneTokenPropagatorInterceptor.</p>
     *
     * @param currentRequest a object
     */
    public TierOneTokenPropagatorInterceptor(HttpServletRequest currentRequest) {
        this.currentRequest = currentRequest;
    }

    /** {@inheritDoc} */
    @Masked
    @Override
    public String get() {
        return Optional.ofNullable(RequestContextHolder.getRequestAttributes())
                .map(i -> currentRequest.getHeader(HttpHeaders.AUTHORIZATION))
                .orElse(null);
    }
}
