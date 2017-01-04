package com.cat.core.server.tcp.state;

import com.cat.core.kit.CodecKit;
import com.cat.core.kit.RandomKit;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
final class Verifier {
	private final String question;
	private final String answer;

	static Verifier generator() {
		int group = RandomKit.randomInteger(0, 49);
		int offset = RandomKit.randomInteger(0, 9);
		return new Verifier(CodecKit.loginKey(group, offset), CodecKit.loginVerify(group, offset));
	}
}
