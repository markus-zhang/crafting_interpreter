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