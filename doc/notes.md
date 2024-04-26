### page 117:

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