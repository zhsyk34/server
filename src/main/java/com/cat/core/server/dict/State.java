package com.cat.core.server.dict;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

/**
 * 会话状态
 */
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
public enum State {
	CREATE(0, "创建连接"),
	REQUEST(1, "接收到登录请求"),
	READY(2, "发送验证码后进入就绪状态等待身份验证"),
	VERIFY(3, "进行验证"),
	PASS(4, "通过验证"),
	WAIT(5, "等待进一步处理(网关登录等待系统分配端口资源)"),
	SUCCESS(6, "登录成功"),
	CLOSED(7, "连接关闭");

	public static final Map<Integer, State> MAP = new HashMap<>();

	static {
		for (State state : values()) {
			MAP.put(state.getStep(), state);
		}
	}

	private final int step;
	private final String description;

	public static State from(int step) {
		return MAP.get(step);
	}

	public State previous() {
		return from(this.getStep() - 1);
	}

	public State next() {
		return from(this.getStep() + 1);
	}

}
