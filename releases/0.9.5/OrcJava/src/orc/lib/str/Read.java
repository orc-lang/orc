package orc.lib.str;

import java.io.IOException;
import java.io.StringReader;

import orc.error.compiletime.ParsingException;
import orc.error.runtime.JavaException;
import orc.error.runtime.TokenException;
import orc.parser.OrcParser;
import orc.runtime.Args;
import orc.runtime.sites.EvalSite;

/**
 * Read an Orc literal from a string.
 * @author quark
 */
public class Read extends EvalSite {
	@Override
	public Object evaluate(Args args) throws TokenException {
		try {
			return new OrcParser(new StringReader(args.stringArg(0)))
				.parseLiteralValue();
		} catch (ParsingException e) {
			throw new JavaException(e);
		} catch (IOException e) {
			// should be impossible
			throw new AssertionError(e);
		}
	}
}
