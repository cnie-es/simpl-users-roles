package eu.europa.ec.simpl.usersroles.services.impl;

import static eu.europa.ec.simpl.common.test.TestUtil.aString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.europa.ec.simpl.common.constants.Topics;
import eu.europa.ec.simpl.common.messaging.OnMessageEvent;
import eu.europa.ec.simpl.events.usersroles.v1.AssignedIdentityAttributesUpdatedEvent;
import eu.europa.ec.simpl.events.usersroles.v1.OnboardingRequestDeletedEvent;
import eu.europa.ec.simpl.usersroles.properties.KafkaProperties;
import eu.europa.ec.simpl.usersroles.services.IdentityAttributeService;
import eu.europa.ec.simpl.usersroles.services.UserService;
import org.instancio.Instancio;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.BDDMockito;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EventDispatcherImplTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    KafkaProperties kafkaProperties;

    @Mock
    ObjectMapper objectMapper;

    @Mock
    UserService userService;

    @Mock
    IdentityAttributeService identityAttributeService;

    @InjectMocks
    EventDispatcherImpl eventDispatcher;

    @Test
    void testOnOnboardingRequestDeleteEvent() throws JsonProcessingException {
        var event = BDDMockito.mock(OnMessageEvent.class);
        var prefix = "aPrefix";
        given(kafkaProperties.topic().prefix()).willReturn(prefix);
        given(event.getTopic()).willReturn(prefix + Topics.ONBOARDING_ONBOARDING_REQUEST_DELETED_EVENT);
        given(event.getMessage()).willReturn(aString());
        given(objectMapper.readValue(anyString(), eq(OnboardingRequestDeletedEvent.class)))
                .willReturn(Instancio.create(OnboardingRequestDeletedEvent.class));
        eventDispatcher.onOnboardingRequestDeleteEvent(event);
        then(userService).should().deleteByEmail(any());
    }

    @Test
    void testOnAuthenticationProviderIdentityAttributesChangedEvent() throws JsonProcessingException {
        var event = BDDMockito.mock(OnMessageEvent.class);
        var prefix = "aPrefix";
        given(kafkaProperties.topic().prefix()).willReturn(prefix);
        given(event.getTopic()).willReturn(prefix + Topics.AUTHENTICATION_PROVIDER_IDENTITY_ATTRIBUTES_CHANGED_EVENT);
        given(event.getMessage()).willReturn(aString());
        given(objectMapper.readValue(anyString(), eq(AssignedIdentityAttributesUpdatedEvent.class)))
                .willReturn(Instancio.create(AssignedIdentityAttributesUpdatedEvent.class));
        eventDispatcher.onAuthenticationProviderIdentityAttributesChangedEvent(event);
        then(identityAttributeService).should().assignIdentityAttributes(any());
    }
}
