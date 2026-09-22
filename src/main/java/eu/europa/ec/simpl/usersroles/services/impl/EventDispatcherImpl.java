package eu.europa.ec.simpl.usersroles.services.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.europa.ec.simpl.common.constants.Topics;
import eu.europa.ec.simpl.common.messaging.OnMessageEvent;
import eu.europa.ec.simpl.events.usersroles.v1.AssignedIdentityAttributesUpdatedEvent;
import eu.europa.ec.simpl.events.usersroles.v1.IdentityAttribute;
import eu.europa.ec.simpl.events.usersroles.v1.OnboardingRequestDeletedEvent;
import eu.europa.ec.simpl.usersroles.properties.KafkaProperties;
import eu.europa.ec.simpl.usersroles.services.EventDispatcher;
import eu.europa.ec.simpl.usersroles.services.IdentityAttributeService;
import eu.europa.ec.simpl.usersroles.services.UserService;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EventDispatcherImpl implements EventDispatcher {

    private final ObjectMapper objectMapper;
    private final KafkaProperties kafkaProperties;
    private final UserService userService;
    private final IdentityAttributeService identityAttributeService;

    @Override
    @EventListener
    public void onOnboardingRequestDeleteEvent(OnMessageEvent event) {
        if (event.getTopic()
                .equals("%s%s"
                        .formatted(
                                kafkaProperties.topic().prefix(),
                                Topics.ONBOARDING_ONBOARDING_REQUEST_DELETED_EVENT))) {
            var onboardingRequestDeletedEvent = parseJson(event.getMessage(), OnboardingRequestDeletedEvent.class);
            userService.deleteByEmail(onboardingRequestDeletedEvent.getApplicantEmail());
        }
    }

    @Override
    @EventListener
    public void onAuthenticationProviderIdentityAttributesChangedEvent(OnMessageEvent event) {
        if (event.getTopic()
                .equals("%s%s"
                        .formatted(
                                kafkaProperties.topic().prefix(),
                                Topics.AUTHENTICATION_PROVIDER_IDENTITY_ATTRIBUTES_CHANGED_EVENT))) {

            var assignedIdentityAttributesUpdatedEvent =
                    parseJson(event.getMessage(), AssignedIdentityAttributesUpdatedEvent.class);
            identityAttributeService.assignIdentityAttributes(
                    assignedIdentityAttributesUpdatedEvent.getAssignedIdentityAttributes().stream()
                            .filter(IdentityAttribute::getAssignableToRoles)
                            .map(IdentityAttribute::getCode)
                            .toList());
        }
    }

    @SneakyThrows
    private <T> T parseJson(String message, Class<T> clazz) {
        return objectMapper.readValue(message, clazz);
    }
}
