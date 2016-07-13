//
// EclipseToOrcMessageAdapter.java -- Java class EclipseToOrcMessageAdapter.java
// Project OrcEclipse
//
// Created by jthywiss on Aug 15, 2009.
//
// Copyright (c) 2016 The University of Texas at Austin. All rights reserved.
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

import scala.util.parsing.input.NoPosition$;
import scala.util.parsing.input.OffsetPosition;
import scala.util.parsing.input.Position;

import orc.ast.AST;
import orc.compile.parse.OrcInputContext;
import orc.compile.parse.PositionWithFilename;
import orc.error.compiletime.CompilationException;
import orc.error.compiletime.CompileLogger;
import orc.error.compiletime.ParsingException;

import edu.utexas.cs.orc.orceclipse.build.OrcBuilder;

/**
 * Manages Eclipse resource markers for an Orc compilation.
 *
 * @author jthywiss
 */
public class EclipseToOrcMessageAdapter implements CompileLogger {

    /** Dummy value for unknown line numbers */
    public static final Integer UNKNOWN_LINE = Integer.valueOf(-1);
    private final String sourceId;
    private final boolean parseOnly;
    private Severity maxSeverity = Severity.UNKNOWN;
    private ProblemMarkerCreator mainMarkerCreator;
    private final Map<String, ProblemMarkerCreator> contextMap = new HashMap<String, ProblemMarkerCreator>();

    /**
     * Constructs an object of class EclipseToOrcMessageAdapter.
     *
     * @param sourceId source ID attribute to use on problem markers
     * @param parseOnly if true, use the "parse problem" marker type
     */
    public EclipseToOrcMessageAdapter(final String sourceId, final boolean parseOnly) {
        this.sourceId = sourceId;
        this.parseOnly = parseOnly;
    }

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

    @Override
    public void recordMessage(final Severity severity, final int code, final String message, final Position position, final AST astNode, final Throwable exception) {
        Assert.isTrue(mainMarkerCreator != null, "mainMarkerCreator != null"); //$NON-NLS-1$

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

        int safeStartOffset = -1;
        int safeEndOffset = -1;
        if (locationNN instanceof OffsetPosition && ((OffsetPosition) locationNN).offset() >= 0) {
            safeStartOffset = safeEndOffset = ((OffsetPosition) locationNN).offset();
        }
        final int safeLineNumber = locationNN.line() >= 1 ? locationNN.line() : UNKNOWN_LINE.intValue();

        markerCreator.addMarker(exception instanceof ParsingException ? OrcBuilder.PARSE_PROBLEM_MARKER_ID : OrcBuilder.PROBLEM_MARKER_ID, markerText, eclipseSeverity, code, dontHaveActualResource && position != null ? position.toString() : null, safeStartOffset, safeEndOffset, safeLineNumber, exception);
    }

    @Override
    public void recordMessage(final Severity severity, final int code, final String message, final Position position, final Throwable exception) {
        recordMessage(severity, code, message, position, null, exception);
    }

    @Override
    public void recordMessage(final Severity severity, final int code, final String message, final Position position, final AST astNode) {
        recordMessage(severity, code, message, position, astNode, null);
    }

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
        if (files.length < 1 || files[0] == null) {
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

    @Override
    public synchronized Severity getMaxSeverity() {
        return maxSeverity;
    }

    protected void endContext(final OrcInputContext inputContext) {
        /* Nothing to do here. */
    }

    @Override
    public void endDependency(final OrcInputContext inputContext) {
        Assert.isNotNull(inputContext, "inputContext != null"); //$NON-NLS-1$
        endContext(inputContext);
    }

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
     * A ProblemMarkerCreator manages creation of Eclipse Markers for a given
     * workspace Resource. Problems are enqueued via addMarker calls, calls, and
     * then the resource is updated in a single operation upon the endMessages
     * call. Existing markers of the given type are deleted during endMessages,
     * unless they match an enqueued marker. This reduces superfluous marker
     * update notifications. Derived from
     * org.eclipse.imp.builder/MarkerCreatorWithBatching.java, Revision 22670
     * (10 Sep 2010), trunk rev as of 11 Aug 2013
     *
     * @see org.eclipse.core.resources.IMarker
     * @see org.eclipse.core.resources.IResource
     */
    public static class ProblemMarkerCreator {
        /** Resource marker attribute key for error code numbers. */
        public static final String ERROR_CODE_KEY = "errorCode"; //$NON-NLS-1$
        protected final Collection<String> dontCompareAttributes = Arrays.asList(IMarker.SOURCE_ID);

        protected final IResource resource;
        protected final String baseMarkerType;
        protected final String sourceId;
        protected final boolean includeSubtypes;
        /** Map of line number to list of marker attributes for line */
        protected Map<Integer, List<TypeAndAttributes>> entries;

        /**
         * Constructs an object of class ProblemMarkerCreator.
         *
         * @param resource the IResource to create the markers on
         * @param baseMarkerType the base type of markers to replace, and the
         *            default type to create
         * @param sourceId string indicating the source of the marker
         * @param includeSubtypes include subtypes when matching and replacing
         *            old markers
         */
        public ProblemMarkerCreator(final IResource resource, final String baseMarkerType, final String sourceId, final boolean includeSubtypes) {
            this.resource = resource;
            this.baseMarkerType = baseMarkerType;
            this.sourceId = sourceId;
            this.includeSubtypes = includeSubtypes;
            this.entries = new HashMap<Integer, List<TypeAndAttributes>>();
        }

        /**
         * Enqueue a marker to be added upon the {@link #endMessages()} call.
         *
         * @param markerType the type of the marker to create, or null to use
         *            this ProblemMarkerCreator's base marker type
         * @param message localized string describing the nature of the marker
         *            (e.g., a name for a bookmark or task)
         * @param severity number from the set of error, warning and info
         *            severities defined by the platform
         * @param code error code number
         * @param location human-readable (localized) string which can be used
         *            to distinguish between markers on a resource
         * @param charStart integer value indicating where a text marker starts
         * @param charEnd integer value indicating where a text marker ends
         * @param lineNumber integer value indicating the (1-relative) line
         *            number for a text marker
         * @param exception the thrown exception leading to the creation of this
         *            marker
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
                if (charStart >= 0) {
                    attributes.put(IMarker.CHAR_START, Integer.valueOf(charStart));
                }
                if (charEnd >= 0) {
                    attributes.put(IMarker.CHAR_END, Integer.valueOf(charEnd));
                }
            }
            final Integer lineKey = lineNumber > 0 ? Integer.valueOf(lineNumber) : UNKNOWN_LINE;
            if (lineNumber > 0) {
                attributes.put(IMarker.LINE_NUMBER, lineKey);
            }
            if (sourceId != null) {
                attributes.put(IMarker.SOURCE_ID, sourceId);
            }
            // attributes.put(OrcBuilder.COMPILE_MSG_TYPE_MARKER_ATTR_NAME, exception != null ? exception.getClass().getCanonicalName() : null);

            List<TypeAndAttributes> lineEntries = entries.get(lineKey);
            if (lineEntries == null) {
                lineEntries = new ArrayList<TypeAndAttributes>();
                entries.put(lineKey, lineEntries);
            }
            lineEntries.add(attributes);
        }

        /**
         * Add the enqueued markers, replacing all markers of the base type on
         * the resource
         */
        public synchronized void endMessages() {
            final IWorkspaceRunnable action = monitor -> {
                if (entries != null) {
                    final IMarker[] oldMarkers = resource.findMarkers(baseMarkerType, includeSubtypes, IResource.DEPTH_ZERO);
                    for (IMarker oldMarker : oldMarkers) {
                        final Map<String, Object> oldAttributes = oldMarker.getAttributes();
                        final Object oldLineKey = oldAttributes.get(IMarker.LINE_NUMBER);
                        final List<TypeAndAttributes> lineEntries1 = entries.get(oldLineKey != null ? oldLineKey : UNKNOWN_LINE);
                        if (lineEntries1 != null) {
                            for (final Map<String, Object> newAttributes : lineEntries1) {
                                if (isSameMarker(oldAttributes, newAttributes)) {
                                    lineEntries1.remove(newAttributes);
                                    oldMarker = null;
                                    break;
                                }
                            }
                        }
                        if (oldMarker != null) {
                            oldMarker.delete();
                        }
                    }
                    for (final List<TypeAndAttributes> lineEntries2 : entries.values()) {
                        for (final TypeAndAttributes entry : lineEntries2) {
                            final IMarker marker = resource.createMarker(entry.getType());
                            marker.setAttributes(entry);
                        }
                    }
                } else {
                    resource.deleteMarkers(baseMarkerType, includeSubtypes, IResource.DEPTH_ZERO);
                }
            };
            try {
                final IProgressMonitor progressMonitor = new NullProgressMonitor();
                resource.getWorkspace().run(action, resource, IWorkspace.AVOID_UPDATE, progressMonitor);
            } catch (final CoreException e) {
                OrcPlugin.log(e);
            }

        }

        protected boolean isSameMarker(final Map<String, Object> oldAttributes, final Map<String, Object> newAttributes) {
            final Set<String> oldKeys = oldAttributes.keySet();
            final Set<String> newKeys = newAttributes.keySet();
            if (oldKeys.size() != newKeys.size()) {
                return false;
            }
            for (final String key : newKeys) {
                if (dontCompareAttributes.contains(key)) {
                    continue;
                }
                if (!oldAttributes.containsKey(key)) {
                    return false;
                }
                final Object oldValue = oldAttributes.get(key);
                final Object newValue = newAttributes.get(key);
                if (oldValue == newValue) {
                    continue;
                }
                if (oldValue == null) {
                    return false;
                }
                if (newValue == null) {
                    return false;
                }
                if (!oldValue.equals(newValue)) {
                    return false;
                }
            }
            return true;
        }

    }

}
