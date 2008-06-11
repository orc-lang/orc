package orc.orchard;

import java.util.logging.Logger;


/**
 * For now this only supports local usage.
 * @author quark
 *
 */
public abstract class AbstractCompilerService implements orc.orchard.CompilerServiceInterface {
	protected Logger logger;

	protected AbstractCompilerService(Logger logger) {
		this.logger = logger;
	}

	protected AbstractCompilerService() {
		this(getDefaultLogger());
	}

	public Oil compile(String program) throws InvalidProgramException {
		return new Oil(program);
	}
	
	protected static Logger getDefaultLogger() {
		Logger out = Logger.getLogger(AbstractExecutorService.class.toString());
		return out;
	}
}