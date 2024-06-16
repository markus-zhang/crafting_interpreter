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
    private static Interpreter interpreter = null;
    static boolean hadError = false;
    static boolean hadRuntimeError = false;
    private static String sourceCode = "";
    static boolean repl = false;

    public static void main(String[] args) throws IOException {
        if (args.length > 1) {
            System.out.println("Usage: jlox [script]");
            System.exit(64);
        }
        else if (args.length == 1) {
            repl = false;
            runFile(args[0]);
        }
        else {
            repl = true;
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
        // If runtime error then exit
        if (hadRuntimeError) {
            System.exit(70);
        }
    }

    private static void runPrompt() throws IOException {
        InputStreamReader input = new InputStreamReader(System.in);
        BufferedReader reader = new BufferedReader(input);

        // Display > at the end of loop so that errors don't FOLLOW the next >
        // Instead errors should precede the next >
        System.out.print("> ");
        for (;;) {
            // System.out.print("> ");
            String line = reader.readLine();
            if (line == null) {
                break;
            }
            run(line);
            // Reset hadError to prevent killing the REPL
            hadError = false;
            System.out.print("> ");
        }
    }

    private static void run(String source) {
        sourceCode = source;
        Scanner scanner = new Scanner(source);
        List<Token> tokens = scanner.scanTokens();
        System.out.println("Tokenizer completes its running.");

        /** If we are in REPL mode, we should notify the parser so that it can also parse expressions.
         Right now we only allow a single line of code in REPL as we use readLine()
         We need to switch to other reading methods if we want to allow very flexible REPL, such as detecting whether we should execute or simply move to next line when user clicks "Enter"
         */

        Parser parser = new Parser(tokens, sourceCode);
        interpreter = new Interpreter(sourceCode);

        if (!repl) {
            List<Stmt> statements = parser.parse();

            if (hadError) {
                return;
            }
            // System.out.println(new AstPrinter().print(expression));

            interpreter.interpret(statements);

        }
        else {
            // REPL mode
            if (tokens.get(tokens.size() - 2).lexeme.equals(";")) {
                List<Stmt> statements = parser.parse();

                if (hadError) {
                    return;
                }
                System.out.println("Parser completes its running.");

                interpreter.interpret(statements);

            }
            else {
                Expr expr = parser.parseExpression();

                if (hadError) {
                    return;
                }
                System.out.println("Parser completes its running.");

                interpreter.interpret(expr);

            }
        }
        if (hadRuntimeError) {
            return;
        }
    }

    static void error(int lineNumber, int column, String line, String message) {
        report(lineNumber, column, line, "", message);
    }

    /**
     *  We put all error reporting code in Lox, whether it is from the scanner or the parser or the interpreter.
     */
    static void error(Token token, String message) {
        if (token.type == TokenType.EOF) {
            report(token.line, token.column, "", "at end", message);
        }
        else {
            report(token.line, token.column, "", " at '" + token.lexeme + "'", message);
        }
    }

    static void runtimeError(RuntimeError err) {
        /**
         * interpret() in the Interpreter class catches a RuntimeError, but we deal it in the Lox class
         */
        String line = sourceCode.split("\n")[err.token.line];
        // System.err.println(err.getMessage());
        System.err.println("[line " + err.token.line + "]");
        System.err.println("[column " + err.token.column + "]");
        report(err.token.line, err.token.column, line, "", err.getMessage());
        hadRuntimeError = true;
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

