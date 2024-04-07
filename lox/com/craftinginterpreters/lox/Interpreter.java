package com.craftinginterpreters.lox;

import static com.craftinginterpreters.lox.TokenType.*;

class Interpreter implements Expr.Visitor<Object> {
    /**
     * We need to implement the visitXExpr functions;
     * Each function returns a Java Object as Lox is dynamically typed,
     * which means that a variable may be assigned to different Java types
     */
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

    public Object visitGroupingExpr(Expr.Grouping expr) {
        return evaluate(expr.expression);
    }

    private boolean isTruthy(Object object) {
        /**
         * If it's null, return false;
         * If it's boolean, return its own value;
         * If it's everything else, it is recognized as true;
         */
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

    private void checkNumberOperand(Token operator, Object operand) {
        /**
         * Check whether the object is a number
         */
        if (operand instanceof Double) {
            return;
        }
        else {
            throw new RuntimeError(operator, "Operand must be a number.");
        }
    }

    private void checkNumberOperands(Token operator, Object left, Object right) {
        /**
         * Check whether both left and right objects are numbers
         */
        if (left instanceof Double && right instanceof Double) {
            return;
        }
        else {
            throw new RuntimeError(operator, "Operands must be numbers.");
        }
    }

    private String stringify(Object object) {
        /**
         * If null      -> "nil";
         * If numerical -> truncate to integer format if ended with ".0";
         * Other cases  -> simply call toString()
         */
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
