package orc.run.porce.instruments;

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
import orc.run.porce.instruments.PorcNodeExecutionProfiler.ProfilerState;
import orc.run.porce.instruments.PorcNodeExecutionProfiler.Counter;

@TruffleInstrument.Registration(id = PorcNodeExecutionProfilerInstrument.ID, services = { PorcNodeExecutionProfiler.class })
public final class PorcNodeExecutionProfilerInstrument extends TruffleInstrument {

	public static final String ID = "porc-execution-profiler";

	private PorcNodeExecutionProfiler profiler;

	@Override
	protected void onCreate(Env env) {
		this.profiler = new PorcNodeExecutionProfiler(env);
		env.registerService(this.profiler);
		SourceSectionFilter.Builder builder = SourceSectionFilter.newBuilder();
		SourceSectionFilter filter = builder.tagIs(ProfiledPorcNodeTag.class).build();
		Instrumenter instrumenter = env.getInstrumenter();
		instrumenter.attachFactory(filter, new EventFactory(env));
	}

	@Override
	protected void onDispose(Env env) {
		if (profiler != null) {
			profiler.dispose();
		}
	}

	private HashMap<RootNode, FrameSlot> profilerStateSlots = new HashMap<>();

	protected synchronized FrameSlot getProfilerStateSlot(RootNode n) {
		return profilerStateSlots.computeIfAbsent(n, (k) -> k.getFrameDescriptor()
				.findOrAddFrameSlot(PorcNodeExecutionProfiler.KEY(), "profilerState", FrameSlotKind.Object));
	}

	protected class EventFactory implements ExecutionEventNodeFactory {
		private final Env env;

		EventFactory(final Env env) {
			this.env = env;
		}

		public ExecutionEventNode create(final EventContext ec) {
			com.oracle.truffle.api.nodes.Node n = ec.getInstrumentedNode();
			RootNode rootNode = n.getRootNode();
			final PorcNodeExecutionProfiler profiler = PorcNodeExecutionProfilerInstrument.this.profiler;
			return rootNode.atomic(() -> {
				PorcAST ast = null;
				if (n instanceof HasPorcNode)
					ast = ((HasPorcNode) n).porcNode().getOrElse(() -> null);
				if (ast != null) {
					final Counter counter = profiler.getCounter(ast);
					final FrameSlot profilerStateSlot = getProfilerStateSlot(rootNode);
					final ConditionProfile setStateProfile = ConditionProfile.createBinaryProfile();
					return new ExecutionEventNode() {
						@Override
						protected void onEnter(VirtualFrame frame) {
							try {
								ProfilerState state = (ProfilerState) frame.getObject(profilerStateSlot);
								if (setStateProfile.profile(state == null)) {
									state = profiler.getProfilerState();
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
					return new ExecutionEventNode() {
					};
				}
			});
		}
	}

}