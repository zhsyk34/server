package com.cat.core.db;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;

public final class SqlSession {

	private static final JdbcTemplate jdbcTemplate;

	static {
		ApplicationContext ctx = new ClassPathXmlApplicationContext("spring.xml");
		jdbcTemplate = ctx.getBean(JdbcTemplate.class);
	}

	public static JdbcTemplate session() {
		return jdbcTemplate;
	}
}
