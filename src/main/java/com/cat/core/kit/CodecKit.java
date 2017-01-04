package com.cat.core.kit;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.util.CharsetUtil;

import static com.cat.core.dict.Packet.*;
import static com.cat.core.kit.ByteKit.compare;
import static com.cat.core.kit.ByteKit.smallIntToByteArray;

public class CodecKit {

	private static final int MARK = 0xff;

	//登录验证密钥矩阵
	private static final byte[] MATRIX = {
			0x49, 0x74, 0x27, 0x73, 0x74, 0x68, 0x65, 0x65, 0x71, 0x75,
			0x69, 0x76, 0x61, 0x6C, 0x65, 0x6E, 0x74, 0x6F, 0x66, 0x69,
			0x6E, 0x76, 0x69, 0x74, 0x69, 0x6E, 0x67, 0x73, 0x65, 0x78,
			0x61, 0x64, 0x64, 0x69, 0x63, 0x74, 0x73, 0x74, 0x6F, 0x61,
			0x62, 0x72, 0x6F, 0x74, 0x68, 0x65, 0x6C, 0x6F, 0x72, 0x68,
			0x6F, 0x6C, 0x64, 0x69, 0x6E, 0x67, 0x61, 0x6E, 0x41, 0x6C,
			0x63, 0x6F, 0x68, 0x6F, 0x6C, 0x69, 0x63, 0x73, 0x41, 0x6E,
			0x6F, 0x6E, 0x79, 0x6D, 0x6F, 0x75, 0x73, 0x28, 0x41, 0x41,
			0x29, 0x6D, 0x65, 0x65, 0x74, 0x69, 0x6E, 0x67, 0x61, 0x74,
			0x74, 0x68, 0x65, 0x70, 0x75, 0x62, 0x2E, 0x49, 0x6E, 0x74,
			0x65, 0x72, 0x6E, 0x65, 0x74, 0x61, 0x64, 0x64, 0x69, 0x63,
			0x74, 0x73, 0x74, 0x69, 0x72, 0x65, 0x64, 0x6F, 0x66, 0x74,
			0x68, 0x65, 0x69, 0x72, 0x73, 0x71, 0x75, 0x61, 0x72, 0x65,
			0x2D, 0x65, 0x79, 0x65, 0x64, 0x2C, 0x6B, 0x65, 0x79, 0x62,
			0x6F, 0x61, 0x72, 0x64, 0x74, 0x61, 0x70, 0x70, 0x69, 0x6E,
			0x67, 0x77, 0x61, 0x79, 0x73, 0x6E, 0x65, 0x65, 0x64, 0x6C,
			0x6F, 0x6F, 0x6B, 0x6E, 0x6F, 0x66, 0x75, 0x72, 0x74, 0x68,
			0x65, 0x72, 0x74, 0x68, 0x61, 0x6E, 0x74, 0x68, 0x65, 0x57,
			0x65, 0x62, 0x66, 0x6F, 0x72, 0x63, 0x6F, 0x75, 0x6E, 0x73,
			0x65, 0x6C, 0x6C, 0x69, 0x6E, 0x67, 0x2E, 0x54, 0x68, 0x65,
			0x72, 0x65, 0x69, 0x73, 0x6E, 0x6F, 0x77, 0x61, 0x6E, 0x6F,
			0x6E, 0x6C, 0x69, 0x6E, 0x65, 0x63, 0x6F, 0x75, 0x6E, 0x73,
			0x65, 0x6C, 0x6C, 0x69, 0x6E, 0x67, 0x73, 0x65, 0x72, 0x76,
			0x69, 0x63, 0x65, 0x61, 0x74, 0x77, 0x77, 0x77, 0x2E, 0x72,
			0x65, 0x6C, 0x61, 0x74, 0x65, 0x2E, 0x6F, 0x72, 0x67, 0x2E,
			0x6E, 0x7A, 0x66, 0x6F, 0x72, 0x49, 0x6E, 0x74, 0x65, 0x72,
			0x6E, 0x65, 0x74, 0x6F, 0x62, 0x73, 0x65, 0x73, 0x73, 0x69,
			0x76, 0x65, 0x73, 0x2E, 0x4A, 0x75, 0x73, 0x74, 0x65, 0x2D,
			0x6D, 0x61, 0x69, 0x6C, 0x74, 0x68, 0x65, 0x64, 0x65, 0x74,
			0x61, 0x69, 0x6C, 0x73, 0x6F, 0x66, 0x79, 0x6F, 0x75, 0x72,
			0x49, 0x6E, 0x74, 0x65, 0x72, 0x6E, 0x65, 0x74, 0x2D, 0x69,
			0x6E, 0x64, 0x75, 0x63, 0x65, 0x64, 0x63, 0x72, 0x69, 0x73,
			0x69, 0x73, 0x61, 0x6E, 0x64, 0x68, 0x65, 0x6C, 0x70, 0x63,
			0x6F, 0x6D, 0x65, 0x73, 0x64, 0x69, 0x72, 0x65, 0x63, 0x74,
			0x74, 0x6F, 0x79, 0x6F, 0x75, 0x72, 0x69, 0x6E, 0x62, 0x6F,
			0x78, 0x2E, 0x54, 0x68, 0x65, 0x6E, 0x65, 0x77, 0x62, 0x72,
			0x65, 0x65, 0x64, 0x6F, 0x66, 0x63, 0x79, 0x62, 0x65, 0x72,
			0x74, 0x68, 0x65, 0x72, 0x61, 0x70, 0x69, 0x73, 0x74, 0x73,
			0x73, 0x65, 0x65, 0x6E, 0x6F, 0x74, 0x68, 0x69, 0x6E, 0x67,
			0x73, 0x74, 0x72, 0x61, 0x6E, 0x67, 0x65, 0x61, 0x62, 0x6F,
			0x75, 0x74, 0x6F, 0x66, 0x66, 0x65, 0x72, 0x69, 0x6E, 0x67,
			0x68, 0x65, 0x6C, 0x70, 0x74, 0x68, 0x72, 0x6F, 0x75, 0x67,
			0x68, 0x74, 0x68, 0x65, 0x76, 0x65, 0x72, 0x79, 0x6D, 0x65,
			0x64, 0x69, 0x75, 0x6D, 0x74, 0x68, 0x61, 0x74, 0x69, 0x73,
			0x73, 0x77, 0x61, 0x6C, 0x6C, 0x6F, 0x77, 0x69, 0x6E, 0x67,
			0x74, 0x68, 0x65, 0x69, 0x72, 0x63, 0x6C, 0x69, 0x65, 0x6E,
			0x74, 0x73, 0x27, 0x66, 0x72, 0x65, 0x65, 0x74, 0x69, 0x6D,
			0x65, 0x61, 0x6E, 0x64, 0x73, 0x70, 0x6C, 0x69, 0x74, 0x74,
			0x69, 0x6E, 0x67, 0x74, 0x68, 0x65, 0x69, 0x72, 0x6D, 0x61,
			0x72, 0x72, 0x69, 0x61, 0x67, 0x65, 0x73, 0x2E, 0x49, 0x73
	};

	/*---------------------以下是编码部分---------------------*/

	/**
	 * 长度编码
	 * 编码内容:数据部分长度+长度(2byte)+校验(2byte)
	 *
	 * @param data 数据部分
	 * @return 编码为small int(2byte)
	 */
	private static byte[] encodeLength(byte[] data) {
		return smallIntToByteArray(data.length + LENGTH_BYTES + VERIFY_BYTES);
	}

	/**
	 * 校验码编码原始结果:长度+数据部分逐位求和
	 *
	 * @param data 数据部分
	 * @return 编码后原始内容
	 */
	private static int verify(byte[] data) {
		if (data == null || data.length == 0) {
			throw new RuntimeException("dict is isEmpty.");
		}
		int value = data.length + 4;//TODO
		for (byte b : data) {
			value += b & MARK;
		}
		return value;
	}

	/**
	 * 校验码编码:返回结算结果的低位(2byte)
	 */
	private static byte[] encodeVerify(byte[] data) {
		int value = verify(data);
		return smallIntToByteArray(value);
	}

	/**
	 * 编码:编码时先加密
	 *
	 * @param cmd 数据部分(原始的指令json)
	 * @return 编码结果
	 */
	public static byte[] encode(String cmd) {
		if (ValidateKit.isEmpty(cmd)) {
			throw new RuntimeException("command is null.");
		}
		byte[] data = DESKit.encrypt(cmd.getBytes(CharsetUtil.UTF_8));

		ByteBuf buffer = Unpooled.buffer(data.length + REDUNDANT_BYTES);

		//header:2
		buffer.writeBytes(new byte[]{HEADERS.get(0), HEADERS.get(1)});
		//length:2
		buffer.writeBytes(encodeLength(data));
		//dict
		buffer.writeBytes(data);
		//verifyKey:2
		buffer.writeBytes(encodeVerify(data));
		//footer:2
		buffer.writeBytes(new byte[]{FOOTERS.get(0), FOOTERS.get(1)});

		return buffer.array();
	}

	/**
	 * 登录验证时的加密密钥信息
	 *
	 * @param group  组号[0,49]
	 * @param offset 偏移量[0,9]
	 * @return 在group与offset前加入2位整数进行混淆
	 */
	public static String loginKey(int group, int offset) {
		if (group < 0 || group > 50 || offset < 0 || offset > 9) {
			throw new RuntimeException("group[0-49], offset[0-9]");
		}
		int mixOne = RandomKit.randomInteger(0, 99);
		int mixTwo = RandomKit.randomInteger(0, 99);

		return ConvertKit.fillZero(mixOne, 2) + ConvertKit.fillZero(group, 2) + ConvertKit.fillZero(mixTwo, 2) + ConvertKit.fillZero(offset, 2);
	}




	/*---------------------以下是解码部分---------------------*/

	/**
	 * @param data      数据部分
	 * @param verifyArr 检验码(2byte)
	 * @return 校验码是否合法
	 */
	public static boolean validateVerify(byte[] data, byte[] verifyArr) {
		return compare(encodeVerify(data), verifyArr);
	}

	/**
	 * @param group  组号[0,49]
	 * @param offset 偏移量[0,9]
	 * @return 登录验证码
	 */
	public static String loginVerify(int group, int offset) {
		byte[] bytes = new byte[10];
		for (int i = 0; i < bytes.length; i++) {
			bytes[i] = MATRIX[(group * 10 + offset + i) % MATRIX.length];
		}
		return new String(bytes, CharsetUtil.UTF_8);
	}

	public static byte[] decode(byte[] bytes) {
		return DESKit.decrypt(bytes);
	}

	public static ByteBuf decode(ByteBuf buf) {
		byte[] bytes = ByteKit.getBytes(buf);
		return Unpooled.wrappedBuffer(decode(bytes));
	}

}
