package com.cat.core.server.tcp.message;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

/**
 * app request info:save app channel id to response
 */
@RequiredArgsConstructor(staticName = "of")
@Getter
@ToString
public final class AppRequest {
	private final String id;
	private final String message;
}