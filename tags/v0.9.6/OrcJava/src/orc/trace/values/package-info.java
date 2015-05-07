/**
 * Serializable, immutable representations of Orc runtime values.
 * While the usual Orc types are immutable and serializable,
 * arbitrary Java objects (which they may contain) aren't. So we
 * need an explicit conversion which translates any Java objects to
 * serializable stand-ins.
 */
package orc.trace.values;