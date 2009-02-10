package orc.orchard.java;

import java.util.logging.Logger;

import orc.orchard.AbstractCompilerService;
import orc.orchard.errors.InvalidProgramException;
import orc.orchard.oil.Oil;

public class CompilerService extends AbstractCompilerService {
	public CompilerService() {
		super();
	}
	public CompilerService(Logger logger) {
		super(logger);
	}
	
	public static void main(String[] args) throws InvalidProgramException {
		CompilerService c = new CompilerService();
		Oil oil = c.compile("", "1 >x> x");
		System.out.println(oil.toXML());
	}
}
