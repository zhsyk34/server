package com.cat.core.server.task;

import com.cat.core.kit.ThreadKit;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

@NoArgsConstructor(staticName = "instance")
public final class TaskHandler {

	private static final List<LoopTask> LOOP_TASKS = new ArrayList<>();

	private static final List<TimerTask> TIMER_TASKS = new ArrayList<>();

	public void register(LoopTask... tasks) {
		Collections.addAll(LOOP_TASKS, tasks);
	}

	public void register(TimerTask... tasks) {
		Collections.addAll(TIMER_TASKS, tasks);
	}

	public void register(FixedTimerTask... tasks) {
		for (FixedTimerTask task : tasks) {
			TIMER_TASKS.add(task.toTimerTask());
		}
	}

	public void execute() {
		if (!LOOP_TASKS.isEmpty()) {
			ExecutorService service = Executors.newFixedThreadPool(LOOP_TASKS.size());
			LOOP_TASKS.forEach(task -> service.submit(() -> {
				while (true) {
					//TODO :TEST
					ThreadKit.await(20 * 1000);
					task.execute();
				}
			}));
			service.shutdown();
		}

		if (!TIMER_TASKS.isEmpty()) {
			ScheduledExecutorService service = Executors.newScheduledThreadPool(TIMER_TASKS.size());
			TIMER_TASKS.forEach(task -> service.scheduleAtFixedRate(task.runnable, task.delay, task.period, task.unit));
		}
	}
}
