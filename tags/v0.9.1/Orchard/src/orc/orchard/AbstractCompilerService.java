package orc.orchard;

import java.io.StringReader;
import java.util.logging.Logger;

import orc.Config;
import orc.Orc;
import orc.ast.simple.arg.Var;
import orc.env.Env;
import orc.error.ParseError;
import orc.orchard.errors.InvalidProgramException;
import orc.orchard.oil.Marshaller;
import orc.orchard.oil.Oil;


/**
 * Implementation of a compiler service.
 * @author quark
 */
public abstract class AbstractCompilerService implements orc.orchard.api.CompilerServiceInterface {
	protected Logger logger;

	protected AbstractCompilerService(Logger logger) {
		this.logger = logger;
	}

	protected AbstractCompilerService() {
		this(getDefaultLogger());
	}

	public Oil compile(String devKey, String program) throws InvalidProgramException {
		if (program == null) throw new InvalidProgramException("Null program!");
		orc.ast.simple.Expression ex0;
		try {
			Config config = new Config();
			// Include sites specifically for orchard services
			config.addInclude("orchard.inc");
			ex0 = Orc.compile(new StringReader(program), config);
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
	
	protected static Logger getDefaultLogger() {
		Logger out = Logger.getLogger(AbstractExecutorService.class.toString());
		return out;
	}
}