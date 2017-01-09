package com.cat.core.server.web;

import lombok.NonNull;

public interface PushController {
	boolean push(@NonNull String msg);
}
