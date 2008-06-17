package orc.orchard;

import java.io.StringReader;

import orc.Config;
import orc.Orc;
import orc.ast.simple.arg.Var;
import orc.env.Env;
import orc.orchard.errors.InvalidProgramException;
import orc.orchard.oil.Oil;

public final class Compiler {
	public Oil compile(String program) throws InvalidProgramException {
		orc.ast.simple.Expression ex0 = Orc.compile(new StringReader(program), new Config());
		if (ex0 == null) {
			// FIXME: obviously need more detail here
			throw new InvalidProgramException("Syntax error");
		}
		orc.ast.oil.Expr ex1 = ex0.convert(new Env<Var>());;
		orc.orchard.oil.Expression ex2 = ex1.marshal();
		return new Oil("1.0", ex2);
	}
}
