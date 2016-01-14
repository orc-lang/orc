//
// SingleField.java -- Java class SingleField
// Project Orchard
//
// Copyright (c) 2009 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.lib.orchard.forms;

import java.util.List;

public abstract class SingleField<V> extends Field<V> {
    protected String posted;

    @SuppressWarnings("serial")
    public static class ValidationException extends Exception {
        private final String message;

        public ValidationException(final String message) {
            this.message = message;
        }

        @Override
        public String getMessage() {
            return message;
        }
    }

    public SingleField(final String key, final String label, final String posted) {
        super(key, label, null);
        this.posted = posted;
    }

    @Override
    public void readRequest(final FormData request, final List<String> errors) {
        try {
            posted = request.getParameter(key);
            value = requestToValue(posted);
        } catch (final ValidationException e) {
            errors.add(e.getMessage());
        }
    }

    public abstract V requestToValue(String posted) throws ValidationException;
}
