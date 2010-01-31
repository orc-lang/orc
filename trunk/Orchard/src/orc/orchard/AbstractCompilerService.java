package orc.orchard;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.logging.Logger;

import orc.Config;
import orc.Orc;
import orc.ast.xml.Oil;
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
		try {
			Config config = new Config();
			// Disable file resources for includes
			config.setIncludePath("");
			// Include sites specifically for orchard services
			config.addInclude("orchard.inc");
			config.inputFromString(program);
			ByteArrayOutputStream ourStdErrBytes = new ByteArrayOutputStream();
			config.setStderr(new PrintStream(ourStdErrBytes));
			orc.ast.oil.expression.Expression ex1 = Orc.compile(config);
			if (ex1 == null) {
				throw new InvalidProgramException(ourStdErrBytes.toString());
			}
			return new Oil("1.0", ex1.marshal());
		} catch (CompilationException e) {
			throw new InvalidProgramException(e.getMessage());
		} catch (IOException e) {
			throw new InvalidProgramException("IO error: " + e.getMessage());
		}
	}
	
	protected static Logger getDefaultLogger() {
		Logger out = Logger.getLogger(AbstractExecutorService.class.toString());
		return out;
	}
}