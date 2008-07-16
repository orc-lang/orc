package orc.lib.orchard;

import orc.error.SiteException;
import orc.error.TokenException;
import orc.runtime.Args;
import orc.runtime.OrcEngine;
import orc.runtime.Token;
import orc.runtime.sites.Site;
import orc.runtime.values.Constant;

/**
 * Ask the user a question and return their response.
 * This is designed to interact with the Orchard services
 * so that Orchard clients can handle the prompt.
 * @author quark
 */
public class Prompt extends Site {
	/**
	 * Interface implemented by an engine which can handle
	 * the Prompt site.
	 * @author quark
	 */
	public interface Promptable {
		public void prompt(String message, PromptCallback callback);
	}
	public interface PromptCallback {
		public void respondToPrompt(String response);
		public void cancelPrompt();
	}
	@Override
	public void callSite(Args args, final Token caller) throws TokenException {
		OrcEngine engine = caller.getEngine();
		final String prompt = args.stringArg(0);
		if (!(engine instanceof Promptable)) {
			caller.error(new SiteException(
					"This Orc engine does not support the Prompt site."));
		}
		((Promptable)engine).prompt(prompt, new PromptCallback () {
			public void respondToPrompt(String response) {
				caller.resume(new Constant(response));	
			}
			public void cancelPrompt() {
				caller.die();
			}
		});
	}
}
