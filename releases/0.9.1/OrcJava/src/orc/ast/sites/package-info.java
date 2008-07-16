/**
 * Compile-time representation of Orc sites.
 * Each site has a URI and a protocol (which governs
 * the interpretation of the URI). For example,
 * the "orc" protocol expects the URI to name
 * a class (extending orc.runtime.sites.Site) in
 * the classpath.
 */
package orc.ast.sites;