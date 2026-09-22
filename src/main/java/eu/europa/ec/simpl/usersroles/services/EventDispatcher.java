package eu.europa.ec.simpl.usersroles.services;

import eu.europa.ec.simpl.common.messaging.OnMessageEvent;
import org.springframework.context.event.EventListener;

public interface EventDispatcher {
    @EventListener
    void onOnboardingRequestDeleteEvent(OnMessageEvent event);

    @EventListener
    void onAuthenticationProviderIdentityAttributesChangedEvent(OnMessageEvent event);
}
