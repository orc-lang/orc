((let(1) | let(2)) >x> x*x) | { return "foo" }
((let(1) | let(2)) >x> x*x) | { return {$x} "foo" } 
((let(1) | let(2)) >x> x*x) | { return {{{{}}} "boo" } >> let(2)