package orc.orchard.rmi;

import java.io.StringReader;

import orc.Config;
import orc.Orc;
import orc.ast.simple.Expression;

/**
 * FIXME: this is a temporary hack to work around the fact that ast.simple is
 * not serializable yet. Will be rewritten soon.
 * 
 * @author quark
 * 
 */
public class Oil implements orc.orchard.interfaces.Oil {
	private String program;

	public Oil(String program) {
		this.program = program;
	}

	public Expression getExpression() {
		return Orc.compile(new StringReader(program), new Config());
	}
}