//
// OrcParseController.java -- Java class OrcParseController
// Project OrcEclipse
//
// $Id$
//
// Created by jthywiss on Aug 9, 2009.
//
// Copyright (c) 2013 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package edu.utexas.cs.orc.orceclipse.parse;

import java.net.URI;
import java.util.Iterator;

import orc.OrcCompilationOptions;
import orc.ast.AST;
import orc.compile.CompilerOptions;
import orc.compile.parse.OrcIncludeParser;
import orc.compile.parse.OrcInputContext;
import orc.compile.parse.OrcProgramParser;
import orc.compile.parse.OrcStringInputContext;
import orc.error.compiletime.CompileLogger.Severity;
import orc.error.compiletime.ParsingException;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.imp.model.ISourceProject;
import org.eclipse.imp.parser.IMessageHandler;
import org.eclipse.imp.parser.ISourcePositionLocator;
import org.eclipse.imp.parser.ParseControllerBase;
import org.eclipse.imp.parser.SimpleAnnotationTypeInfo;
import org.eclipse.imp.preferences.IPreferencesService;
import org.eclipse.imp.preferences.PreferenceConstants;
import org.eclipse.imp.runtime.RuntimePlugin;
import org.eclipse.imp.services.IAnnotationTypeInfo;
import org.eclipse.imp.services.ILanguageSyntaxProperties;
import org.eclipse.jface.text.IRegion;

import scala.util.parsing.combinator.Parsers.NoSuccess;
import scala.util.parsing.combinator.Parsers.ParseResult;
import edu.utexas.cs.orc.orceclipse.Activator;
import edu.utexas.cs.orc.orceclipse.ImpToOrcMessageAdapter;
import edu.utexas.cs.orc.orceclipse.OrcConfigSettings;

/**
 * A parsing environment (file, project, etc.) that IMP constructs when it will request Orc parsing.
 *
 * @author jthywiss
 */
public class OrcParseController extends ParseControllerBase {

	/**
	 * Singleton that provides particular information about Orc's syntax to IMP.
	 *
	 * @author jthywiss
	 */
	public static class OrcSyntaxProperties implements ILanguageSyntaxProperties {

		/* (non-Javadoc)
		 * @see org.eclipse.imp.services.ILanguageSyntaxProperties#getBlockCommentContinuation()
		 */
		@Override
		public String getBlockCommentContinuation() {
			return null;
		}

		/* (non-Javadoc)
		 * @see org.eclipse.imp.services.ILanguageSyntaxProperties#getBlockCommentEnd()
		 */
		@Override
		public String getBlockCommentEnd() {
			return "-}"; //$NON-NLS-1$
		}

		/* (non-Javadoc)
		 * @see org.eclipse.imp.services.ILanguageSyntaxProperties#getBlockCommentStart()
		 */
		@Override
		public String getBlockCommentStart() {
			return "{-"; //$NON-NLS-1$
		}

		/* (non-Javadoc)
		 * @see org.eclipse.imp.services.ILanguageSyntaxProperties#getFences()
		 */
		@Override
		public String[][] getFences() {
			return orcFences;
		}

		private static String[][] orcFences = { { "(", ")" }, { "[", "]" }, { "{", "}" }, { "{-", "-}" }, }; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$ //$NON-NLS-8$

		/**
		 * Not used. Orc doesn't have multi-component identifiers at present.
		 * @param ident ignored
		 * @return Dummy value -- an int array with one zero element
		 */
		@Override
		public int[] getIdentifierComponents(final String ident) {
			return dummyComponents;
		}

		private static int dummyComponents[] = { 0 };

		/* (non-Javadoc)
		 * @see org.eclipse.imp.services.ILanguageSyntaxProperties#getIdentifierConstituentChars()
		 */
		@Override
		public String getIdentifierConstituentChars() {
			return "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789_'"; //$NON-NLS-1$
		}

		/* (non-Javadoc)
		 * @see org.eclipse.imp.services.ILanguageSyntaxProperties#getSingleLineCommentPrefix()
		 */
		@Override
		public String getSingleLineCommentPrefix() {
			return "--"; //$NON-NLS-1$
		}

	}

	private final static SimpleAnnotationTypeInfo annotationTypeInfo = new SimpleAnnotationTypeInfo();
	private final static OrcSyntaxProperties syntaxProperties = new OrcSyntaxProperties();
	private OrcSourcePositionLocator sourcePositionLocator;
	private String currentParseString;
	private AST currentAst;
	private OrcLexer lexer;

	/**
	 * Constructs an object of class OrcParseController.
	 */
	public OrcParseController() {
		super(Activator.getInstance().getLanguageID());
		// The following might seem to be needed, but the editor handles problem markers better without it!
		//		if (!annotationTypeInfo.getProblemMarkerTypes().contains(OrcBuilder.PROBLEM_MARKER_ID)) {
		//			annotationTypeInfo.addProblemMarkerType(OrcBuilder.PROBLEM_MARKER_ID);
		//		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.imp.parser.ParseControllerBase#initialize(org.eclipse.core.runtime.IPath, org.eclipse.imp.model.ISourceProject, org.eclipse.imp.parser.IMessageHandler)
	 */
	@Override
	public void initialize(final IPath filePath, final ISourceProject project, final IMessageHandler handler) {
		super.initialize(filePath, project, handler);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.imp.parser.IParseController#getSourcePositionLocator()
	 */
	@Override
	public ISourcePositionLocator getSourcePositionLocator() {
		if (sourcePositionLocator == null) {
			sourcePositionLocator = new OrcSourcePositionLocator(this);
		}
		return sourcePositionLocator;
	}

	/**
	 * Returns an Iterator that iterates over the tokens contained within the given region, including any tokens that are only partially contained.
	 * <p>
	 * <code>parse</code> must be called before this method is called.
	 *
	 * @see org.eclipse.imp.parser.IParseController#getTokenIterator(org.eclipse.jface.text.IRegion)
	 */
	@SuppressWarnings("rawtypes")
	@Override
	public Iterator getTokenIterator(final IRegion region) {
		// Will throw a NullPointerException if called before parsing
		return lexer.iterator(region);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.imp.parser.IParseController#getAnnotationTypeInfo()
	 */
	@Override
	public IAnnotationTypeInfo getAnnotationTypeInfo() {
		return annotationTypeInfo;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.imp.parser.IParseController#getSyntaxProperties()
	 */
	@Override
	public ILanguageSyntaxProperties getSyntaxProperties() {
		return syntaxProperties;
	}

	/**
	 * Parse the given source and return the resulting AST.
	 * The AST should is cached, so that immediately after a
	 * successful parse, {@link #getCurrentAst()} returns the same AST as this method
	 * produced.
	 * <p>
	 * {@link #initialize(IPath, ISourceProject, IMessageHandler)} must be called before calling this method.
	 *
	 * @return the AST, if any, resulting from the parse
	 * @param contents String containing the source text to parse
	 * @param monitor ProgressMonitor to check for cancellation
	 * @see org.eclipse.imp.parser.IParseController#parse(java.lang.String, org.eclipse.core.runtime.IProgressMonitor)
	 */
	@Override
	public AST parse(final String contents, final IProgressMonitor monitor) {

		currentParseString = contents;

		OrcConfigSettings config;
		try {
			config = new OrcConfigSettings(getProject().getRawProject(), null);
		} catch (final CoreException e) {
			// Shouldn't happen with project settings only
			Activator.logAndShow(e);
			return currentAst;
		}

		final IPath absolutePath = getProject().getRawProject().getLocation().append(getPath());

		final OrcStringInputContext ic = new OrcStringInputContext(contents) {
			@Override
			public String descr() {
				return absolutePath.toFile().toString();
			}
			@Override
			public URI toURI() {
				return absolutePath.toFile().toURI();
			}
		};

		final ImpToOrcMessageAdapter compileLogger = new ImpToOrcMessageAdapter(Activator.getInstance().getID() + ".parse.orcParseController", true); //$NON-NLS-1$
		final CompilerOptions co = new CompilerOptions(config, compileLogger);
		
		compileLogger.beginProcessing(ic);
		if (lexer == null) {
			lexer = new OrcLexer(this);
		}

		final orc.OrcCompilerRequires dummyEnvServices = new orc.OrcCompilerRequires() {
			@Override
			public OrcInputContext openInclude(final String includeName, final OrcInputContext orcinputcontext, final OrcCompilationOptions orcoptions) {
				return new OrcStringInputContext("") { //$NON-NLS-1$
					@Override
					public String descr() {
						return includeName;
					}
				};
			}

			@Override
			@SuppressWarnings({ "unchecked", "rawtypes" })
			public Class loadClass(final String s) {
				return null;
			}
		};

		try {
			ParseResult<?> result;
			if (!Activator.isOrcIncludeFile(absolutePath)) {
				result = OrcProgramParser.apply(ic, co, dummyEnvServices);
			} else {
				result = OrcIncludeParser.apply(ic, co, dummyEnvServices);
			}
			if (result.successful()) {
				currentAst = (AST) result.get();
			} else {
				final NoSuccess n = (NoSuccess) result;
				compileLogger.recordMessage(Severity.FATAL, 0, n.msg(), n.next().pos(), null, new ParsingException(n.msg(), n.next().pos()));
			}
		} catch (final Exception e) {
			compileLogger.recordMessage(Severity.FATAL, 0, e.getLocalizedMessage(), null, null, e);
		} finally {
			compileLogger.endProcessing(ic);
		}

		// Walk AST and tie id refs to id defs
		//		parser.resolve(currentAst);

		maybeDumpTokens();

		return currentAst;
	}

	/**
	 * @return the current parse string, as passed to the last invocation of {@link #parse(String, IProgressMonitor)}, or null
	 */
	public String getCurrentParseString() {
		return currentParseString;
	}

	/**
	 * @return the current AST, as created by the last invocation of {@link #parse(String, IProgressMonitor)}, or null
	 */
	@Override
	public AST getCurrentAst() {
		return currentAst;
	}

	/**
	 * @return the {@link OrcLexer} for this ParseController, as created by the
	 *         last invocation of {@link #parse(String, IProgressMonitor)}, or
	 *         null
	 */
	public OrcLexer getLexer() {
		return lexer;
	}

	protected void maybeDumpTokens() {
		final IPreferencesService ourPrefSvc = Activator.getInstance().getPreferencesService();
		final IPreferencesService impPrefSvc = RuntimePlugin.getInstance().getPreferencesService();

		final boolean dump = ourPrefSvc.isDefined(PreferenceConstants.P_DUMP_TOKENS) ? ourPrefSvc.getBooleanPreference(PreferenceConstants.P_DUMP_TOKENS) : impPrefSvc.getBooleanPreference(PreferenceConstants.P_DUMP_TOKENS);

		if (dump) {
			for (final OrcLexer.OrcToken currToken : lexer) {
				System.out.println(currToken);
			}
			System.out.println("END-OF-FILE"); //$NON-NLS-1$
		}
	}
}
