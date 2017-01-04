package com.cat.core.server.task;

import lombok.AllArgsConstructor;

import java.util.concurrent.TimeUnit;

@AllArgsConstructor(staticName = "of")
public final class TimerTask {
	final Runnable runnable;
	final long delay;
	final long period;
	final TimeUnit unit;
}