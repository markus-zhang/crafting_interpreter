package com.craftinginterpreters.lox;

import java.util.HashMap;
import java.util.Map;

class Environment {
    private final Map<String, Object> values = new HashMap<>();

    void define(String name, Object value) {
        /**
         * This is define AND redefine,
         * value of the same name can be modified.
         * This is allowed, because it's easy for REPL users:
         * var meal = "Oriental";
         * var meal = "Western";
         * Technically, it's weird to have it in non-REPL, though
         */
        values.put(name, value);
    }

    Object get(Token name) {
        if (values.containsKey(name.lexeme)) {
            return values.get(name.lexeme);
        }
        // See note 01
        throw new RuntimeError(
            name,
            "Undefined variable '" + name.lexeme + "'."
        );
    }

    void assign(Token name, Object value) {
        /**
         * If the map of values already contains it, then simply mutate the value,
         * otherwise throw an error -- looks like Lox does NOT allow assigning before definition
         */
        if (values.containsKey(name.lexeme)) {
            values.put(name.lexeme, value);
            return;
        }
        throw new RuntimeError(
                name,
                "Undefined variable '" + name.lexeme + "'."
        );
    }
}
