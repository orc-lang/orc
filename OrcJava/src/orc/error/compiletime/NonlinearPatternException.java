package orc.error.compiletime;

import orc.ast.simple.arg.NamedVar;

public class NonlinearPatternException extends PatternException {
	public NonlinearPatternException(NamedVar x) {
		super("Variable " + x + " occurs more than once in a pattern");
		setSourceLocation(x.getSourceLocation());
	}
}
