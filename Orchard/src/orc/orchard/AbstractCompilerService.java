package orc.orchard;

import java.util.logging.Logger;

import orc.orchard.errors.InvalidProgramException;
import orc.orchard.oil.Oil;


/**
 * For now this only supports local usage.
 * @author quark
 *
 */
public abstract class AbstractCompilerService implements orc.orchard.api.CompilerServiceInterface {
	protected Logger logger;
	protected Compiler compiler;

	protected AbstractCompilerService(Logger logger) {
		this.logger = logger;
		this.compiler = new Compiler();
	}

	protected AbstractCompilerService() {
		this(getDefaultLogger());
	}

	public Oil compile(String program) throws InvalidProgramException {
		return compiler.compile(program);
	}
	
	protected static Logger getDefaultLogger() {
		Logger out = Logger.getLogger(AbstractExecutorService.class.toString());
		return out;
	}
}