package orc.error.compiletime;

import orc.ast.simple.argument.FreeVariable;

public class NonlinearPatternException extends PatternException {
	public NonlinearPatternException(FreeVariable x) {
		super("Variable " + x + " occurs more than once in a pattern");
		setSourceLocation(x.getSourceLocation());
	}
}
