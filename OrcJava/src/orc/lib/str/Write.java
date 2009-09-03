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
import orc.type.Type;
import orc.type.TypeVariable;
import orc.type.structured.ArrowType;
import xtc.parser.ParseException;
import xtc.parser.Result;

/**
 * Convert an Orc literal to a String.
 * @author quark
 */
public class Write extends EvalSite {
	@Override
	public Object evaluate(Args args) throws TokenException {
		return write(args.getArg(0));
	}
	
	public Type type() {
		return new ArrowType(Type.TOP, Type.STRING);
	}
}
