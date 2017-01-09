package com.cat.core.server.web;

import lombok.NonNull;

public interface PushHandler {

	boolean push(@NonNull String msg);

}
