package com.craftinginterpreters.lox;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.Buffer;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class Lox {
    static boolean hadError = false;

    public static void main(String[] args) throws IOException {
        if (args.length > 1) {
            System.out.println("Usage: jlox [script]");
            System.exit(64);
        }
        else if (args.length == 1) {
            runFile(args[0]);
        }
        else {
            runPrompt();
        }
    }

    private static void runFile(String path) throws IOException {
        byte[] bytes = Files.readAllBytes(Paths.get(path));
        run(new String(bytes, Charset.defaultCharset()));

        // If error then exit
        if (hadError) {
            System.exit(65);
        }
    }

    private static void runPrompt() throws IOException {
        InputStreamReader input = new InputStreamReader(System.in);
        BufferedReader reader = new BufferedReader(input);

        for (;;) {
            System.out.print("> ");
            String line = reader.readLine();
            if (line == null) {
                break;
            }
            run(line);
            // Reset hadError to prevent killing the REPL
            hadError = false;
        }
    }

    private static void run(String source) {
        Scanner scanner = new Scanner(source);
        List<Token> tokens = scanner.scanTokens();

        // For now just print all tokens
        System.out.println("line:column type        lexeme      literal");
        for (Token token : tokens) {
            System.out.println(token);
        }
    }

    static void error(int lineNumber, int column, String line, String message) {
        report(lineNumber, column, line, "", message);
    }

    static void error(Token token, String message) {
        if (token.type == TokenType.EOF) {
            report(token.line, token.column, "", "at end", message);
        }
        else {
            report(token.line, token.column, "", " at '" + token.lexeme + "'", message);
        }
    }

    private static void report(int lineNumber, int column, String line, String where, String message) {
        System.err.println(
                "[line " + lineNumber + "] Error" + where + ": " + message
        );
        System.err.println(line);
        printCaret(column);
        hadError = true;
    }

//    private static void printLine(int line, String source) {
//        String[] lines = source.split("\n");
//        // Assuming line starts from 0
//        if (line >= 0 && line <= lines.length - 1) {
//            System.out.println(lines[line]);
//        }
//        else {
//            System.err.println("Error: Line " + line + " is not available in source file.");
//        }
//    }

    private static void printCaret(int column) {
        if (column < 0) {
            System.err.println("Error: Caret position is negative.");
        }
        System.err.print(" ".repeat(column));
        System.err.print("^");
        System.err.println();
    }
}

