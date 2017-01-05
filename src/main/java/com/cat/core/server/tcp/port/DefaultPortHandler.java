package com.cat.core.server.tcp.port;

import com.cat.core.config.Config;
import com.cat.core.db.PortDao;
import com.cat.core.db.UDPRecord;
import com.cat.core.kit.AllocateKit;
import com.cat.core.kit.ThreadKit;
import com.cat.core.kit.ValidateKit;
import com.cat.core.log.Factory;
import com.cat.core.log.Log;
import com.cat.core.server.task.TimerTask;
import io.netty.handler.logging.LogLevel;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.NonNull;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@NoArgsConstructor(staticName = "instance")
public final class DefaultPortHandler implements PortHandler {

	/**
	 * <ip, Map<sn, Record>>
	 */
	private static final Map<String, Map<String, Record>> PORT_MAP = new ConcurrentHashMap<>();

	static {
		for (int i = 0; i < 4; i++) {
			ThreadKit.await(300);
			Log.logger(Factory.UDP_EVENT, "----------load port data................");
		}
		Log.logger(Factory.UDP_EVENT, "----------load port data success.");
//		init();
	}

	/**
	 * load from database
	 */
	private static void init() {
		Log.logger(Factory.TCP_EVENT, "正从数据库加载网关UDP端口信息...");

		List<UDPRecord> list;
		int cursor = 0;
		while (true) {
			list = PortDao.find(cursor, Config.BATCH_FETCH_SIZE);
			if (ValidateKit.isEmpty(list)) {
				break;
			}
			list.forEach(DefaultPortHandler::register);
			cursor += Config.BATCH_FETCH_SIZE;
		}

		StringBuilder builder = new StringBuilder();
		builder.append("加载完毕:\n");
		builder.append("----------------------------------\n");
		PORT_MAP.forEach((ip, map) -> builder.append("ip[").append(ip).append("]下有[").append(map.size()).append("]个端口正被使用\n"));
		builder.append("----------------------------------\n");
		Log.logger(Factory.TCP_EVENT, builder.toString());
	}

	/**
	 * if only used call once at init method synchronized is not necessary
	 */
	private static void register(@NonNull UDPRecord record) {
		final Map<String, Record> map;
		final String ip = record.getIp();
		synchronized (PORT_MAP) {
			if (PORT_MAP.containsKey(ip)) {
				map = PORT_MAP.get(ip);
			} else {
				map = new ConcurrentHashMap<>();
				PORT_MAP.put(ip, map);
			}
		}
		map.put(record.getSn(), Record.of(record.getPort(), record.getHappen()));
	}

	@Override
	public int port(String ip, String sn) {
		final Map<String, Record> map = PORT_MAP.get(ip);
		if (map == null) {
			return -1;
		}
		Record record = map.get(sn);
		return record == null ? -1 : record.port;
	}

	@Override
	public int allocate(String sn, String ip, int apply) {
		synchronized (PORT_MAP) {
			Log.logger(Factory.TCP_RECEIVE, LogLevel.TRACE, "网关[" + sn + "]请求登录登录信息:[" + ip + " : " + apply + "]");

			final Map<String, Record> map;

			//1.idle
			if (!PORT_MAP.containsKey(ip)) {
				map = new ConcurrentHashMap<>();
				map.put(sn, Record.of(apply));

				PORT_MAP.put(ip, map);
				Log.logger(Factory.TCP_EVENT, "ip:[" + ip + "]下无相应网关,直接启用端口");
				return apply;
			}

			//2.used
			map = PORT_MAP.get(ip);
			Set<Integer> set = new HashSet<>();
			map.forEach((k, v) -> set.add(v.port));
			Log.logger(Factory.TCP_EVENT, LogLevel.TRACE, "ip:[" + ip + "]下已被使用的端口:\n" + Arrays.toString(set.toArray(new Integer[set.size()])));

			//3.load
			Record record = map.get(sn);
			if (record == null) {
				//temp,must update
				record = Record.of(-1);
				map.put(sn, record);
			} else {
				record.happen = System.currentTimeMillis();
			}

			//4.该IP下的端口未被使用
			if (!set.contains(apply)) {
				record.port = apply;
				Log.logger(Factory.TCP_EVENT, "端口[" + apply + "]未被使用,直接启用");
				return apply;
			}

			//5.申请的端口 apply 恰为原网关使用
			if (record.port == apply) {
				Log.logger(Factory.TCP_EVENT, "申请的端口[" + apply + "]恰为原网关使用,继续使用");
				return apply;
			}

			//6.分配新端口
			int allocated = AllocateKit.allocate(Config.TCP_ALLOT_MIN_UDP_PORT, set);
			record.port = allocated;
			Log.logger(Factory.TCP_EVENT, "网关[" + sn + "]申请的端口[" + apply + "]已被使用,为其分配新端口[" + allocated + "]");
			return allocated;
		}
	}

	@Override
	public TimerTask recycle() {
		Runnable task = () -> {
			//重新分组:转为(sn,(ip,record))
			final Map<String, Map<String, Record>> snMap = new HashMap<>();

			PORT_MAP.forEach((ip, map) -> map.forEach((sn, record) -> {
				final Map<String, Record> ipMap;
				if (snMap.containsKey(sn)) {
					ipMap = snMap.get(sn);
				} else {
					ipMap = new HashMap<>();
					snMap.put(sn, ipMap);
				}

				ipMap.put(ip, record);
			}));

			snMap.forEach((sn, map) -> {
				Log.logger(Factory.TCP_EVENT, "网关[" + sn + "]占用的端口号数为" + map.size());
				if (map.size() < 2) {
					Log.logger(Factory.TCP_EVENT, "占用端口号数 < 2,无需清理");
					return;
				}
				//对需要移除的数据按照端口分配时间进行排序
				LinkedHashMap<String, Record> linkMap = map.entrySet().stream().sorted((o1, o2) -> o2.getValue().happen - o1.getValue().happen > 0 ? 1 : -1).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));

				Record last = linkMap.entrySet().iterator().next().getValue();
				Log.logger(Factory.TCP_EVENT, "网关[" + sn + "]最后使用的端口信息:[" + last + "]");

				//开始移除(移除时与首元素再次进行比较,防止误删,避免加锁)
				linkMap.forEach((ip, record) -> {
					Map<String, Record> presentSnMap = PORT_MAP.get(ip);
					if (presentSnMap != null) {
						Record udpPortRecord = presentSnMap.get(sn);
						if (udpPortRecord != null && udpPortRecord.happen < last.happen) {
							presentSnMap.remove(sn, udpPortRecord);
						}
					}
				});
			});
		};

		return TimerTask.of(task, 6, 6, TimeUnit.HOURS);
	}

	@AllArgsConstructor(access = AccessLevel.PRIVATE, staticName = "of")
	private static final class Record {
		int port;
		long happen;

		private static Record of(int port) {
			return Record.of(port, System.currentTimeMillis());
		}
	}
}
