package orc.util;

import java.io.PrintWriter;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicLong;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.FrameSlotKind;
import com.oracle.truffle.api.frame.FrameSlotTypeException;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.EventContext;
import com.oracle.truffle.api.instrumentation.ExecutionEventNode;
import com.oracle.truffle.api.instrumentation.ExecutionEventNodeFactory;
import com.oracle.truffle.api.instrumentation.Instrumenter;
import com.oracle.truffle.api.instrumentation.SourceSectionFilter;
import com.oracle.truffle.api.instrumentation.TruffleInstrument;
import com.oracle.truffle.api.nodes.RootNode;
import com.oracle.truffle.api.profiles.ConditionProfile;

import orc.ast.porc.PorcAST;
import orc.run.porce.HasPorcNode;

@TruffleInstrument.Registration(id = PorcNodeExecutionProfiler.ID)
public final class PorcNodeExecutionProfiler extends TruffleInstrument {

	public static final String ID = "porc-execution-profiler";
	
	// FIXME: Remove measurements from constants, reads, writes, sequences (that are not representing a non sequence Porc node).
	// FIXME: Implement Orc controlled starting and output. This will allow the benchmark harness to output/reset the data at the end of each iteration.
	
	private ThreadLocal<ProfilerState> profilerState = new ThreadLocal<ProfilerState>() {
		protected  ProfilerState initialValue() {
			return new ProfilerState();
		}
	};
	
	@TruffleBoundary(allowInlining = true)
	private ProfilerState getProfilerState() {
		return profilerState.get();
	}
	
	@Override
	protected void onCreate(final Env env) {
		SourceSectionFilter.Builder builder = SourceSectionFilter.newBuilder();
		SourceSectionFilter filter = builder.build();
		Instrumenter instrumenter = env.getInstrumenter();
		instrumenter.attachFactory(filter, new EventFactory(env));
	}

	@Override
	protected void onDispose(Env env) {
		PrintWriter out = new PrintWriter(env.out());
		for (HashMap.Entry<?, Counter> entry : nodeCounts.entrySet()) {
			Object k = entry.getKey();
			Counter count = entry.getValue();
			String s = k.toString();
			if (s.length() < 100) {
				out.println(s + ": " + count);
			}
		}
		out.flush();
	}
	
	protected static class ProfilerState {
		public final static Object KEY = ProfilerState.class; 

		private ArrayDeque<Counter> counterStack = new ArrayDeque<Counter>();
		
		@TruffleBoundary(allowInlining = true)
		public Counter popCurrentCounter() {
			counterStack.pop();
			return counterStack.peek(); 
		}
		
		@TruffleBoundary(allowInlining = true)
		public boolean pushCurrentCounter(Counter c) {
			if (c != counterStack.peek()) {
				counterStack.push(c);
				return true;
			} else {
				return false;
			}
		}

		private ArrayDeque<Long> startTimeStack = new ArrayDeque<Long>();
		
		@TruffleBoundary(allowInlining = true)
		public long popStartTime() {
			return startTimeStack.pop();
		}
		
		@TruffleBoundary(allowInlining = true)
		public void pushStartTime(long t) {
			startTimeStack.push(t);
		}

	}

	protected static class Counter {
		private final AtomicLong hits = new AtomicLong(0L);
		private final AtomicLong time = new AtomicLong(0L);
		private final AtomicLong childTime = new AtomicLong(0L);
		
		public void addHit() {
			hits.getAndIncrement();
		}	
		
		public void addTime(long time) {
			this.time.getAndAdd(time);
		}

		public void addChildTime(long time) {
			childTime.getAndAdd(time);
		}

		public long getSelfTime() {
			return time.get() - childTime.get();
		}

		public long getTime() {
			return time.get();
		}

		public long getHits() {
			return hits.get();
		}
		
		public static double toSeconds(long ns) {
			return ((double)ns) / 1000 / 1000 / 1000; 
		}

		@Override
		public String toString() {
			return "hits = " + getHits() + ", self time = " + toSeconds(getSelfTime()) + "s, total time = " + toSeconds(getTime()) + "s";
		}
	}

	private HashMap<PorcAST, Counter> nodeCounts = new HashMap<>();

	private HashMap<RootNode, FrameSlot> profilerStateSlots = new HashMap<>();

	protected synchronized Counter getCounter(PorcAST n) {
		return nodeCounts.computeIfAbsent(n, (key) -> new Counter());
	}

	protected synchronized FrameSlot getProfilerStateSlot(RootNode n) {
		return profilerStateSlots.computeIfAbsent(n, (k) -> 
			k.getFrameDescriptor().findOrAddFrameSlot(
					ProfilerState.KEY, "profilerState", FrameSlotKind.Object));
	}

	private class EventFactory implements ExecutionEventNodeFactory {
		private final Env env;

		EventFactory(final Env env) {
			this.env = env;
		}

		public ExecutionEventNode create(final EventContext ec) {
			com.oracle.truffle.api.nodes.Node n = ec.getInstrumentedNode();
			RootNode rootNode = n.getRootNode();
			return rootNode.atomic(() -> {
				PorcAST ast = null;
				if (n instanceof HasPorcNode)
					ast = ((HasPorcNode) n).porcNode().getOrElse(() -> null);
				if (ast != null) {
					final Counter counter = getCounter(ast);
					final FrameSlot profilerStateSlot = getProfilerStateSlot(rootNode);
					final ConditionProfile setStateProfile = ConditionProfile.createBinaryProfile();
					return new ExecutionEventNode() {
						@Override
						protected void onEnter(VirtualFrame frame) {
							try {
								ProfilerState state = (ProfilerState) frame.getObject(profilerStateSlot);
								if (setStateProfile.profile(state == null)) {
									state = getProfilerState();
									frame.setObject(profilerStateSlot, state);
								}

								if (state.pushCurrentCounter(counter)) {
									counter.addHit();
									state.pushStartTime(System.nanoTime());
								} else {
									state.pushStartTime(-1);
								}
							} catch (FrameSlotTypeException e) {
								throw new Error(e);
							}
						}
	
						@Override
						protected void onReturnExceptional(VirtualFrame frame, Throwable exception) {
							onReturnValue(frame, null);
						}
	
						@Override
						protected void onReturnValue(VirtualFrame frame, Object result) {
							try {
								ProfilerState state = (ProfilerState) frame.getObject(profilerStateSlot);
								long startTime = state.popStartTime();
								
								if (startTime >= 0) {
									long time = System.nanoTime() - startTime;
									Counter parentCounter = state.popCurrentCounter();
									counter.addTime(time);
									if (parentCounter != null)
										parentCounter.addChildTime(time);
								} else {
									// The entry failed to push
								}
							} catch (FrameSlotTypeException e) {
								throw new Error(e);
							}
						}
					};
				} else {
					return new ExecutionEventNode() {};
				}
			});
		}
	}

}