//
// OAuthProviderSite.java -- Java class OAuthProviderSite
// Project OrcSites
//
// Copyright (c) 2016 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.lib.ui;

import java.io.IOException;

import orc.error.runtime.JavaException;
import orc.error.runtime.TokenException;
import orc.values.sites.compatibility.Args;
import orc.values.sites.compatibility.EvalSite;

public class OAuthProviderSite extends EvalSite {
    @Override
    public Object evaluate(final Args args) throws TokenException {
        try {
            /**
             * This implementation of OAuthProvider
             */
            return new GuiOAuthProvider(
            // force root-relative resource path
                    "/" + args.stringArg(0));
        } catch (final IOException e) {
            throw new JavaException(e);
        }
    }
}
