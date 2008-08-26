package orc.trace.values;

import java.io.IOException;
import java.io.Serializable;
import java.io.Writer;

import orc.trace.Term;

/**
 * Supertype for traced value representations.
 * @author quark
 */
public interface Value extends Serializable, Term {}
