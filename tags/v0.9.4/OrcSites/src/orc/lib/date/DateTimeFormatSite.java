package orc.lib.date;

import org.joda.time.format.DateTimeFormat;

import orc.error.runtime.TokenException;
import orc.runtime.Args;
import orc.runtime.sites.DotSite;
import orc.runtime.sites.EvalSite;

public class DateTimeFormatSite extends DotSite {

	@Override
	protected void addMethods() {
		addMethod("forPattern", new EvalSite() {
			@Override
			public Object evaluate(Args args) throws TokenException {
				return DateTimeFormat.forPattern(args.stringArg(0));
			}
		});
		addMethod("forStyle", new EvalSite() {
			@Override
			public Object evaluate(Args args) throws TokenException {
				return DateTimeFormat.forStyle(args.stringArg(0));
			}
		});
	}

}
