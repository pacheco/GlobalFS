package ch.usi.paxosfs.util;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.UUID;

public class UUIDUtils {
	public static byte[] toBytes(UUID id) {
		byte[] b = new byte[16];
		return ByteBuffer.wrap(b)
			.order(ByteOrder.BIG_ENDIAN)
			.putLong(id.getMostSignificantBits())
			.putLong(id.getLeastSignificantBits())
			.array();
	}
	
	public static UUID fromBytes(byte[] b) {
		if (b.length < 16) {
			throw new IllegalArgumentException("uuid needs at least 16 bytes");
		}
		ByteBuffer bb = ByteBuffer.wrap(b);
		bb.flip();
		return new UUID(bb.getLong(), bb.getLong());
	}
}
