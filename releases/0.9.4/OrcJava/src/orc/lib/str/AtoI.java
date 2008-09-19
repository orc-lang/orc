/**
 * 
 */
package orc.lib.str;

import orc.error.runtime.TokenException;
import orc.runtime.Args;
import orc.runtime.sites.PartialSite;
import orc.type.ArrowType;
import orc.type.EllipsisArrowType;
import orc.type.Type;

/**
 * @author dkitchin
 *
 */
public class AtoI extends PartialSite {

	public Object evaluate(Args args) throws TokenException {
		
		String s = args.stringArg(0);
		
		try {
			return Integer.parseInt(s);
		} catch (NumberFormatException e) {
			return null;
		}
	}
	public static Type type() {
		return new ArrowType(Type.STRING, Type.NUMBER);
	}


}
