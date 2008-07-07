package orc.orchard;

import java.io.StringReader;

import orc.Config;
import orc.Orc;
import orc.ast.simple.arg.Var;
import orc.env.Env;
import orc.error.ParseError;
import orc.orchard.errors.InvalidProgramException;
import orc.orchard.oil.Marshaller;
import orc.orchard.oil.Oil;

public final class Compiler {
	public Oil compile(String program) throws InvalidProgramException {
		if (program == null) throw new InvalidProgramException("Null program!");
		orc.ast.simple.Expression ex0;
		try {
			ex0 = Orc.compile(new StringReader(program), new Config());
		} catch (ParseError e) {
			throw new InvalidProgramException("Syntax error: " + e.getMessage());
		}
		if (ex0 == null) {
			// FIXME: obviously need more detail here
			throw new InvalidProgramException("Syntax error in: " + program);
		}
		orc.ast.oil.Expr ex1 = ex0.convert(new Env<Var>());;
		orc.orchard.oil.Expression ex2 = ex1.accept(new Marshaller());
		return new Oil("1.0", ex2);
	}
}
