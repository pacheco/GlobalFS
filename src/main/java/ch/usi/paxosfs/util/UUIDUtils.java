package ch.usi.paxosfs.util;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.UUID;

public class UUIDUtils {
	public static byte[] uuidToBytes(UUID id) {
		byte[] b = new byte[16];
		return ByteBuffer.wrap(b)
			.order(ByteOrder.BIG_ENDIAN)
			.putLong(id.getMostSignificantBits())
			.putLong(id.getLeastSignificantBits())
			.array();
	}
	
	public static UUID uuidFromBytes(byte[] b) {
		if (b.length < 16) {
			throw new IllegalArgumentException("uuid needs at least 16 bytes");
		}
		ByteBuffer bb = ByteBuffer.wrap(b);
		bb.flip();
		bb.order(ByteOrder.BIG_ENDIAN);
		return new UUID(bb.getLong(), bb.getLong());
	}
	
	public static byte[] longToBytes(long l) {
		byte[] b = new byte[8];
		return ByteBuffer.wrap(b)
				.order(ByteOrder.BIG_ENDIAN)
				.putLong(l).array();
	}

	public static long bytesToLong(byte[] b) {
		if (b.length < 8) {
			throw new IllegalArgumentException("long needs at least 8 bytes");
		}
		ByteBuffer bb = ByteBuffer.wrap(b)
				.order(ByteOrder.BIG_ENDIAN);
		bb.flip();
		return bb.getLong();
	}
}
