package com.cat.core.db;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public final class UDPRecord {
	private String sn;
	private String ip;
	private int port;
	private long happen;
}
