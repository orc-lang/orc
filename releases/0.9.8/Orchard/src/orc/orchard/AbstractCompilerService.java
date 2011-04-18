package orc.orchard;

import java.io.IOException;
import java.io.StringReader;
import java.util.logging.Logger;

import orc.Config;
import orc.Orc;
import orc.ast.oil.xml.Marshaller;
import orc.ast.oil.xml.Oil;
import orc.error.compiletime.CompilationException;
import orc.orchard.errors.InvalidProgramException;


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
		logger.info("compile(" + devKey + ", " + program + ")");
		if (program == null) throw new InvalidProgramException("Null program!");
		orc.ast.oil.Expr ex1;
		try {
			Config config = new Config();
			// Disable file resources for includes
			config.setIncludePath(null);
			// Include sites specifically for orchard services
			config.addInclude("orchard.inc");
			ex1 = Orc.compile(new StringReader(program), config);
		} catch (CompilationException e) {
			throw new InvalidProgramException(e.getMessage());
		} catch (IOException e) {
			throw new InvalidProgramException("IO error: " + e.getMessage());
		}
		if (ex1 == null) {
			// FIXME: obviously need more detail here
			throw new InvalidProgramException("Syntax error in: " + program);
		}
		orc.ast.oil.xml.Expression ex2 = ex1.accept(new Marshaller());
		return new Oil("1.0", ex2);
	}
	
	protected static Logger getDefaultLogger() {
		Logger out = Logger.getLogger(AbstractExecutorService.class.toString());
		return out;
	}
}