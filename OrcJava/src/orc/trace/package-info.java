/**
 * Generate and query Orc trace files. An Orc trace file is a gziped sequence
 * of serialized {@link orc.trace.events.Event} {@link orc.trace.handles.Handle}s.
 * These trace files are written with {@link OutputStreamTracer} and read
 * with {@link orc.trace.InputStreamEventCursor}. It's possible to define
 * other kinds of tracers and trace formats by providing your own implementation
 * of {@link TokenTracer}.
 */
package orc.trace;