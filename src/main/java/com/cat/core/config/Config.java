package com.cat.core.config;

/**
 * 参数配置
 * 如无特殊说明,时间的单位均为秒
 */
public final class Config {

	public static final String SERVER_START_TIME = "2016-12-01";
	/**
	 * -----------------------------TCP配置-----------------------------
	 */
	//TCP服务器默认端口
	public static final int TCP_SERVER_PORT = 15999;
	//TCP服务器最大并发连接数
	public static final int TCP_SERVER_BACKLOG = 1 << 16;
	//TCP预计并发连接数
	public static final int TCP_APP_COUNT_PREDICT = 1 << 12;
	public static final int TCP_GATEWAY_COUNT_PREDICT = 1 << 14;
	//TCP连接超时时间
	public static final int TCP_CONNECT_TIMEOUT = 5;
	//TCP登录时间
	public static final int TCP_LOGIN_TIMEOUT = 5;
	//app单次与服务器建立连接的最大时长
	public static final int TCP_APP_TIMEOUT = 17;
	//网关单次与服务器建立连接的最大时长
	public static final int TCP_GATEWAY_TIMEOUT = 30 * 60;
	//APP请求的最长处理时间(从开始处理时计时)
	public static final int TCP_MESSAGE_HANDLE_TIMEOUT = 18;
	//TCP管理(扫描)线程执行频率
	public static final int TCP_TIMEOUT_SCAN_FREQUENCY = 10;
	//TCP允许的最大的无效缓冲数据
	public static final int TCP_BUFFER_SIZE = 1 << 10;
	//TCP为网关分配的最小UDP端口
	public static final int TCP_ALLOT_MIN_UDP_PORT = 50000;
	/**
	 * -----------------------------UDP配置-----------------------------
	 */
	//UDP服务器默认端口
	public static final int UDP_SERVER_PORT = 15998;
	//UDP推送服务器默认端口
	public static final int UDP_PUSHER_PORT = 15997;
	//TCP服务器默认端口 TODO
	public static final int UDP_WEB_PORT = 9999;
	//扫描网关在线状态扫描频率
	public static final int UDP_ONLINE_SCAN_FREQUENCY = 10;
	//端口回收扫描频率
	public static final int UDP_PORT_COLLECTION_SCAN_FREQUENCY = 24 * 60 * 60;
	//端口信息保存频率
	public static final int UDP_PORT_SAVE_FREQUENCY = 6 * 60 * 60;
	/**
	 * -----------------------------日志配置-----------------------------
	 */
	public static final int LOGGER_CAPACITY = 5000;
	/**
	 * -----------------------------系统时间配置-----------------------------
	 */
	//服务器启动完毕后执行扫描任务
	public static final int SCHEDULE_TASK_DELAY_TIME = 1;
	//	//服务器启动状态监视时间间隔
	public static final int SERVER_START_MONITOR_TIME = 1500;//ms
	//通过UDP唤醒网关时检测状态时间间隔
	public static final int GATEWAY_AWAKE_CHECK_TIME = 100;//ms
	//
//	/**
//	 * -----------------------------DB配置-----------------------------
//	 */
	public static final int BATCH_FETCH_SIZE = 10;
	private static final String LOCAL_HOST = "127.0.0.1";
	/**
	 * -----------------------------TCP配置-----------------------------
	 */
	//TCP服务器地址
	public static final String TCP_SERVER_HOST = LOCAL_HOST;
	//本地服务器地址:TODO
	public static final String UDP_WEB_IP = LOCAL_HOST;
	//网关发送UDP心跳包频率
	private static final int UDP_HEART_FREQUENCY = 10;
	//UDP信息过期时间
	public static final int UDP_HEART_DUE = UDP_HEART_FREQUENCY * 6;
//	/**
//	 * -----------------------------web-udp信息-----------------------------
//	 */

//
//
//	//网关UDP心跳最长离线时间
//	public static final int UDP_MAX_IDLE = UDP_CLIENT_FREQUENCY * 10;

}
