//
// OrcScriptEngine.java -- Java class OrcScriptEngine
// Project OrcScala
//
// $Id$
//
// Created by jthywiss on May 25, 2010.
//
// Copyright (c) 2010 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.script;

import java.io.Reader;
import java.io.StringReader;

import javax.script.AbstractScriptEngine;
import javax.script.Bindings;
import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptException;

import orc.ExperimentalOrc;
import orc.Orc;
import orc.OrcCompiler;

import scala.util.parsing.input.StreamReader;

/**
 * @author jthywiss
 */
public class OrcScriptEngine extends AbstractScriptEngine implements Compilable {

	private OrcScriptEngineFactory factory;
	private OrcCompiler compiler;
	private Orc executor;

	/**
	 * Constructs an object of class OrcScriptEngine.
	 */
	public OrcScriptEngine() {
		super();
	}

	/**
	 * Constructs an object of class OrcScriptEngine.
	 * 
	 * @param n
	 */
	public OrcScriptEngine(final Bindings n) {
		super(n);
	}

	private class OrcCompiledScript extends CompiledScript {
		private orc.oil.Expression astRoot;

		/**
		 * Constructs an object of class OrcCompiledScript.
		 *
		 */
		public OrcCompiledScript(orc.oil.Expression oilAstRoot) {
			this.astRoot = oilAstRoot;
		}

		/* (non-Javadoc)
		 * @see javax.script.CompiledScript#eval(javax.script.ScriptContext)
		 */
		@Override
		public Object eval(ScriptContext context) throws ScriptException {
			OrcScriptEngine.this.getExecutor().run(astRoot);
			return null;
		}

		/* (non-Javadoc)
		 * @see javax.script.CompiledScript#getEngine()
		 */
		@Override
		public ScriptEngine getEngine() {
			return OrcScriptEngine.this;
		}

	}

	/* (non-Javadoc)
	 * @see javax.script.Compilable#compile(java.lang.String)
	 */
	@Override
	public CompiledScript compile(final String script) throws ScriptException {
		return compile(new StringReader(script));
	}

	/* (non-Javadoc)
	 * @see javax.script.Compilable#compile(java.io.Reader)
	 */
	@Override
	public CompiledScript compile(final Reader script) throws ScriptException {
		return new OrcCompiledScript(getCompiler().compile(script));
	}

	/* (non-Javadoc)
	 * @see javax.script.ScriptEngine#createBindings()
	 */
	@Override
	public Bindings createBindings() {
		return new OrcBindings();
	}

	/* (non-Javadoc)
	 * @see javax.script.ScriptEngine#eval(java.lang.String, javax.script.ScriptContext)
	 */
	@Override
	public Object eval(final String script, final ScriptContext ctxt) throws ScriptException {
		return compile(script).eval(ctxt);
	}

	/* (non-Javadoc)
	 * @see javax.script.ScriptEngine#eval(java.io.Reader, javax.script.ScriptContext)
	 */
	@Override
	public Object eval(final Reader reader, final ScriptContext ctxt) throws ScriptException {
		return compile(reader).eval(ctxt);
	}

	/* (non-Javadoc)
	 * @see javax.script.ScriptEngine#getFactory()
	 */
	@Override
	public ScriptEngineFactory getFactory() {
		synchronized (this) {
			if (factory == null) {
				factory = new OrcScriptEngineFactory();
			}
		}
		return factory;
	}

	/**
	 * @param factory
	 */
	void setFactory(final OrcScriptEngineFactory owningFactory) {
		factory = owningFactory;
	}

	/**
	 * @return
	 */
	OrcCompiler getCompiler() {
		synchronized (this) {
			if (compiler == null) {
				compiler = new OrcCompiler(); //FIXME: Need a factory
			}
		}
		return compiler;
	}

	/**
	 * @return
	 */
	Orc getExecutor() {
		synchronized (this) {
			if (executor == null) {
				executor = new ExperimentalOrc(); //FIXME: Need a factory
			}
		}
		return executor;
	}

}
