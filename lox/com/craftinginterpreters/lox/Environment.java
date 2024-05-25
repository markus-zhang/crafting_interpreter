package com.craftinginterpreters.lox;

import java.util.HashMap;
import java.util.Map;

class Environment {
    /** Each environment has a pointer pointing to its enclosing parent;
     *  See note 01 for more details about "Scope";
     *  The top environment (global) should have NULL as its "enclosing" member
     */
    final Environment enclosing;
    private final Map<String, Object> values = new HashMap<>();

    Environment() {
        enclosing = null;
    }

    Environment(Environment enclosing) {
        this.enclosing = enclosing;
    }

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
        /** We first try to find "name" in "self.values";
         *  If we cannot locate it we move up to its enclosing environment;
         *  Until we hit the global, then we report an error if we cannot locate it
         */
        if (values.containsKey(name.lexeme)) {
            return values.get(name.lexeme);
        }
        // This is pretty clever. We avoid recursion by simplu calling the enclosing environment's get(). If the first if {} block does return something non-null, the program stops above and wouldn't reach here
        if (enclosing != null) {
            return enclosing.get(name);
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
            // return is a MUST to avoid falling to the next statements
            return;
        }
        if (enclosing != null) {
            enclosing.assign(name, value);
            return;
        }
        throw new RuntimeError(
                name,
                "Undefined variable '" + name.lexeme + "'."
        );
    }
}
