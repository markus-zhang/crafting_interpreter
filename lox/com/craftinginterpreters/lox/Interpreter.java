package com.craftinginterpreters.lox;
import java.sql.Statement;
import java.util.List;
import java.lang.Boolean;

import static com.craftinginterpreters.lox.TokenType.*;

class Interpreter implements    Expr.Visitor<Object>,
                                Stmt.Visitor<Void>  {
    private final String source;
    // private static class InterpretError extends RuntimeException {}
    private boolean breakSignal = false;
    private boolean continueSignal = false;

    // Adding an environment for IDENTIFIERs
    final Environment globals = new Environment();
    private Environment environment = globals;

    Interpreter(String source) {
        globals.define("clock", new LoxCallable() {

            }
        )
        this.source = source;
    }

    /**
     * We need to implement the visitXExpr functions;
     * Each function returns a Java Object as Lox is dynamically typed,
     * which means that a variable may be assigned to different Java types
     */
    void interpret(List<Stmt> statements) {
        try {
            for (Stmt statement : statements) {
                execute(statement);
            }
        } catch (RuntimeError error) {
            Lox.runtimeError(error);
        }
    }

    // For REPL expression
    void interpret(Expr expression) {
        try {
            Object value = evaluate(expression);
            System.out.println(stringify(value));
        } catch (RuntimeError error) {
            Lox.runtimeError(error);
        }
    }

    @Override
    public Object visitLiteralExpr(Expr.Literal expr) {
        return expr.value;
    }

    @Override
    public Object visitUnaryExpr(Expr.Unary expr) {
        Object right = evaluate(expr.right);
        if (expr.operator.type == MINUS) {
            checkNumberOperand(expr.operator, right);
            return -(double)right;
        }
        else if (expr.operator.type == BANG) {
            return !isTruthy(right);
        }
        // Next line should not be reachable
        return null;
    }

    @Override
    public Object visitBinaryExpr(Expr.Binary expr) {
        Object left = evaluate(expr.left);
        Object right = evaluate(expr.right);

        // In this stage we only consider numerical operands
        switch(expr.operator.type) {
            case PLUS:
                if (left instanceof Double && right instanceof Double) {
                    return (double) left + (double) right;
                }
                else if (left instanceof String && right instanceof String) {
                    return (String) left + (String) right;
                }
                // Challenge 7.2, p109
                else if (left instanceof String || right instanceof String) {
                    return left.toString() + right.toString();
                }
                // If it reaches here then the types are wrong
                throw new RuntimeError(expr.operator, "Both operands must be numbers or Strings");
            case MINUS:
                checkNumberOperands(expr.operator, left, right);
                return (double)left - (double)right;
            case STAR:
                checkNumberOperands(expr.operator, left, right);
                return (double)left * (double)right;
            case SLASH:
                checkNumberOperands(expr.operator, left, right);
                return (double)left / (double)right;
            case GREATER:
                checkNumberOperands(expr.operator, left, right);
                return (double)left > (double)right;
            case GREATER_EQUAL:
                checkNumberOperands(expr.operator, left, right);
                return (double)left >= (double)right;
            case LESS:
                checkNumberOperands(expr.operator, left, right);
                return (double)left < (double) right;
            case LESS_EQUAL:
                checkNumberOperands(expr.operator, left, right);
                return (double)left <= (double) right;
            case EQUAL_EQUAL:
                return isEqual(left, right);
            case BANG_EQUAL:
                return !isEqual(left, right);
        }
        // Next line should not be reachable
        return null;
    }

    // This one is for variable declaration (var a = "blah";)
    @Override
    public Void visitVarStmt(Stmt.Var stmt) {
        // If user did not initialize its value, it's set to nil
        Object value = null;
        if (stmt.initializer != null) {
            value = evaluate(stmt.initializer);
        }
        environment.define(stmt.name.lexeme, value);
        return null;
    }

    // This one is for variable expression (print(a);)
    @Override
    public Object visitVariableExpr(Expr.Variable expr) {
        return environment.get(expr.name);
    }

    /**
     * Evaluate the RHS and assign the result to LHS
     */
    @Override
    public Object visitAssignExpr(Expr.Assign expr) {
        Object value = evaluate(expr.value);
        environment.assign(expr.name, value);
        return value;
    }

    @Override
    public Object visitLogicalExpr(Expr.Logical expr) {
        Object left_value = evaluate(expr.left);
        boolean left_logical_value = isTruthy(left_value);
        // Short-circuit
        if (expr.operator.type == TokenType.OR) {
            if (left_logical_value) {
                return left_value;
            }
        }
        else if (expr.operator.type == TokenType.AND) {
            if (!left_logical_value) {
                return left_value;
            }
        }
        // If we cannot short-circuit, the previous value is of no concern to us.
        // The result is determined by the right side
        return evaluate(expr.right);
    }

    @Override
    public Void visitExpressionStmt(Stmt.Expression expressionStmt) {
        evaluate(expressionStmt.expression);
        return null;
    }

    @Override
    public Void visitIfStmt(Stmt.If ifStmt) {
        if (isTruthy(evaluate(ifStmt.condition))) {
            execute(ifStmt.thenBranch);
        }
        else {
            if (ifStmt.elseBranch != null) {
                execute(ifStmt.elseBranch);
            }
        }
        return null;
    }

    @Override
    public Void visitWhileStmt(Stmt.While whileStmt) {
        // "break" should send a signal to all subsequent execute()
        // and while loop should reset this signal on quit
        // "continue" should send a signal to all subsequent execute()
        // and reset within the while loop because continue just skips one loop
        while (isTruthy(evaluate(whileStmt.condition))) {
            execute(whileStmt.body);
            if (breakSignal) {
                breakSignal = false;
                break;
            }
            if (continueSignal) {
                continueSignal = false;
            }
        }
        return null;
    }

    @Override
    public Void visitForStmt(Stmt.For forStmt) {
        if (forStmt.initializer != null) {
            execute(forStmt.initializer);
        }
        while (forStmt.condition == null || isTruthy(evaluate(forStmt.condition))) {
            execute(forStmt.body);
            if (breakSignal) {
                breakSignal = false;
                break;
            }
            if (continueSignal) {
                continueSignal = false;
            }
            if (forStmt.increment != null) {
                evaluate(forStmt.increment);
            }
        }
        return null;
    }

    @Override
    public Void visitPrintStmt(Stmt.Print printStmt) {
        Object value = evaluate(printStmt.expression);
        System.out.println(stringify(value));
        return null;
    }

    @Override
    public Void visitBreakStmt(Stmt.Break breakStmt) {
        breakSignal = true;
        return null;
    }

    @Override
    public Void visitContinueStmt(Stmt.Continue continueStmt) {
        continueSignal = true;
        return null;
    }

    @Override
    public Object visitGroupingExpr(Expr.Grouping expr) {
        return evaluate(expr.expression);
    }

    /**
     * If it's null, return false;
     * If it's boolean, return its own value;
     * If it's everything else, it is recognized as true;
     */
    private boolean isTruthy(Object object) {
        if (object == null) {
            return false;
        }
        else if (object instanceof Boolean) {
            return (boolean)object;
        }
        return true;
    }

    private boolean isEqual(Object left, Object right) {
        if (left == null && right == null) {
            return true;
        }
        else if (left == null) {
            // At this point right must be non-null
            return false;
        }
        // If left is not null we can then use the equals method (even if right is null)
        return left.equals(right);
    }

    private Object evaluate(Expr expr) {
        return expr.accept(this);
    }

    private void execute(Stmt stmt) {
        if ((!breakSignal) && (!continueSignal)) {
            stmt.accept(this);
        }
    }

    @Override
    public Void visitBlockStmt(Stmt.Block stmt) {
        executeBlock(stmt.statements, new Environment(environment));
        return null;
    }

    /**
     * Save current environment and then restore it at the end
     */
    void executeBlock(List<Stmt> statements, Environment environment) {
        Environment previousEnv = this.environment;

        try {
            this.environment = environment;

            for (Stmt statement : statements) {
                execute(statement);
            }
        }
        finally {
            this.environment = previousEnv;
        }
    }

    private void checkNumberOperand(Token operator, Object operand) {
        // Check whether the object is a number
        if (operand instanceof Double) {
            return;
        }
        else {
            throw new RuntimeError(operator, "Operand must be a number.");
        }
    }

    private void checkNumberOperands(Token operator, Object left, Object right) {
        // Check whether both left and right objects are numbers
        if (left instanceof Double && right instanceof Double) {
            return;
        }
        else {
            throw new RuntimeError(operator, "Operands must be numbers.");
        }
    }

//    private Interpreter.InterpretError error(Token token, String message) {
//        // Lox.error(token, message);
//        String line = source.split("\n")[token.line];
//        Lox.error(token.line, token.column, line, message);
//        return new Interpreter.InterpretError();
//    }

    /**
     * If null      -> "nil";
     * If numerical -> truncate to integer format if ended with ".0";
     * Other cases  -> simply call toString()
     */
    private String stringify(Object object) {
        if (object == null) {
            return "nil";
        }
        else if (object instanceof Double) {
            String text = object.toString();
            if (text.endsWith(".0")) {
                text = text.substring(0, text.length() - 2);
            }
            return text;
        }
        return object.toString();
    }
}
