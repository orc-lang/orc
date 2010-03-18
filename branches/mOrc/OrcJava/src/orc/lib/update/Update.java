//
// Update.java -- Java class Update
// Project OrcJava
//
// $Id$
//
// Created by jthywiss on Mar 16, 2010.
//
// Copyright (c) 2010 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.lib.update;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOError;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Iterator;

import orc.Config;
import orc.ast.oil.TokenContinuation;
import orc.ast.oil.expression.AstNode;
import orc.ast.oil.expression.Def;
import orc.ast.oil.expression.Expression;
import orc.ast.oil.visitor.SiteResolver;
import orc.ast.oil.visitor.TailCallMarker;
import orc.ast.oil.visitor.Walker;
import orc.ast.xml.Oil;
import orc.error.compiletime.CompilationException;
import orc.error.compiletime.typing.TypeException;
import orc.error.runtime.SiteException;
import orc.error.runtime.TokenException;
import orc.runtime.Args;
import orc.runtime.OrcEngine;
import orc.runtime.Token;
import orc.runtime.sites.Site;
import orc.runtime.values.Value;
import orc.type.Type;
import orc.type.structured.ArrowType;

import org.kohsuke.args4j.CmdLineException;

/**
 * Update a running Orc program to the supplied OIL program. One argument is
 * expected, an OIL file name.
 * 
 * @author jthywiss
 */
public class Update extends Site {
	//FIXME: This should be a threaded site, and the OIL loading should execute in parallel with the engine.

	/*
	 * (non-Javadoc)
	 * @see orc.runtime.sites.Site#callSite(orc.runtime.Args, orc.runtime.Token)
	 */
	@Override
	public void callSite(final Args args, final Token caller) throws TokenException {
		boolean updateSuceeded = false;
		try {
			updateSuceeded = update(caller.getEngine(), caller.getNode().getRoot(), new File(args.stringArg(0)));
		} catch (final CompilationException e) {
			throw new SiteException(e.getMessage(), e);
		} catch (final IOException e) {
			throw new SiteException(e.getMessage(), e);
		}
		if (updateSuceeded) {
			caller.resume(Value.signal());
		} else {
			caller.die();
		}
	}

	/**
	 * Update the OIL AST <code>oldOilAst</code>, running in the Orc engine
	 * <code>engine</code>, to the AST found in OIL file <code>newOilFile</code>
	 * , by moving currently executing tokens to the new AST. This will result
	 * in the engine being suspended during the update. When the update
	 * successfully completes, the engine's config will reflect the new source
	 * file.
	 * 
	 * @param engine OrcEngine in which the update is to occur (not null)
	 * @param oldOilAst The root of the OIL AST currently running in the engine (not null)
	 * @param newOilFile File for the new OIL AST (not null)
	 * @return true if the update succeeded, false for "unsafe now, try later"
	 * @throws IOException If new OIL file cannot be read
	 * @throws CompilationException If new OIL file fails to unmarshal or resolve
	 * @throws NullPointerException If any param is null
	 */
	public boolean update(final OrcEngine engine, final AstNode oldOilAst, final File newOilFile) throws CompilationException, IOException {
		final Expression newOilAst = loadNewProgram(newOilFile, engine);
		final AstEditScript editList = AstEditScript.computeEditScript(oldOilAst, newOilAst);
		if (editList != null && !editList.isEmpty()) {
			suspendEngine(engine);
			if (!isSafeState(engine, oldOilAst, newOilAst, editList)) {
				return false;
			}
			migrateTokens(engine, oldOilAst, newOilAst, editList);
			updateConfig(engine, newOilFile);
			resumeEngine(engine);
		} else {
			System.err.println("No changes");
		}
		return true;
	}

	/**
	 * @param newOilFile File for the new OIL AST (not null)
	 * @param engine OrcEngine in which the update is to occur (not null)
	 * @return the new OIL AST
	 * @throws IOException If new OIL file cannot be read
	 * @throws CompilationException If new OIL file fails to unmarshal or resolve 
	 */
	private Expression loadNewProgram(final File newOilFile, final OrcEngine engine) throws IOException, CompilationException {
		//FIXME: This is cut-and-pasted -- refactor back into OrcCompiler & Config & OrcEngine
		final Config config = engine.getConfig();
		final Oil oil = Oil.fromXML(new InputStreamReader(new FileInputStream(newOilFile)));
		Expression oilAst = oil.unmarshal();
		oilAst = SiteResolver.resolve(oilAst, config); //TODO: config only needed for ClassLoader -- create a new site resolution environment class
		oilAst.accept(new Walker() {
			@Override
			public void enter(final Def def) {
				def.body.accept(new TailCallMarker());
			};
		});
		oilAst.initParents();
		final TokenContinuation K = new TokenContinuation() {
			public void execute(final Token t) {
				t.publish();
				t.die();
			}
		};
		oilAst.setPublishContinuation(K);
		oilAst.populateContinuations();
		return oilAst;
	}

	/**
	 * @param engine
	 */
	private void suspendEngine(final OrcEngine engine) {
		engine.pause();
	}

	/**
	 * @param oldOilAst
	 * @param newOilAst
	 * @param editList
	 * @return
	 */
	private boolean isSafeState(final OrcEngine engine, final AstNode oldOilAst, final AstNode newOilAst, final AstEditScript editList) {
		final Iterator<Token> tokenIterator = engine.tokenIterator();
		while (tokenIterator.hasNext()) {
			final Token token = tokenIterator.next();
			for (final AstEditOperation editOperation : editList) {
				if (!editOperation.isTokenSafe(token)) {
					return false;
				}
			}
		}
		return true;
	}

	/**
	 * @param oldOilAst
	 * @param newOilAst
	 * @param editList
	 */
	private void migrateTokens(final OrcEngine engine, final AstNode oldOilAst, final AstNode newOilAst, final AstEditScript editList) {
		final Iterator<Token> tokenIterator = engine.tokenIterator();
		tokenLoop: while (tokenIterator.hasNext()) {
			final Token token = tokenIterator.next();
			for (final AstEditOperation editOperation : editList) {
				if (editOperation.migrateToken(token)) {
					continue tokenLoop;
				}
			}
			// ALL tokens in the old tree MUST be covered by some edit operation.
			// Assuming tokens not migrated belong to other ASTs.
			System.err.println("AstEditScript did not migrate Token " + token);
		}
	}

	/**
	 * @param engine
	 * @param newOilFile
	 */
	private void updateConfig(final OrcEngine engine, final File newOilFile) {
		final Config config = engine.getConfig();
		try {
			config.setInputFile(newOilFile);
		} catch (final CmdLineException e) {
			// File disappeared between reading it and now?
			throw new IOError(e);
		}
	}

	/**
	 * @param engine
	 */
	private void resumeEngine(final OrcEngine engine) {
		engine.unpause();
	}

	/*
	 * (non-Javadoc)
	 * @see orc.runtime.sites.Site#type()
	 */
	@Override
	public Type type() throws TypeException {
		return new ArrowType(Type.STRING, Type.SIGNAL);
	}

}
