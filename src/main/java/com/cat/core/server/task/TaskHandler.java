package com.cat.core.server.task;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class TaskHandler {

	private static final List<LoopTask> LOOP_TASKS = new ArrayList<>();

	private static final List<TimerTask> TIMER_TASKS = new ArrayList<>();

	public static void register(LoopTask... tasks) {
		Collections.addAll(LOOP_TASKS, tasks);
	}

	public static void register(TimerTask... tasks) {
		Collections.addAll(TIMER_TASKS, tasks);
	}

	public static void execute() {
		if (!LOOP_TASKS.isEmpty()) {
			ExecutorService service = Executors.newFixedThreadPool(LOOP_TASKS.size());
			LOOP_TASKS.forEach(task -> service.submit(() -> {
				while (true) {
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
