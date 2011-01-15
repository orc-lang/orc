//
// Prompt.java -- Java class Prompt
// Project Orchard
//
// $Id$
//
// Copyright (c) 2010 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.lib.orchard;

import orc.Handle;
import orc.OrcRuntime;
import orc.error.runtime.JavaException;
import orc.error.runtime.TokenException;
import orc.values.sites.compatibility.Args;
import orc.values.sites.compatibility.SiteAdaptor;

/**
 * Ask the user a question and return their response.
 * This is designed to interact with the Orchard services
 * so that Orchard clients can handle the prompt.
 * @author quark
 */
public class Prompt extends SiteAdaptor {
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
	public void callSite(final Args args, final Handle caller) throws TokenException {
		final OrcRuntime engine = ((Orc.Token) caller).runtime(); //FIXME:Use OrcEvents, not subclassing for Prompts
		final String prompt = args.stringArg(0);
		if (!(engine instanceof Promptable)) {
			caller.$bang$bang(new JavaException(new UnsupportedOperationException("This Orc engine does not support the Prompt site.")));
		}
		((Promptable) engine).prompt(prompt, new PromptCallback() {
			@Override
			public void respondToPrompt(final String response) {
				caller.publish(response);
			}

			@Override
			public void cancelPrompt() {
				caller.halt();
			}
		});
	}
}
