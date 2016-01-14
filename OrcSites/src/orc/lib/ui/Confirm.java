//
// Confirm.java -- Java class Confirm
// Project OrcSites
//
// Copyright (c) 2009 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.lib.ui;

import javax.swing.JOptionPane;

import orc.error.runtime.TokenException;
import orc.values.sites.compatibility.Args;
import orc.values.sites.compatibility.PartialSite;

/**
 * A Yes/No/Cancel confirmation dialog. "Yes" = true, "No" = false, and "Cancel"
 * = null.
 */
public class Confirm extends PartialSite {
    @Override
    public Object evaluate(final Args args) throws TokenException {
        final String message = args.stringArg(0);
        final int chosen = JOptionPane.showConfirmDialog(null, message);
        switch (chosen) {
        case 0: // YES
            return true;
        case 1: // NO
            return false;
        }
        // CANCEL
        return null;
    }
}
