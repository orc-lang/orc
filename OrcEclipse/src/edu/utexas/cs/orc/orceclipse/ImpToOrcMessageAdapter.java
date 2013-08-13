//
// ImpToOrcMessageAdapter.java -- Java class ImpToOrcMessageAdapter.java
// Project OrcEclipse
//
// $Id$
//
// Created by jthywiss on Aug 15, 2009.
//
// Copyright (c) 2013 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package edu.utexas.cs.orc.orceclipse;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import orc.ast.AST;
import orc.compile.parse.OrcInputContext;
import orc.compile.parse.PositionWithFilename;
import orc.error.compiletime.CompilationException;
import orc.error.compiletime.CompileLogger;
import orc.error.compiletime.ParsingException;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.imp.parser.IMessageHandler;

import edu.utexas.cs.orc.orceclipse.build.OrcBuilder;

import scala.util.parsing.input.NoPosition$;
import scala.util.parsing.input.OffsetPosition;
import scala.util.parsing.input.Position;

/**
 * Wraps IMP's IMessageHandler interface in Orc's CompileMessageRecorder
 * interface.
 *
 * @author jthywiss
 */
public class ImpToOrcMessageAdapter implements CompileLogger {

	public static final Integer UNKNOWN_LINE = Integer.valueOf(-1);
	private final String sourceId;
	private final boolean parseOnly;
	private Severity maxSeverity = Severity.UNKNOWN;
	private ProblemMarkerCreator mainMarkerCreator;
	private final Map<String, ProblemMarkerCreator> contextMap = new HashMap<String, ProblemMarkerCreator>();

	/**
	 * Constructs an object of class ImpToOrcMessageAdapter.
	 *
	 * @param sourceId
	 * @param parseOnly
	 */
	public ImpToOrcMessageAdapter(final String sourceId, final boolean parseOnly) {
		this.sourceId = sourceId;
		this.parseOnly = parseOnly;
	}

	/* (non-Javadoc)
	 * @see orc.error.compiletime.CompileLogger#beginProcessing(orc.compile.parse.OrcInputContext)
	 */
	@Override
	public synchronized void beginProcessing(final OrcInputContext inputContext) {
		Assert.isTrue(contextMap.isEmpty(), "contextMap.isEmpty()"); //$NON-NLS-1$
		Assert.isTrue(mainMarkerCreator == null, "mainMarkerCreator == null"); //$NON-NLS-1$
		Assert.isNotNull(inputContext, "inputContext != null"); //$NON-NLS-1$
		contextMap.clear();
		mainMarkerCreator = markerCreatorForInputContext(inputContext);
		beginContext(inputContext, mainMarkerCreator);
		maxSeverity = Severity.UNKNOWN;
	}

	/* (non-Javadoc)
	 * @see orc.error.compiletime.CompileLogger#beginDependency(orc.compile.parse.OrcInputContext)
	 */
	@Override
	public void beginDependency(final OrcInputContext inputContext) {
		Assert.isNotNull(inputContext, "inputContext != null"); //$NON-NLS-1$
		final ProblemMarkerCreator markerCreator = markerCreatorForInputContext(inputContext);
		beginContext(inputContext, markerCreator);
	}

	/**
	 * @param inputContext
	 * @param markerCreator
	 */
	protected synchronized void beginContext(final OrcInputContext inputContext, final ProblemMarkerCreator markerCreator) {
		final ProblemMarkerCreator oldMapping = contextMap.put(inputContext.descr(), markerCreator);
		Assert.isTrue(oldMapping == null, "oldMapping == null"); //$NON-NLS-1$
	}

	/* (non-Javadoc)
	 * @see orc.error.compiletime.CompileLogger#recordMessage(orc.error.compiletime.CompileLogger.Severity, int, java.lang.String, scala.util.parsing.input.Position, orc.ast.AST, java.lang.Throwable)
	 */
	@Override
	public void recordMessage(final Severity severity, final int code, final String message, final Position position, final AST astNode, final Throwable exception) {

		synchronized (this) {
			maxSeverity = severity.ordinal() > maxSeverity.ordinal() ? severity : maxSeverity;
		}

		final String markerText = message + (exception instanceof CompilationException ? " [[OrcWiki:" + exception.getClass().getSimpleName() + "]]" : ""); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

		final int eclipseSeverity = eclipseSeverityFromOrcSeverity(severity);

		Position locationNN = position;
		if (locationNN == null) {
			locationNN = NoPosition$.MODULE$;
		}
		ProblemMarkerCreator markerCreator = null;
		if (locationNN instanceof PositionWithFilename) {
			final String descr = ((PositionWithFilename) locationNN).filename();
			if (descr != null && descr.length() > 0) {
				markerCreator = contextMap.get(descr);
			}
		}
		final boolean dontHaveActualResource = markerCreator == null;
		if (markerCreator == null) {
			markerCreator = mainMarkerCreator;
			locationNN = NoPosition$.MODULE$;
		}

		// IMP's AnnotationCreator breaks for our UNKNOWN offset values
		int safeStartOffset = 0;
		int safeEndOffset = -1;
		if (locationNN instanceof OffsetPosition && ((OffsetPosition) locationNN).offset() >= 0) {
			safeStartOffset = safeEndOffset = ((OffsetPosition) locationNN).offset();
		}
		final int safeLineNumber = locationNN.line() >= 1 ? locationNN.line() : -1;

		markerCreator.addMarker(
				exception instanceof ParsingException ? OrcBuilder.PARSE_PROBLEM_MARKER_ID : OrcBuilder.PROBLEM_MARKER_ID,
				markerText,
				eclipseSeverity,
				code,
				(dontHaveActualResource && position != null) ? position.toString() : null,
				safeStartOffset,
				safeEndOffset,
				safeLineNumber,
				exception				
		);
	}

	/* (non-Javadoc)
	 * @see orc.error.compiletime.CompileLogger#recordMessage(orc.error.compiletime.CompileLogger.Severity, int, java.lang.String, scala.util.parsing.input.Position, java.lang.Throwable)
	 */
	@Override
	public void recordMessage(final Severity severity, final int code, final String message, final Position position, final Throwable exception) {
		recordMessage(severity, code, message, position, null, exception);
	}

	/* (non-Javadoc)
	 * @see orc.error.compiletime.CompileLogger#recordMessage(orc.error.compiletime.CompileLogger.Severity, int, java.lang.String, scala.util.parsing.input.Position, orc.AST)
	 */
	@Override
	public void recordMessage(final Severity severity, final int code, final String message, final Position position, final AST astNode) {
		recordMessage(severity, code, message, position, astNode, null);
	}

	/* (non-Javadoc)
	 * @see orc.error.compiletime.CompileLogger#recordMessage(orc.error.compiletime.CompileLogger.Severity, int, java.lang.String)
	 */
	@Override
	public void recordMessage(final Severity severity, final int code, final String message) {
		recordMessage(severity, code, message, null, null, null);
	}

	protected ProblemMarkerCreator markerCreatorForInputContext(final OrcInputContext inputContext) {
		if (inputContext == null) {
			return null;
		}
		final URI inputUri = inputContext.toURI();
		if (!inputUri.isAbsolute() || !"file".equalsIgnoreCase(inputUri.getScheme())) { //$NON-NLS-1$
			return null;
		}
		final IFile[] files = ResourcesPlugin.getWorkspace().getRoot().findFilesForLocationURI(inputUri);
		if (files.length < 1 || files[0] == null || !files[0].exists()) {
			return null;
		} else {
			return new ProblemMarkerCreator(files[0], parseOnly ? OrcBuilder.PARSE_PROBLEM_MARKER_ID : OrcBuilder.PROBLEM_MARKER_ID, sourceId, true);
		}
	}

	static protected int eclipseSeverityFromOrcSeverity(final Severity severity) {
		int eclipseSeverity = IMarker.SEVERITY_ERROR; // Default
		switch (severity) {
		case DEBUG:
		case INFO:
		case NOTICE:
			eclipseSeverity = IMarker.SEVERITY_INFO;
			break;
		case WARNING:
			eclipseSeverity = IMarker.SEVERITY_WARNING;
			break;
		case ERROR:
		case FATAL:
		case INTERNAL:
			eclipseSeverity = IMarker.SEVERITY_ERROR;
			break;
		default:
			// Leave default value
		}
		return eclipseSeverity;
	}

	/* (non-Javadoc)
	 * @see orc.error.compiletime.CompileLogger#getMaxSeverity()
	 */
	@Override
	public synchronized Severity getMaxSeverity() {
		return maxSeverity;
	}

	protected void endContext(final OrcInputContext inputContext) {
		/* Nothing to do here. */
	}

	/* (non-Javadoc)
	 * @see orc.error.compiletime.CompileLogger#endDependency(orc.compile.parse.OrcInputContext)
	 */
	@Override
	public void endDependency(final OrcInputContext inputContext) {
		Assert.isNotNull(inputContext, "inputContext != null"); //$NON-NLS-1$
		endContext(inputContext);
	}

	/* (non-Javadoc)
	 * @see orc.error.compiletime.CompileLogger#endProcessing(orc.compile.parse.OrcInputContext)
	 */
	@Override
	public synchronized void endProcessing(final OrcInputContext inputContext) {
		Assert.isNotNull(inputContext, "inputContext != null"); //$NON-NLS-1$
		endContext(inputContext);
		mainMarkerCreator = null;
		for (final Map.Entry<String, ProblemMarkerCreator> entry : contextMap.entrySet()) {
			if (entry.getValue() != null) {
				entry.getValue().endMessages();
			}
		}
		contextMap.clear();
	}

	private static class TypeAndAttributes extends HashMap<String, Object> {
		private static final long serialVersionUID = -872546911743411691L;
		private final String typeId;

		public TypeAndAttributes(final String typeId) {
			super();
			this.typeId = typeId;
		}

		public String getType() {
			return typeId;
		}

	}

	/**
	 * A ProblemMarkerCreator manages creation of Eclipse Markers for a
	 * given workspace Resource.  Problems are enqueued via addMarker calls,
	 * calls, and then the resource is updated in a single operation upon
	 * the endMessages call.  Existing markers of the given type are deleted
	 * during endMessages, unless they match an enqueued marker.  This
	 * reduces superfluous marker update notifications.
	 *
	 * Derived from org.eclipse.imp.builder/MarkerCreatorWithBatching.java,
	 * Revision 22670 (10 Sep 2010), trunk rev as of 11 Aug 2013
	 *
	 * @see org.eclipse.core.resources.IMarker
	 * @see org.eclipse.core.resources.IResource
	 */
	public static class ProblemMarkerCreator implements IMessageHandler {
		public static final String ERROR_CODE_KEY = "errorCode"; //$NON-NLS-1$
		protected final Collection<String> dontCompareAttributes = Arrays.asList(IMarker.SOURCE_ID);

		protected final IResource resource;
		protected final String baseMarkerType;
		protected final String sourceId;
		protected final boolean includeSubtypes;
		protected Map<Integer, List<TypeAndAttributes>> entries; // Map of line number to list of marker attributes for line

		/**
		 * Constructs an object of class ProblemMarkerCreator.
		 *
		 * @param resource
		 * @param baseMarkerType
		 * @param sourceId
		 * @param includeSubtypes
		 */
		public ProblemMarkerCreator(final IResource resource, final String baseMarkerType, final String sourceId, final boolean includeSubtypes) {
			this.resource = resource;
			this.baseMarkerType = baseMarkerType;
			this.sourceId = sourceId;
			this.includeSubtypes = includeSubtypes;
			this.entries = new HashMap<Integer, List<TypeAndAttributes>>();
		}

		/**
		 * @param markerType
		 * @param message
		 * @param severity
		 * @param code
		 * @param location
		 * @param charStart
		 * @param charEnd
		 * @param lineNumber
		 * @param exception
		 */
		public synchronized void addMarker(final String markerType, final String message, final int severity, final int code, final String location, final int charStart, final int charEnd, final int lineNumber, final Throwable exception) {
			final TypeAndAttributes attributes = new TypeAndAttributes(markerType != null ? markerType : baseMarkerType);
			if (message != null) {
				attributes.put(IMarker.MESSAGE, message);
			}
			attributes.put(IMarker.SEVERITY, Integer.valueOf(severity));
			attributes.put(ERROR_CODE_KEY, Integer.valueOf(code));
			if (location != null) {
				attributes.put(IMarker.LOCATION, location);
			}
			if (charStart <= charEnd) {
				attributes.put(IMarker.CHAR_START, Integer.valueOf(charStart));
				attributes.put(IMarker.CHAR_END, Integer.valueOf(charEnd));
			}
			final Integer lineKey = lineNumber > 0 ? Integer.valueOf(lineNumber) : UNKNOWN_LINE;
			if (lineNumber > 0) {
				attributes.put(IMarker.LINE_NUMBER, lineKey);
			}
			if (sourceId != null) {
				attributes.put(IMarker.SOURCE_ID, sourceId);
			}
			//attributes.put(OrcBuilder.COMPILE_EXCEPTION_NAME, exception != null ? exception.getClass().getCanonicalName() : null);

			List<TypeAndAttributes> lineEntries = entries.get(lineKey);
			if (lineEntries == null) {
				lineEntries = new ArrayList<TypeAndAttributes>();
				entries.put(lineKey, lineEntries);
			}
			lineEntries.add(attributes);
		}

		/* (non-Javadoc)
		 * @see org.eclipse.imp.parser.IMessageHandler#endMessages()
		 */
		public synchronized void endMessages() {
			if (resource.exists()) {
				final IWorkspaceRunnable action = new IWorkspaceRunnable() {

					@Override
					public void run(final IProgressMonitor monitor) throws CoreException {
						if (entries != null) {
							final IMarker[] oldMarkers = resource.findMarkers(baseMarkerType, includeSubtypes, IResource.DEPTH_ZERO);
							for (IMarker oldMarker : oldMarkers) {
								final Map<String, Object> oldAttributes = oldMarker.getAttributes();
								final Object oldLineKey = oldAttributes.get(IMarker.LINE_NUMBER);
								final List<TypeAndAttributes> lineEntries = entries.get(oldLineKey != null ? oldLineKey : UNKNOWN_LINE);
								if (lineEntries != null) {
									for (final Map<String, Object> newAttributes : lineEntries) {
										if (isSameMarker(oldAttributes, newAttributes)) {
											lineEntries.remove(newAttributes);
											oldMarker = null;
											break;
										}
									}
								}
								if (oldMarker != null)
									oldMarker.delete();
							}
							for (final List<TypeAndAttributes> lineEntries : entries.values()) {
								for (final TypeAndAttributes entry : lineEntries) {
									final IMarker marker = resource.createMarker(entry.getType());
									marker.setAttributes(entry);
								}
							}
						} else
							resource.deleteMarkers(baseMarkerType, includeSubtypes, IResource.DEPTH_ZERO);
					}
				};
				try {
					final IProgressMonitor progressMonitor = new NullProgressMonitor();
					resource.getWorkspace().run(action, resource, IWorkspace.AVOID_UPDATE, progressMonitor);
				} catch (final CoreException e) {
					Activator.log(e);
				}
			}
		}

		/* (non-Javadoc)
		 * @see org.eclipse.imp.parser.IMessageHandler#clearMessages()
		 */
		@Override
		public void clearMessages() {
			throw new UnsupportedOperationException("ProblemMarkerCreator.clearMessages is not implemented."); //$NON-NLS-1$
		}

		/* (non-Javadoc)
		 * @see org.eclipse.imp.parser.IMessageHandler#startMessageGroup(java.lang.String)
		 */
		@Override
		public void startMessageGroup(final String groupName) {
			throw new UnsupportedOperationException("ProblemMarkerCreator.startMessageGroup is not implemented."); //$NON-NLS-1$
		}

		/* (non-Javadoc)
		 * @see org.eclipse.imp.parser.IMessageHandler#endMessageGroup()
		 */
		@Override
		public void endMessageGroup() {
			throw new UnsupportedOperationException("ProblemMarkerCreator.endMessageGroup is not implemented."); //$NON-NLS-1$
		}

		protected boolean isSameMarker(final Map<String, Object> oldAttributes, final Map<String, Object> newAttributes) {
			final Set<String> oldKeys = oldAttributes.keySet();
			final Set<String> newKeys = newAttributes.keySet();
			if (oldKeys.size() != newKeys.size())
				return false;
			for (final String key : newKeys) {
				if (dontCompareAttributes.contains(key))
					continue;
				if (!oldAttributes.containsKey(key))
					return false;
				final Object oldValue = oldAttributes.get(key);
				final Object newValue = newAttributes.get(key);
				if (oldValue == newValue)
					continue;
				if (oldValue == null)
					return false;
				if (newValue == null)
					return false;
				if (!oldValue.equals(newValue))
					return false;
			}
			return true;
		}

		/* (non-Javadoc)
		 * @see org.eclipse.imp.parser.IMessageHandler#handleSimpleMessage(java.lang.String, int, int, int, int, int, int)
		 */
		@Deprecated
		@Override
		public void handleSimpleMessage(final String msg, final int startOffset, final int endOffset, final int startCol, final int endCol, final int startLine, final int endLine) {
			throw new UnsupportedOperationException("ProblemMarkerCreator.handleSimpleMessage should no longer be called."); //$NON-NLS-1$
		}

	}

}
