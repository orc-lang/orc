//
// IntegerField.java -- Java class IntegerField
// Project Orchard
//
// Copyright (c) 2016 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.lib.orchard.forms;

import java.io.IOException;
import java.io.PrintWriter;

public class IntegerField extends SingleField<Integer> {

    public IntegerField(final String key, final String label) {
        super(key, label, "");
    }

    @Override
    public void renderControl(final PrintWriter out) throws IOException {
        out.write("<input type='textbox'" + " id='" + key + "'" + " name='" + key + "'" + " value='" + posted + "'" + ">");
    }

    @Override
    public Integer requestToValue(final String posted) throws ValidationException {
        try {
            if (posted.equals("")) {
                return null;
            }
            return Integer.parseInt(posted);
        } catch (final NumberFormatException e) {
            throw new ValidationException(label + " must be an integer.");
        }
    }
}
