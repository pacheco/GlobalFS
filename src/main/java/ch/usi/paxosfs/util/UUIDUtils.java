package ch.usi.paxosfs.util;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Random;
import java.util.UUID;

public class UUIDUtils {
    final protected static char[] hexArray = "0123456789ABCDEF".toCharArray();

    /**
     * Generate a random byte array of size n
     * @param n
     * @return
     */
    public static byte[] randomBytes(Random r, int n) {
        byte[] bytes = new byte[n];
        r.nextBytes(bytes);
        return bytes;
    }

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
		return bb.getLong();
	}


    /**
     * Convert byte array to Hex string. Taken from StackOverflow:
     * http://stackoverflow.com/questions/9655181/convert-from-byte-array-to-hex-string-in-java
     * @param bytes
     * @return
     */
    public static String bytesToHex(byte[] bytes) {
        if (bytes.length % 4 != 0) {
            throw new IllegalArgumentException("byte array should be multiple of 4");
        }
        char[] hexChars = new char[bytes.length * 2];
        for ( int j = 0; j < bytes.length; j++ ) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }
}
