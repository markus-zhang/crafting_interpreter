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
                    -> ifStmt ;
                    -> whileStmt ;
                    -> forStmt ;
                    -> breakStmt ;
                    -> continueStmt;
                    -> block ;
    exprStmt        -> expression ";" ;
    ifStmt          -> "if" "(" expression ")" statement
                       ("else" statement)?
    whileStmt       -> "while" "(" expression ")" statement
    forStmt         -> "for" "(" (varDecl | assignment | ";")
                       expression? ";"
                       expression? ")"
                       statement
    printStmt       -> "print" expression ";" ;
    breakStmt       -> "break" ";" ;
    continueStmt    -> "continue" ";" ;
    // In block, we use declaration instead of statement as varDecl can also live in blocks
    block           -> "{" (declaration)* "}"
    expression      -> assignment ;
    // assignment is the lowest expression thus it's at the top of expression
    assignment      -> IDENTIFIER "=" assignment ;
    assignment      -> logical_or ;
    logical_or      -> logical_and ("or" logical_and)* ;
    logical_and     -> equality ("and" equality)* ;
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

    Expr parseExpression() {
        try {
            // expression is top level
            return expression();
        }
        catch(ParseError error) {
            // Instead of crashing or hanging, simply return nothing
            return null;
        }
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
        else if (match(LEFT_BRACE)) {
            return new Stmt.Block(block());
        }
        else if (match(IF)) {
            return ifStatement();
        }
        else if (match(WHILE)) {
            return whileStatement();
        }
        else if (match(FOR)) {
            return forStatement();
        }
        else if (match(BREAK)) {
            return breakStatement();
        }
        else if (match(CONTINUE)) {
            return continueStatement();
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

    private Stmt ifStatement() {
        // Recall that the node has 3 children:
        // condition, thenBranch and elseBranch
        consume(LEFT_PAREN, "Expect '(' after if.");
        Expr condition = expression();
        consume(RIGHT_PAREN, "Expect ')' after the condition expression.");

        // We don't need to worry about block as statement is the parent of block.
        // If the statement starts with LEFT_PAREN, then it automatically return new Stmt.Block(block());
        // Recall that Block simply extends Stmt so it still matches the type of thenBranch or elseBranch
        Stmt thenBranch = statement();
        Stmt elseBranch = null;

        if (match(ELSE)) {
            elseBranch = statement();
        }

        return new Stmt.If(condition, thenBranch, elseBranch);
    }

    /**
     * whileStmt       -> "while" "(" expression ")" statement
     * @return: Stmt
     */
    private Stmt whileStatement() {
        consume(LEFT_PAREN, "Expect '(' after while.");
        Expr condition = expression();
        consume(RIGHT_PAREN, "Expect ')' after the condition expressions.");

        Stmt body = statement();

        return new Stmt.While(condition, body);
    }

    /**
     * forStmt         -> "for" "(" (varDecl | exprStmt | ";")
     *                        expression? ";"
     *                        expression? ")"
     *                        statement
     * @return: Stmt
     */
    private Stmt forStatement() {
        consume(LEFT_PAREN, "Expect '(' after for.");
        Stmt initializer = null;
        Expr condition = null;
        Expr increment = null;
        Stmt body = null;

        // all components could be null (if all are null, it's a dead loop)

        if (peek().type != SEMICOLON) {
            if (match(VAR)) {
                initializer = varDeclaration();
            }
            else {
                initializer = expressionStatement();
            }
        }
        else {
        consume(SEMICOLON, "Expect ';' after initializer.");
        }

        if (peek().type != SEMICOLON) {
            condition = expression();
        }
        consume(SEMICOLON, "Expect ';' after condition.");

        if (peek().type != RIGHT_PAREN) {
            increment = expression();
        }
        consume(RIGHT_PAREN, "Expect ';' after condition.");

//        if (peek().type == SEMICOLON) {
//            advance();
//        }
//        else if (peek().type != RIGHT_PAREN) {
//            condition = expression();
//            consume(SEMICOLON, "Expect ';' after condition.");
//            if (peek().type != RIGHT_PAREN) {
//                increment = expression();
//            }
//        }

//        consume(RIGHT_PAREN, "Expect ')' at the end of for loop.");

        body = statement();

        return new Stmt.For(initializer, condition, increment, body);
    }

    private Stmt printStatement() {
        Expr value = expression();
        consume(SEMICOLON, "Expect ';' after statement.");
        return new Stmt.Print(value);
    }

    private Stmt breakStatement() {
        consume(SEMICOLON, "Expect ';' after statement.");
        return new Stmt.Break(null);
    }

    private Stmt continueStatement() {
        consume(SEMICOLON, "Expect ';' after statement.");
        return new Stmt.Continue(null);
    }

    private List<Stmt> block() {
        List<Stmt> statements = new ArrayList<>();

        while (!check(RIGHT_BRACE) && !isAtEnd()) {
            statements.add(declaration());
        }

        consume(RIGHT_BRACE, "Expect ')' after block.");
        return statements;
    }

    private Expr expression() {
        return assignment();
    }

    /**
     * Consider the following lines:
     * var a = 5
     * a = 6
     * At the second line, we do NOT necessarily evaluate a (which is 5 before the assignment).
     * We only need to figure out where to store the value of 6.
     * In this case (line 2), this is a l-value (evaluates to a storage location)
     * ------------------------------------------------------------------------------
     * We want the AST to reflect that an l-value isn't evaluated like a normal expression, thus the Expr.Assign node has a Token as the LHS instead of an Expr.
     * The problem is the parser does not know it is parsing an l-value until it hits the =, which may occur many tokens later:
     * makeList().head.next = node;
     */
    private Expr assignment() {
        // assignment      -> IDENTIFIER "=" assignment
        // assignment      -> logical_or
        Expr expr = logical_or();

        if (match(EQUAL)) {
            Token equals = previous();
            // Assignment is right-associative -> recursively call assignment() to parse the RHS
            Expr value = assignment();

            if (expr instanceof Expr.Variable) {
                /*
                 * This conversion (from Expr to Expr.Variable) works because every valid assignment target happens to also be valid syntax as a normal expression.
                 * Consider this:
                 * a = 3;
                 * a definitely can be cast to a Token
                 * a.b.c = 3;
                 * a.b.c definitely can be cast to a Token
                 * a + b  = 3;
                 * The LHS cannot be cast to a Token, thus would trigger an error
                 *
                 * Right now, the only valid target is a simple variable expression, but we will add fields later. You can use ChatGPT to confirm that actually none of the other types can be cast to an Expr.Variable, so it's kinda useless, but with more types added in the future this could be useful
                 */
                Token name = ((Expr.Variable)expr).name;
                return new Expr.Assign(name, value);
            }

            error(equals, "Invalid assignment target.");
        }
        return expr;
    }

    private Expr logical_or() {
        // logical_or      -> logical_and ("or" logical_and)*
        Expr expr = logical_and();
        while (match(OR)) {
            Token operator = previous();
            Expr right = logical_and();
            expr = new Expr.Logical(expr, operator, right);
        }
        return expr;
    }

    private Expr logical_and() {
        // logical_and     -> equality ("and" equality)*
        Expr expr = equality();
        while (match(AND)) {
            Token operator = previous();
            Expr right = equality();
            expr = new Expr.Logical(expr, operator, right);
        }
        return expr;
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

    private boolean check(TokenType type) {
        if (isAtEnd()) {
            return false;
        }
        return peek().type == type;
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
