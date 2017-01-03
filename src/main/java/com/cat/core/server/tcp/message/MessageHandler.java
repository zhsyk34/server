package com.cat.core.server.tcp.message;

import lombok.NonNull;

public interface MessageHandler {

	boolean receive(@NonNull String sn, @NonNull AppRequest request);

	void process();

	boolean response(@NonNull String sn, @NonNull String msg);

	boolean push(@NonNull String sn, @NonNull String msg);
}
