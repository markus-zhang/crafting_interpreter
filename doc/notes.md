### Note 00: page 117:

In the beginning of 8.2.1, the author talks about NOT allowing variable declaration following an if statement without braces (scope).

```lox
// This is not allowed
if (monday) car beverage = "espresso";
```

He goes on to explain that we can fix the problem by creating another layer "declaration" above "statement":

```cfg
    program         -> declaration* EOF ;
    declaration     -> varDecl;
                    -> statement;
    statement       -> exprStmt ;
                    -> printStmt ;
```
My understanding is: the `if` statement would only allow statements following it, or some statements wrapped in `{}`. Since variable declaration is NOT a statement (`varDecl`), the grammar forbids it tails the `if` statement.

### Note 01: page 121:

In the second half of the page the author explains why he makes calling a variable (such as in func() or print(a)) a runtime error, instead of a syntax error (compile time) or allowing it and returning nil.

His reasoning is two folds:
- If we allow it, it's too lax
- If we make it into a compile time error, it's too strict as it forbids mutually recursive functions:

```lox
fun a(n) {
    if ... return n/2;
    return b(n-1);
}

fun b(m) {
    if ... return m/4;
    return a(m-1);
}
```

If we make it into a compile time error, since `b()` is not in the HashMap  when `a()` calls it, the perfectly legit mutually recursive call results in an error. But if we allow it to be compiled, a runtime error will be raised ONLY when `b()` is not in the HashMap when everything has been parsed.

However, we should not allow using variables before they are declared:
```lox
print a;
var a = "too late!";
```
I don't exactly know how the author is going to do this. Theoretically, if he scans ahead and saves the function names (they are also IDENTIFIERs) into the HashMap, what is preventing him from scanning ahead and saves the variable names as well? Maybe he distinguishes them in the code.

### Note 01: Scope

Rules that we want to follow:

- Variables inside of a scope should be able to shadow the ones outside if they have the same names;
- Variables declared inside of a scope should be removed once the program is out of the scope;
- When the compiler tries to find the variable, it starts from the current scope and then search its enclosing ones, until it reaches the top one (global)

The implementation is a tree. Each environment has a "pointer" pointing to its enclosing one.

### Note 02: Logical Operators

**Short-circuit:**

- Logical `and` is always `false` if any of the conditions is `false`;
- Logical `or` is always `true` if any of the condition is `true`

**Precedence:**

Logical operators are of the lowest precedence in the operators. 
They are still higher than the assignment expression though:

```
a = b and c
```
This would be weird if b is assigned to a first and then and with c, although the result is the same.

Within the logical operators themselves, we decided to put logical_or as the lowest one.
Thus in BNF assignment cascades to logical_or, which cascades to logical_and, which cascades to equality.

**Evaluation**

If we can short-circuit it by evaluating the left side, we should return the left side (Why? Why not return TRUE or FALSE?)

If we cannot short-circuit it, the result is then determined by the right side, and the left side can be discarded.
