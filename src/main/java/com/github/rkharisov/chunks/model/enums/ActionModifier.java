package com.github.rkharisov.chunks.model.enums;

import java.util.Optional;
import java.util.UUID;

public enum ActionModifier {

    MARK_REPEATED("/repeated"),
    MARK_DROP("/drop"),
    MARK_UNMUTATE("/unmutate"),
    GET_CHUNKS("/chunks"),
    GET_ACTIVITY("/activity");

    private UUID target;
    private final String alias;

    ActionModifier(String alias) {
        this.alias = alias;
    }

    public ActionModifier setTarget(UUID target) {
        this.target = target;
        return this;
    }

    public UUID getTarget() {
        return target;
    }

    public String getAlias() {
        return alias;
    }

    public static Optional<ActionModifier> defineAction(String action_) {
        for (ActionModifier value : ActionModifier.values()) {
            if(action_.startsWith(value.alias)) return Optional.of(value);
        }
        return Optional.empty();
    }
}
