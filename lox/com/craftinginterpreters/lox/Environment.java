package com.craftinginterpreters.lox;

import java.util.HashMap;
import java.util.Map;

class Environment {
    private final Map<String, Object> values = new HashMap<>();

    void define(String name, Object value) {
        // This is define AND redefine,
        // value of the same name can be modified.
        // This is allowed, because it's easy for REPL users:
        // var meal = "Oriental";
        // var meal = "Western";
        // Technically, it's weird to have it in non-REPL, though
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
}
