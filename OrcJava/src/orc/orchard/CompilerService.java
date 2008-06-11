package orc.orchard;

import orc.orchard.error.InvalidProgramException;

/**
 * For now this only supports local usage.
 * @author quark
 *
 */
public class CompilerService implements
		orc.orchard.interfaces.CompilerService<Oil>
{
	public Oil compile(String program) throws InvalidProgramException {
		return new Oil(program);
	}
}