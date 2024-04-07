package com.craftinginterpreters.lox;

import static com.craftinginterpreters.lox.TokenType.*;

class Interpreter implements Expr.Visitor<Object> {
    /**
     * We need to implement the visitXExpr functions;
     * Each function returns a Java Object as Lox is dynamically typed,
     * which means that a variable may be assigned to different Java types
     */
    @Override
    public Object visitLiteralExpr(Expr.Literal expr) {
        return expr.value;
    }

    @Override
    public Object visitUnaryExpr(Expr.Unary expr) {
        Object right = evaluate(expr.right);
        if (expr.operator.type == MINUS) {
            // Then we know it's a number
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
                break;
            case MINUS:
                return (double)left - (double)right;
            case STAR:
                return (double)left * (double)right;
            case SLASH:
                return (double)left / (double)right;
            case GREATER:
                return (double)left > (double)right;
            case GREATER_EQUAL:
                return (double)left >= (double)right;
            case LESS:
                return (double)left < (double) right;
            case LESS_EQUAL:
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
}
