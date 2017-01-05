package com.cat.core;

import java.time.Duration;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.concurrent.TimeUnit;

public class TestTime {
	public static void main(String[] args) {
		one();
	}

	private static void base() {
		LocalTime zero = LocalTime.MIN;

		LocalTime time = LocalTime.of(23, 59, 0);
		System.out.println(zero.isBefore(time));

		Duration duration = Duration.between(time, zero);
		System.out.println(duration.toHours());
		Duration plus = duration.plus(Duration.ofDays(1));

		System.out.println(plus.toMinutes());
	}

	private static void one() {
		LocalTime zero = LocalTime.MIN;
		LocalTime time = LocalTime.of(1, 1, 1);
		LocalTime now = LocalTime.now();

		ZoneId zoneId = ZoneId.systemDefault();
		System.out.println(zoneId.getId());

		System.out.println(Duration.between(zero, time).toMinutes());
		System.out.println(Duration.between(zero, now).toMinutes());

		Duration duration = Duration.between(now, time);
		long x = Duration.between(now, time).toMinutes();
		System.out.println(x);

		long dayM = TimeUnit.DAYS.toMinutes(1);
		System.out.println(dayM);
		System.out.println(x + dayM);
		long between = duration.isNegative() ? duration.plus(Duration.ofDays(1)).toMinutes() : duration.toMillis();
		System.out.println(between);
	}
}
