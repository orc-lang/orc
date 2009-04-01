package orc.lib.str;

import java.io.IOException;
import java.io.StringReader;

import orc.error.compiletime.ParsingException;
import orc.error.runtime.JavaException;
import orc.error.runtime.TokenException;
import orc.parser.AbortParse;
import orc.parser.OrcLiteralParser;
import orc.parser.OrcParser;
import orc.runtime.Args;
import orc.runtime.sites.EvalSite;
import orc.type.ArrowType;
import orc.type.Type;
import orc.type.TypeVariable;
import xtc.parser.ParseException;
import xtc.parser.Result;

/**
 * Read an Orc literal from a string.
 * @author quark
 */
public class Read extends EvalSite {
	@Override
	public Object evaluate(Args args) throws TokenException {
		try {
			OrcLiteralParser parser = new OrcLiteralParser(
					new StringReader(args.stringArg(0)),
					"<input string>");
			Result result = parser.pLiteralValue(0);
			return parser.value(result);
		} catch (AbortParse e) {
			throw new JavaException(e);
		} catch (ParseException e) {
			throw new JavaException(e);
		} catch (IOException e) {
			// should be impossible
			throw new AssertionError(e);
		}
	}
	
	public Type type() {
		TypeVariable X = new TypeVariable(0);
		return new ArrowType(Type.STRING, X, 1);
	}
}
