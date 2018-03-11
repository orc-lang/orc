//
// PorcENodeExecutionProfilerInstrument.java -- Truffle instrument PorcENodeExecutionProfilerInstrument
// Project PorcE
//
// Copyright (c) 2018 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.run.porce.instruments;

import java.util.HashMap;

import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.FrameSlotKind;
import com.oracle.truffle.api.frame.FrameSlotTypeException;
import com.oracle.truffle.api.frame.FrameUtil;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.EventContext;
import com.oracle.truffle.api.instrumentation.ExecutionEventNode;
import com.oracle.truffle.api.instrumentation.ExecutionEventNodeFactory;
import com.oracle.truffle.api.instrumentation.Instrumenter;
import com.oracle.truffle.api.instrumentation.SourceSectionFilter;
import com.oracle.truffle.api.instrumentation.TruffleInstrument;
import com.oracle.truffle.api.nodes.RootNode;
import com.oracle.truffle.api.profiles.ConditionProfile;

import orc.run.porce.instruments.ProfilerUtils.ProfilerState;
import orc.run.porce.instruments.ProfilerUtils.Counter;

@TruffleInstrument.Registration(id = PorcENodeExecutionProfilerInstrument.ID, services = { PorcENodeExecutionProfiler.class })
public final class PorcENodeExecutionProfilerInstrument extends TruffleInstrument {

	public static final String ID = "porce-execution-profiler";

	private PorcENodeExecutionProfiler profiler;

	@Override
	protected void onCreate(Env env) {
		this.profiler = new PorcENodeExecutionProfiler(env);
		env.registerService(this.profiler);
		SourceSectionFilter.Builder builder = SourceSectionFilter.newBuilder();
		SourceSectionFilter filter = builder.tagIs(ProfiledPorcENodeTag.class).build();
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
				.findOrAddFrameSlot(PorcENodeExecutionProfiler.KEY(), "<profilerStateE>", FrameSlotKind.Object));
	}

	protected class EventFactory implements ExecutionEventNodeFactory {
		@SuppressWarnings("unused")
		private final Env env;

		EventFactory(final Env env) {
			this.env = env;
		}

		@Override
		public ExecutionEventNode create(final EventContext ec) {
			final com.oracle.truffle.api.nodes.Node n = ec.getInstrumentedNode();
			final RootNode rootNode = n.getRootNode();
			final PorcENodeExecutionProfiler profiler = PorcENodeExecutionProfilerInstrument.this.profiler;
			return rootNode.atomic(() -> {
				final Counter counter = profiler.getCounter(n);
				final FrameSlot profilerStateSlot = getProfilerStateSlot(rootNode);
				final ConditionProfile setStateProfile = ConditionProfile.createBinaryProfile();
				return new ExecutionEventNode() {
					@Override
					protected void onEnter(VirtualFrame frame) {
						ProfilerState state = (ProfilerState) FrameUtil.getObjectSafe(frame, profilerStateSlot);
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
								if (parentCounter != counter) {
									counter.addTime(time);
									if (parentCounter != null)
										parentCounter.addChildTime(time);
								}
							} else {
								// The entry failed to push
							}
						} catch (FrameSlotTypeException e) {
							throw new Error(e);
						}
					}
				};
			});
		}
	}

}
