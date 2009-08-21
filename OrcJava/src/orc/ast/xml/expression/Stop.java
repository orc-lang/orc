package orc.ast.xml.expression;

import orc.Config;
import orc.error.compiletime.CompilationException;

public class Stop extends Expression {
	@Override
	public orc.ast.oil.expression.Expression unmarshal(Config config) throws CompilationException {
		return new orc.ast.oil.expression.Stop();
	}
}
