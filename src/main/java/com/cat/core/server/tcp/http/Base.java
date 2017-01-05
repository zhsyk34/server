package com.cat.core.server.tcp.http;

import io.netty.util.CharsetUtil;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Map;

//TODO:ALL PACKAGE
public class Base {

	private static final String CHARSET = CharsetUtil.UTF_8.name();

	public static Map<String, String> baseHeader() {
		Map<String, String> map = new HashMap<>();
		map.put("Accept", "*/*");
		map.put("Accept-Charset", CHARSET);

		map.put("Cache-Control", "no-cache");
		map.put("Pragma", "no-cache");

		map.put("Connection", "keep-alive");
		map.put("Content-Type", "text/plain; charset=" + CHARSET);
		return map;
	}

	public static URLConnection setHeader(URLConnection connection, Map<String, String> map, boolean base) {
		if (map == null) {
			map = new HashMap<>();
		}

		if (base) {
			map.putAll(baseHeader());
		}

		map.forEach(connection::setRequestProperty);
		return connection;
	}

	public static String parseParams(RequestType type, Map<String, String> params) {
		StringBuilder result = new StringBuilder();
		params.forEach((k, v) -> result.append("&").append(k).append("=").append(v));
		result.replace(0, 1, "");

		switch (type) {
			case GET:
				result.insert(0, "?");
				break;
			case POST:
				break;
		}
		return result.toString();
	}

	public static String data(InputStream in) throws IOException {
		StringBuilder builder = new StringBuilder();
		BufferedReader reader = new BufferedReader(new InputStreamReader(in));

		String line;
		while ((line = reader.readLine()) != null) {
			builder.append(line);
		}
		reader.close();
		return builder.toString();
	}
}
