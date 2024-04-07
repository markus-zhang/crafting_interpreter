package com.craftinginterpreters.lox;

public class RuntimeError extends RuntimeException {
    /**
     * Note that the token is embedded in this class,
     * which makes error reporting in runtimeError() in Lox class easier
     */
    final Token token;

    RuntimeError(Token token, String message) {
        super(message);
        this.token = token;
    }
}
