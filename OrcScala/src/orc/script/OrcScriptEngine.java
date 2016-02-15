//
// OrcScriptEngine.java -- Java class OrcScriptEngine
// Project OrcScala
//
// $Id$
//
// Created by jthywiss on May 25, 2010.
//
// Copyright (c) 2011 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.script;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import javax.script.AbstractScriptEngine;
import javax.script.Bindings;
import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptException;

import orc.Backend;
import orc.BackendType;
import orc.OrctimizerBackend;
import orc.PorcBackend;
import orc.OrctimizerInterpreterBackend;
import orc.PorcCompilerBackend;
import orc.Runtime;
import orc.Compiler;
import orc.OrcEvent;
import orc.OrcEventAction;
import orc.StandardBackend;
import orc.TokenInterpreterBackend;
import orc.error.OrcException;
import orc.error.loadtime.LoadingException;
import orc.lib.str.PrintEvent;
import orc.run.OrcDesktopEventAction;

/* TODO:
 * Generalize types to allow different backends
 * Set up appropriate checks to choose the right backend throughout. But ideally it will 
 * be as simple as just instantiating the right Compiler and Runtime. This will require new
 * traits for interfacing to unknown compilers and backends.
 */

/**
 * @author jthywiss
 */
public class OrcScriptEngine<CompiledCode> extends AbstractScriptEngine implements Compilable {

  private OrcScriptEngineFactory factory;
  private Backend<CompiledCode>  _backend;
  private Runtime<CompiledCode>  _executor;
  private BackendType backendType;

  /**
   * Constructs an object of class OrcScriptEngine using a
   * <code>SimpleScriptContext</code> as its default <code>ScriptContext</code>.
   */
  public OrcScriptEngine() {
    this(new OrcBindings());
  }

  /**
   * Constructs an object of class OrcScriptEngine using the specified
   * <code>Bindings</code> as the <code>ENGINE_SCOPE</code>
   * <code>Bindings</code>.
   *
   * @param n
   */
  public OrcScriptEngine(final Bindings n) {
    super(n);
  }

  public class OrcCompiledScript extends CompiledScript {
    final CompiledCode code;

    /**
     * Constructs an object of class OrcCompiledScript.
     *
     * @param oilAstRoot Root node of the OIL AST from the compilation
     */
    /* default-access */OrcCompiledScript(final CompiledCode _code) {
      this.code = _code;
    }

    /*
     * (non-Javadoc)
     * @see javax.script.CompiledScript#eval(javax.script.ScriptContext)
     */
    @Override
    public Object eval(final ScriptContext ctx) throws ScriptException {
      final List<Object> pubs = new ArrayList<Object>();
      final OrcEventAction addPubToList = new OrcDesktopEventAction() {
        @Override
        public void published(final Object value) {
          pubs.add(value);
        }

        @Override
        public void caught(final Throwable e) {
          // TODO: Consider saving the exception and throwing it out of this
          // eval() invocation. Can't throw here, because we're in engine when
          // this OrcEventAction is called.
          try {
            ctx.getWriter().flush();
            if (e instanceof OrcException) {
              ctx.getErrorWriter().write(((OrcException) e).getMessageAndDiagnostics());
            } else {
              e.printStackTrace(new PrintWriter(ctx.getErrorWriter()));
            }
            ctx.getErrorWriter().flush();
          } catch (final IOException e1) {
            // Can't happen, according to API spec
            throw new AssertionError(e1);
          }
        }

        @Override
        public void haltedOrKilled() { /* Do nothing, handled for us by runSynchronous */
        }

        @Override
        public void other(final OrcEvent event) throws Exception {
          if (event instanceof PrintEvent) {
            try {
              final PrintEvent pe = (PrintEvent) event;
              ctx.getWriter().write(pe.text());
              ctx.getWriter().flush();
            } catch (final IOException e) {
              // Can't happen, according to API spec
              throw new AssertionError(e);
            }
          } else {
            super.other(event);
          }
        }
      };
      run(ctx, addPubToList);
      return pubs;
    }

    /**
     * Executes the program stored in this <code>CompiledScript</code> object.
     * This, like <code>eval</code>, runs synchronously. Instead of returning a
     * list of publications, <code>run</code> calls the <code>publish</code>
     * method of the given <code>OrcEventAction</code> object.
     *
     * @param ctx A <code>ScriptContext</code> that is used in the same way as
     *          the <code>ScriptContext</code> passed to the <code>eval</code>
     *          methods of <code>ScriptEngine</code>.
     * @param pubAct A <code>OrcEventAction</code> that receives publish
     *          messages for each published value from the script.
     * @throws ScriptException if an error occurs.
     * @throws NullPointerException if context is null.
     */
    public void run(final ScriptContext ctx, final OrcEventAction pubAct) throws ScriptException {
      Logger.julLogger().entering(getClass().getCanonicalName(), "run");
      // We make the assumption that the standard runtime implements SupportForSynchronousExecution.
      // JSR 223 requires the eval methods to run synchronously.
      final Runtime<CompiledCode> exec = OrcScriptEngine.this.getExecutor();
      ctx.setAttribute("context", ctx, ScriptContext.ENGINE_SCOPE); // Required by JSR-223 §SCR.4.3.4.1.2
      // TODO: Make ENGINE_SCOPE bindings visible in Orc execution?
      try {
        exec.runSynchronous(code, pubAct.asFunction());
      } catch (final InterruptedException e) {
        Thread.currentThread().interrupt();
      } catch (final OrcException e) {
        throw new OrcScriptException(e);
      } finally {
        exec.stop(); // kill threads and reclaim resources
        ctx.removeAttribute("context", ScriptContext.ENGINE_SCOPE);
        Logger.julLogger().exiting(getClass().getCanonicalName(), "run");
      }
    }

    /**
     * Convenience method for <code>run(getEngine().getContext(), pubAct)</code>
     * .
     *
     * @param pubAct A <code>OrcEventAction</code> that receives publish
     *          messages for each published value from the script.
     * @throws ScriptException if an error occurs.
     * @throws NullPointerException if context is null.
     */
    public void run(final OrcEventAction pubAct) throws ScriptException {
      run(getEngine().getContext(), pubAct);
    }

    /*
     * (non-Javadoc)
     * @see javax.script.CompiledScript#getEngine()
     */
    @Override
    public ScriptEngine getEngine() {
      return OrcScriptEngine.this;
    }

  }

  /*
   * (non-Javadoc)
   * @see javax.script.Compilable#compile(java.lang.String)
   */
  @Override
  public CompiledScript compile(final String script) throws ScriptException {
    return compile(new StringReader(script));
  }

  /*
   * (non-Javadoc)
   * @see javax.script.Compilable#compile(java.io.Reader)
   */
  @Override
  public CompiledScript compile(final Reader script) throws ScriptException {
    Logger.julLogger().entering(getClass().getCanonicalName(), "compile", script);
    try {
      final CompiledCode result = getCompiler().compile(script, asOrcBindings(getBindings(ScriptContext.ENGINE_SCOPE)), getContext().getErrorWriter());
      if (result == null) {
        throw new ScriptException("Compilation failed");
      } else {
        Logger.julLogger().exiting(getClass().getCanonicalName(), "compile", result);
        return new OrcCompiledScript(result);
      }
    } catch (final ScriptException e) {
      throw e;
    } catch (final IOException e) {
      throw new ScriptException(e);
    }
  }

  /*
   * (non-Javadoc)
   * @see javax.script.ScriptEngine#createBindings()
   */
  @Override
  public Bindings createBindings() {
    return new OrcBindings();
  }

  /*
   * (non-Javadoc)
   * @see javax.script.ScriptEngine#eval(java.lang.String,
   * javax.script.ScriptContext)
   */
  @Override
  public Object eval(final String script, final ScriptContext ctxt) throws ScriptException {
    return compile(script).eval(ctxt);
  }

  /*
   * (non-Javadoc)
   * @see javax.script.ScriptEngine#eval(java.io.Reader,
   * javax.script.ScriptContext)
   */
  @Override
  public Object eval(final Reader reader, final ScriptContext ctxt) throws ScriptException {
    return compile(reader).eval(ctxt);
  }

  /*
   * (non-Javadoc)
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
   * Set the <code>ScriptEngineFactory</code> instance to which this
   * <code>ScriptEngine</code> belongs.
   *
   * @param factory The <code>ScriptEngineFactory</code>
   */
  void setFactory(final OrcScriptEngineFactory owningFactory) {
    factory = owningFactory;
  }

  /**
   * @return The <code>Backend</code> which this <code>OrcScriptEngine</code> 
   * uses to compile and rune the Orc script.
   */
  @SuppressWarnings("unchecked")
  Backend<CompiledCode> getBackend() {
    synchronized (this) {
      OrcBindings b = asOrcBindings(getBindings(ScriptContext.ENGINE_SCOPE));
      BackendType k = b.backend();
      if (_backend == null) {
        backendType = k;
        if(k == TokenInterpreterBackend.it())
          _backend = (Backend<CompiledCode>)new StandardBackend();
        else if(k == OrctimizerInterpreterBackend.it())
          _backend = (Backend<CompiledCode>)new OrctimizerBackend();
        else if(k == PorcCompilerBackend.it())
          _backend = (Backend<CompiledCode>)new PorcBackend();
        else
          throw new IllegalArgumentException("Unknown backend: " + b.backend());      
      } else {
        if(k != backendType) {
          throw new UnsupportedOperationException("Backend change after creation is not supported");
        }
      }
    }
    return _backend;
  }

  /**
   * @return The <code>StandardOrcCompiler</code> which this
   *         <code>OrcScriptEngine</code> uses to compile the Orc script.
   */
  Compiler<CompiledCode> getCompiler() {
    return getBackend().compiler();
  }

  /**
   * @return The <code>StandardOrcRuntime</code> which this
   *         <code>OrcScriptEngine</code> uses to run the Orc script.
   */
  Runtime<CompiledCode> getExecutor() {
    synchronized (this) {
      if (_executor == null) {
        _executor = getBackend().createRuntime(asOrcBindings(getBindings(ScriptContext.ENGINE_SCOPE)));
      }
    }
    return _executor;
  }

  /**
   * Takes an unknown <code>Bindings</code> and makes an instance of
   * <code>OrcBindings</code>. Copies if needed; returns the argument unchanged
   * if it's already an <code>OrcBindings</code>.
   *
   * @param b The unknown <code>Bindings</code>
   * @return An <code>OrcBindings</code>
   */
  static OrcBindings asOrcBindings(final Bindings b) {
    if (b instanceof OrcBindings) {
      return (OrcBindings) b;
    } else {
      return new OrcBindings(b);
    }
  }

  /**
   * Provide an input stream to load the compiled code from. The code will be deserialized by
   * the backend.
   */
  public OrcCompiledScript loadDirectly(final InputStream in) throws LoadingException {
    return new OrcCompiledScript(getBackend().serializer().get().deserialize(in));
  }

  /**
   * Import an existing compiled object into a new OrcCompiledScript.
   */
  @SuppressWarnings("unchecked")
  public OrcCompiledScript importLoaded(final CompiledScript script) throws LoadingException {
    if(script instanceof OrcScriptEngine.OrcCompiledScript)
      return new OrcCompiledScript(((OrcCompiledScript)script).code);
    else
      throw new IllegalArgumentException("Provided compiled script is not of the correct time.");
  }   
}
