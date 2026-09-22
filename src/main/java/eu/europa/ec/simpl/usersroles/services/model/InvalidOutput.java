package eu.europa.ec.simpl.usersroles.services.model;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public sealed interface InvalidOutput {
    record NotAssignableToRole(@Valid @NotBlank String code) implements InvalidOutput {
        @Override
        public @NotNull String toString() {
            return "Identity attribute with code [ %s ] is not assignable to a role".formatted(code);
        }
    }

    record NotAssignedToParticipant(@Valid @NotBlank String code) implements InvalidOutput {
        @Override
        public @NotNull String toString() {
            return "Identity attribute with code [ %s ] is not assigned to a participant".formatted(code);
        }
    }

    record NotEnabled(@Valid @NotBlank String code) implements InvalidOutput {
        @Override
        public @NotNull String toString() {
            return "Identity attribute with code [ %s ] is not enabled".formatted(code);
        }
    }

    record NotFound(@Valid @NotBlank String code) implements InvalidOutput {
        @Override
        public @NotNull String toString() {
            return "Identity attribute with code [ %s ] is not found".formatted(code);
        }
    }
}
