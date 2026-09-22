package eu.europa.ec.simpl.usersroles.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "simpl.kafka")
public record KafkaProperties(Topic topic, Inbox inbox) {
    /**
     * @param prefix Prefix applied to all Kafka topics
     */
    public record Topic(String prefix) {}

    public record Inbox(Topic topic) {
        /**
         * @param pattern Regex pattern used to match Kafka inbox topics
         */
        public record Topic(String pattern) {}
    }
}
