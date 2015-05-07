/**
 * Internal representation of OIL, the Orc Intermediate Language.
 * This is an AST which uses DeBruijn indices and therefore
 * has no explicit named variables. This is the simplest AST representation
 * of a program before it is translated into a runtime DAG.
 */
package orc.ast.oil;