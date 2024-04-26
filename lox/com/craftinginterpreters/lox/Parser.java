package com.craftinginterpreters.lox;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import static com.craftinginterpreters.lox.TokenType.*;

/*
    Grammar:

    // See note 00 for declaration
    program         -> declaration* EOF ;
    declaration     -> varDecl;
                    -> statement;
    [In varDecl, the ? means it's optional]
    varDecl         -> "var" IDENTIFIER ("=" expression)? ";" ;
    statement       -> exprStmt ;
                    -> printStmt ;
    exprStmt        -> expression ";" ;
    printStmt       -> "print" expression ";" ;
    expression      -> equality ;
    equality        -> comparison ("==" comparison)* ;
    equality        -> comparison ("!=" comparison)* ;
    comparison      -> term ("<=" term)* ;
    comparison      -> term ("<" term)* ;
    comparison      -> term (">=" term)* ;
    comparison      -> term (">" term)* ;
    term            -> factor ("+" factor)* ;
    term            -> factor ("-" factor)* ;
    factor          -> unary ("*" unary)* ;
    factor          -> unary ("/" unary)* ;
    unary           -> ("!" | "-") unary ;
    unary           -> primary;
    primary         -> NUMBER | STRING | "true" | "false" | "nil" | "(" expression ") | IDENTIFIER";
*/
public class Parser {
    private final String source;
    private static class ParseError extends RuntimeException {}
    private final List<Token> tokens;
    private int current = 0;

    Parser(List<Token> tokens, String source) {
        this.source = source;
        this.tokens = tokens;
    }

    List<Stmt> parse() {
        List<Stmt> statements = new ArrayList<>();
        while (!isAtEnd()) {
            statements.add(declaration());
        }
        return statements;
    }

    private Stmt declaration() {
        try {
            if (match(VAR)) {
                return varDeclaration();
            }
            return statement();
        // We put exception catching and synchronization in the top level
        } catch (ParseError error) {
            synchronize();
            return null;
        }
    }

    private Stmt statement() {
        if (match(PRINT)) {
            return printStatement();
        }
        return expressionStatement();
    }

    private Stmt varDeclaration() {
        Token name = consume(IDENTIFIER, "Expect a variable name.");
        Expr initializer = null;
        if (match(EQUAL)) {
            initializer = expression();
        }
        consume(SEMICOLON, "Expect ';' after a variable declaration");
        return new Stmt.Var(name, initializer);
    }

    private Stmt expressionStatement() {
        Expr value = expression();
        consume(SEMICOLON, "Expect ';' after value.");
        return new Stmt.Expression(value);
    }

    private Stmt printStatement() {
        Expr value = expression();
        consume(SEMICOLON, "Expect ';' after value.");
        return new Stmt.Print(value);
    }

    private Expr expression() {
        return equality();
    }

    private Expr equality() {
        // equality        -> comparison ("==" comparison)* ;
        // equality        -> comparison ("!=" comparison)* ;
        // ! Lox does NOT allow chained equalities (as Java, which evaluates 5 <= 6 == 6 as ERROR instead of true
        Expr expr = comparison();
        while (match(BANG_EQUAL, EQUAL_EQUAL)) {
            Token operator = previous();
            Expr right = comparison();
            expr = new Expr.Binary(expr, operator, right);
        }
        return expr;
    }

    private Expr comparison() {
        // comparison      -> term ("<=" term)* ;
        // comparison      -> term ("<" term)* ;
        // comparison      -> term (">=" term)* ;
        // comparison      -> term (">" term)* ;
        Expr expr = term();
        while (match(LESS_EQUAL, LESS, GREATER_EQUAL, GREATER)) {
            Token operator = previous();
            Expr right = term();
            expr = new Expr.Binary(expr, operator, right);
        }
        return expr;
    }

    private Expr term() {
        // term            -> factor ("+" factor)* ;
        // term            -> factor ("-" factor)* ;
        Expr expr = factor();
        while (match(PLUS, MINUS)) {
            Token operator = previous();
            Expr right = factor();
            expr = new Expr.Binary(expr, operator, right);
        }
        return expr;
    }

    private Expr factor() {
        // factor          -> unary ("*" unary)* ;
        // factor          -> unary ("/" unary)* ;
        Expr expr = unary();
        while (match(STAR, SLASH)) {
            Token operator = previous();
            Expr right = unary();
            expr = new Expr.Binary(expr, operator, right);
        }
        return expr;
    }

    private Expr unary() {
        // unary           -> ("!" | "-") unary ;
        // unary           -> primary;
        if (match(BANG, MINUS)) {
            Token operator = previous();
            Expr right = unary();
            return new Expr.Unary(operator, right);
        }
        else {
            return primary();
        }
    }

    private Expr primary() {
        // primary         -> NUMBER | STRING | "true" | "false" | "nil" | "(" expression ")";
        if (match(FALSE)) {
            return new Expr.Literal(false);
        }
        if (match(TRUE)) {
            return new Expr.Literal(true);
        }
        if (match(NIL)) {
            return new Expr.Literal(null);
        }
        if (match(NUMBER, STRING)) {
            // ! Recall that match() advances the pointer
            return new Expr.Literal(previous().literal);
        }
        if (match(LEFT_PAREN)) {
            Expr expr = expression();
            consume(RIGHT_PAREN, "Expect ')' after expression.");
            return new Expr.Grouping(expr);
        }
        if (match(IDENTIFIER)) {
            return new Expr.Variable(previous());
        }
        // If nothing matches then it's an error
        throw error(peek(), "Expect expression.");
    }

    private Token consume(TokenType type, String message) {
        Token t = tokens.get(current);
        TokenType t_type = t.type;
        if (t_type == type) {
            return advance();
        }
        throw error(peek(), message);
    }

    private ParseError error(Token token, String message) {
        // Lox.error(token, message);
        String line = source.split("\n")[token.line];
        Lox.error(token.line, token.column, line, message);
        return new ParseError();
    }

    private void synchronize() {
        /*
            When synchronizing the parser (after encountering a non-panicking parsing error, we simply find the Token that follows the next semicolon -- that is, perhaps the beginning of the next statement.

            We used "perhaps" as this function is not aware of whether the parser is inside, say, a for loop. In this case, the next statement is not the one after the next semicolon.
        */
        advance();
        while (!isAtEnd()) {
            if (previous().type == SEMICOLON) {
                return;
            }

            switch(peek().type) {
                // Valid statements
                case CLASS:
                case FOR:
                case FUN:
                case IF:
                case PRINT:
                case RETURN:
                case VAR:
                case WHILE:
                    return;
            }
        }
    }

    private boolean match(TokenType... types) {
        Token t = tokens.get(current);
        TokenType t_type = t.type;
        for (TokenType type : types) {
            if (type == t_type) {
                advance();
                return true;
            }
        }
        return false;
    }

    private Token advance() {
        if (!isAtEnd()) {
            current ++;
        }
        return previous();
    }

    private boolean isAtEnd() {
        // I'm using a slightly different index from the one used in the book,
        // thus I need to subtract size() by 1, otherwise it causes issues with EOF
        return current >= tokens.size() - 1;
    }

    private Token peek() {
        return tokens.get(current);
    }

    private Token previous() {
        return tokens.get(current - 1);
    }
}
