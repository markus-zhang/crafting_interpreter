package com.craftinginterpreters.lox;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.craftinginterpreters.lox.TokenType.*;

class Scanner {
    private final String source;
    private final String[] lines;
    private final List<Token> tokens = new ArrayList<>();
    private int start = 0;
    private int current = 0;
    private int line = 0;

    private int startLine = line;
    private int column = 0;

    private int startColumn = column;
    private char prevChar = '\0';

    private static final Map<String, TokenType> keywords;

    static {
        keywords = new HashMap<>();
        keywords.put("and",     AND);
        keywords.put("class",   CLASS);
        keywords.put("else",    ELSE);
        keywords.put("false",   FALSE);
        keywords.put("for",     FOR);
        keywords.put("fun",     FUN);
        keywords.put("if",      IF);
        keywords.put("nil",     NIL);
        keywords.put("or",      OR);
        keywords.put("print",   PRINT);
        keywords.put("return",  RETURN);
        keywords.put("super",   SUPER);
        keywords.put("this",    THIS);
        keywords.put("true",    TRUE);
        keywords.put("var",     VAR);
        keywords.put("while",   WHILE);
        keywords.put("break",   BREAK);
    }

    Scanner(String source) {
        this.source = source;
        this.lines = source.split("\n");
    }

    List<Token> scanTokens() {
        while (!isAtEnd()) {
            start = current;
            scanToken();
        }
        tokens.add(new Token(EOF, "", null, line, column + 1));
        return tokens;
    }

    private boolean isAtEnd() {
        return current >= source.length();
    }

    private char advance() {
        char currentChar = source.charAt(current++);
        if (prevChar == '\n') {
            line ++;
            // For convenience, we set first column as 1, not 0
            column = 1;
        }
        else {
            column ++;
        }
        prevChar = currentChar;
        return currentChar;
    }

    private void addToken(TokenType type, Object literal, int startLine, int startColumn) {
        String text = source.substring(start, current);
        tokens.add(new Token(type, text, literal, startLine, startColumn));
    }

    private void scanToken() {
        char c = advance();
        startColumn = column;
        startLine = line;
        switch (c) {
            case '(': addToken(LEFT_PAREN, null, line, column); break;
            case ')': addToken(RIGHT_PAREN, null, line, column); break;
            case '{': addToken(LEFT_BRACE, null, line, column); break;
            case '}': addToken(RIGHT_BRACE, null, line, column); break;
            case ',': addToken(COMMA, null, line, column); break;
            case '.': addToken(DOT, null, line, column); break;
            case '-': addToken(MINUS, null, line, column); break;
            case '+': addToken(PLUS, null, line, column); break;
            case ';': addToken(SEMICOLON, null, line, column); break;
            case '*': addToken(STAR, null, line, column); break;
            case '!':
                if (match('=')) {
                    addToken(BANG_EQUAL, null, line, startColumn);
                }
                else {
                    addToken(BANG, null, line, startColumn);
                }
                break;
            case '=':
                if (match('=')) {
                    addToken(EQUAL_EQUAL, null, line, startColumn);
                }
                else {
                    addToken(EQUAL, null, line, startColumn);
                }
                break;
            case '<':
                if (match('=')) {
                    addToken(LESS_EQUAL, null, line, startColumn);
                }
                else {
                    addToken(LESS, null, line, startColumn);
                }
                break;
            case '>':
                if (match('=')) {
                    addToken(GREATER_EQUAL, null, line, startColumn);
                }
                else {
                    addToken(GREATER, null, line, startColumn);
                }
                break;
            case '/':
                if (match('/')) {
                    // Single line comment, skip till next line
                    while (peek() != '\n' && !isAtEnd()) {
                        advance();
                    }
                }
                else {
                    addToken(SLASH, null, line, startColumn);
                }
                break;
            case ' ':
            case '\r':
            case '\t':
                // Skip all spaces
                break;
            case '\n':
                // advance() already increments line
                // line++;
                break;
            case '"': getString(); break;
            default:
                // Numbers
                if (isDigit(c)) {
                    getNumber();
                }
                else if (isAlpha(c)) {
                    getIdentifier();
                }
                else {
                    Lox.error(line, column, lines[line], "Unexpected character.");
                }
                break;
        }
    }

    private boolean match(char expected) {
        if (isAtEnd()) return false;
        if (source.charAt(current) != expected) return false;
        // Once true addToken() consumes two characters so need to advance to next char
        current ++;
        // set prevChar
        prevChar = expected;
        // prevChar is not '\n' in this function so we simply increment
        column ++;
        return true;
    }

    private char peek() {
        if (isAtEnd()) return '\0';
        return source.charAt(current);
    }

    private boolean isDigit(char c) {
        return (c >= '0' && c <= '9');
    }

    private boolean isAlpha(char c) {
        return (c == '_' || (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z'));
    }

    private void getString() {
        while (peek() != '"' && !isAtEnd()) {
            advance();
        }

        if (isAtEnd()) {
            Lox.error(line, column, "", "Unterminated string.");
            return;
        }

        // Consume closing quote
        advance();

        // Trim the quotes
        String s = source.substring(start + 1, current - 1);
        addToken(STRING, s, startLine, startColumn);
    }

    private void getNumber() {
        boolean isFloat = false;
        while (isDigit(peek()) || peek() == '.') {
            if (isDigit(peek())) advance();
            else if (peek() == '.') {
                if (isFloat) {
                    Lox.error(line, column, lines[line], "Multiple decimal points.");
                    return;
                }
                else {
                    isFloat = true;
                    advance();
                }
            }
        }

        String s = source.substring(start, current);
        addToken(NUMBER, Double.parseDouble(s), startLine, startColumn);
    }

    private void getIdentifier() {
        while (isAlpha(peek()) || isDigit(peek())) {
            advance();
        }

        String s = source.substring(start, current);
        TokenType t = keywords.get(s);
        if (t == null) {
            addToken(IDENTIFIER, null, startLine, startColumn);
        }
        else {
            addToken(t, null, startLine, startColumn);
        }
    }
}
