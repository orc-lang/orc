## Optimizations

# Required Optimizations:

* Future elimination (both when it will immediately be forced and when it will get a value immediately)
** Requires: Publication count, time to first publication, variable forcing, variable dependence.
* Force elimination
** Requires: Futureness, forces in scope (searching for forces in the context which match the type needed).
* Force lifting
** Requires: forces in an expression.
* Trim elimination and compile time choice
** Requires: Effect times, publication count, publication time.
* Simple algebraic optimizations: Stop elim, Seq elim, stop equiv (DCE).
** Requires: Publication count, effects, time to halt.
* Tuple scalar replacement
** Requires: Time to value available, free variables.

# Enabling Optimizations:

There should probably be implemented as a library or part of another optimization, so that every optimization can actually enable another optimization. However, for cases like Inlining/Cloning this may not be possible.

* Inlining/Cloning
** Requires: Compatible context information, expression "cost".
* Seq expansion (RHS cloning)
** Requires: None.
* Recursion unrolling
** Requires: None.

# Desirable Optimizations:

* Object scalar replacement
** Requires: Free variables, escape analysis.
* Field future force elimination
** Requires: Object field futureness.
* Lift subexpressions which will always run and do not need their enclosing expression: body of graft and RHS of seq (in some cases)
** Requires: time to halt, publication count, time to first publication/
* Constant propagation
** Requires: A way to call sites at compile time.


## Analyses

All other analyses depend on callable metadata for handling calls to external and internal sites and functions.

Ideally these analyses would be interprocedural. In addition, each analysis may need a custom way to handle recursive calls so as to still get useful information.

# On variables:

* Futureness: Whether a variable could be bound to a future. From {Always, Never, Sometimes}.
** Data Flow Analysis
* Time to value available: The maximum amount of time binding can be delayed if this is a future. From {Nonblocking, Blocking, Forever}. The means are exactly analogous to the below definitions for "time to first publication". Non-future values will always be non-blocking.
** DFA
* Callable metadata: Analysis or asserted metadata about the callable referenced by the variable. This would include a summary to translate argument variable metadata to expression metadata for a call and metadata about the return value.
** DFA
* Object metadata: Analysis or asserted metadata about the fields of the variable. This would include all of the above analyses for each field.
** DFA

# On expressions:

* Publication count: The possible numbers of publications of an expression. From intervals over {0, 1, ... \omaga}.
** Recursive Expression Analysis.
* Time to first publication: The maximum amount of time the first publication can be delayed. From {Nonblocking, Blocking, Forever}.
*** Nonblocking means the expression will publish without waiting on any event outside itself (such as real time or a value form a channel). Critically for optimization this mean that this expression can execute to completion even if the rest of the problem is prevented from executing; regardless of the state of the program.
*** Blocking means the expression may wait for some external even before publishing. In this case, the expression may not complete if the rest of the program is not running.
*** Forever means the expression may never publish.
** REA
* Time to halting: The maximum amount of time until this expression halts. From {Nonblocking, Blocking, Forever}. The means are exactly analogous to the above definitions for "time to first publication".
** REA
* Effect times: When this expression may have side effects. From {Never, BeforeFirstPublications, Anytime}.
** REA
* Effect types: Potentially we could track effect types to further limit effect conflicts.
** REA
* Variable forcing: What events of the expression's execution always happen after the variable is resolved (shallowly; closed variables need not be)? From the power set of {Publication, Halting, Effects}.
** REA
* Variable dependence: Can this expression do anything if the variable halts? From {T, F}. If T then this expression will not have any effect or publish if the variable halts.
** REA


## The Orctimizer Language

# Futures

Futures are explicit in Orctimizer so graft is replaced with future creation and forcing.

# Objects

Since futures are explicit in Orctimizer, objects can be strict records with with fields that may or may not be futures. Field access in Orc will be encoded as an Orctimizer field access followed by an explicit force.

# Sites

Sites are introduced to Orctimizer as a primitive declaration along side defs.

Sites could be specially formatted defs if Orctimizer had explicit terminators and ways to control them. However this would complicate the language with additional low level details (that would be need to be tracked in some cases) without removing anything other than the site declaration primitive.

# Why not optimize in OIL?

OIL does not allow forces to be eliminated because they are not explicit.

# Why not optimize in Porc?

Analysis would be very difficult because of the callback based site calls and forces. The analyzer would have to regenerate most of that Orctimizer had to begin with to determine reasonable values for things like how long a variable will block when forced.
