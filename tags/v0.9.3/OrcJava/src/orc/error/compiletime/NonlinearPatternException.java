package orc.error.compiletime;

import orc.ast.simple.arg.NamedVar;

public class NonlinearPatternException extends PatternException {

	public NonlinearPatternException(NamedVar x) {
		// TODO: Associate x's source location with this exception
		super("Variable " + x + " occurs more than once in a pattern");
	}

}
