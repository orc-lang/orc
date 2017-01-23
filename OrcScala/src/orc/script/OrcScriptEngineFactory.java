//
// OrcScriptEngineFactory.java -- Java class OrcScriptEngineFactory
// Project OrcScala
//
// Created by jthywiss on May 25, 2010.
//
// Copyright (c) 2016 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.script;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;

/**
 * @author jthywiss
 */
public class OrcScriptEngineFactory implements ScriptEngineFactory {

    /**
     * Constructs an object of class OrcScriptEngineFactory.
     */
    public OrcScriptEngineFactory() {
    }

    @Override
    public String getEngineName() {
        return "Orc";
    }

    @Override
    public String getEngineVersion() {
        return System.getProperty("orc.version");
    }

    @Override
    public List<String> getExtensions() {
        return extensions;
    }

    @Override
    public String getLanguageName() {
        return "orc";
    }

    @Override
    public String getLanguageVersion() {
        return System.getProperty("orc.version");
    }

    @Override
    public String getMethodCallSyntax(final String obj, final String m, final String... args) {
        final StringBuilder buf = new StringBuilder();
        buf.append(obj);
        buf.append(".");
        buf.append(m);
        buf.append("(");
        if (args.length != 0) {
            int i = 0;
            for (; i < args.length - 1; i++) {
                buf.append(args[i] + ", ");
            }
            buf.append(args[i]);
        }
        buf.append(")");
        return buf.toString();
    }

    @Override
    public List<String> getMimeTypes() {
        return mimeTypes;
    }

    @Override
    public List<String> getNames() {
        return names;
    }

    @Override
    public String getOutputStatement(final String toDisplay) {
        final StringBuilder buf = new StringBuilder();
        buf.append("print(\"");
        final int len = toDisplay.length();
        for (int i = 0; i < len; i++) {
            final char ch = toDisplay.charAt(i);
            switch (ch) {
            case '"':
                buf.append("\\\"");
                break;
            case '\\':
                buf.append("\\\\");
                break;
            default:
                buf.append(ch);
                break;
            }
        }
        buf.append("\")");
        return buf.toString();
    }

    @Override
    public Object getParameter(final String key) {
        if (key.equals(ScriptEngine.ENGINE)) {
            return getEngineName();
        } else if (key.equals(ScriptEngine.ENGINE_VERSION)) {
            return getEngineVersion();
        } else if (key.equals(ScriptEngine.NAME)) {
            return getEngineName();
        } else if (key.equals(ScriptEngine.LANGUAGE)) {
            return getLanguageName();
        } else if (key.equals(ScriptEngine.LANGUAGE_VERSION)) {
            return getLanguageVersion();
        } else if (key.equals("THREADING")) {
            return "STATELESS";
            // STATELESS = Each thread will effectively have its own
            // thread-local engine scope
            // -and- the mappings in the Bindings are not modified by any
            // ScriptExecution.
        } else {
            return null;
        }
    }

    @Override
    public String getProgram(final String... statements) {
        final StringBuilder buf = new StringBuilder();
        if (statements.length != 0) {
            for (int i = 0; i < statements.length; i++) {
                buf.append(statements[i]);
                if (i < statements.length - 1) {
                    buf.append("  >>\n");
                }
            }
        }
        return buf.toString();
    }

    @Override
    public ScriptEngine getScriptEngine() {
        // This is a bit of a hack. This is instantiating the type in
        // OrcScriptEngine,
        // only because it allows OrcScriptEngine to be written more safely.
        final OrcScriptEngine<?> engine = new OrcScriptEngine<Object>();
        engine.setFactory(this);
        return engine;
    }

    private static List<String> names = Collections.unmodifiableList(Arrays.asList(new String[] { "orc" }));
    private static List<String> extensions = Collections.unmodifiableList(Arrays.asList(new String[] { "orc", "inc" }));
    private static List<String> mimeTypes = Collections.unmodifiableList(Arrays.asList(new String[] {}));

}
