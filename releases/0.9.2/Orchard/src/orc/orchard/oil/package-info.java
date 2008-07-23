/**
 * JAXB implementation classes for the Orc Intermediate Language
 * XML binding. These classes exactly mirror the structure of the
 * XML, unlike the OIL classes in orc.ast.oil which are
 * designed around the needs of the implementation.
 * 
 * <p>To convert between orc.ast.oil.Expr and orc.orchard.oil.Expression,
 * use the methods orc.ast.oil.Expr.marshal() and
 * orc.orchard.oil.Expression.unmarshal().
 *
 * <p>When you change any of these files, make sure to recompile with
 * Build "Clean", so that the JAX-WS stuff can rebuild. 
 *
 * @see orc.orchard.soap
 * @see orc.ast.oil
 */
package orc.orchard.oil;