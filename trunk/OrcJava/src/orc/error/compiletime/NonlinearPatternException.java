package orc.error.compiletime;

import orc.ast.simple.argument.NamedVariable;

public class NonlinearPatternException extends PatternException {
	public NonlinearPatternException(NamedVariable x) {
		super("Variable " + x + " occurs more than once in a pattern");
		setSourceLocation(x.getSourceLocation());
	}
}
