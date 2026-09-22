package eu.europa.ec.simpl.usersroles.configurations;

import eu.europa.ec.simpl.api.authenticationprovider.v1.exchanges.IdentityAttributesApi;
import eu.europa.ec.simpl.common.argumentresolvers.QueryParamsArgumentResolver;
import eu.europa.ec.simpl.usersroles.properties.MicroserviceProperties;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

@Configuration
@RequiredArgsConstructor
public class ClientConfig {
    private static final String V1_PREFIX = "/v1";

    private final MicroserviceProperties properties;
    private final RestClient.Builder restClientBuilder;

    @Bean
    public IdentityAttributesApi identityAttributesApi() {
        return buildExchange(
                properties.authenticationProvider().url().resolve(V1_PREFIX),
                restClientBuilder,
                IdentityAttributesApi.class);
    }

    private static <E> E buildExchange(URI baseurl, RestClient.Builder restClientBuilder, Class<E> clazz) {
        var restClient = restClientBuilder.baseUrl(baseurl).build();
        var adapter = RestClientAdapter.create(restClient);
        var factory = HttpServiceProxyFactory.builderFor(adapter)
                .customArgumentResolver(new QueryParamsArgumentResolver())
                .build();
        return factory.createClient(clazz);
    }
}
