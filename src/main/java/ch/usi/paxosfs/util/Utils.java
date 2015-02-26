package ch.usi.paxosfs.util;

import java.util.Random;
import java.util.Set;

public class Utils {
	public static Byte randomElem(Random rand, Set<Byte> partitions) {
		int item = rand.nextInt(partitions.size());
		int i = 0;
		for(Byte obj : partitions)
		{
		    if (i == item)
		        return obj;
		    i = i + 1;
		}
		// should never get here but the compiler needs the return.
		return partitions.iterator().next();
	}

    public static byte[] randomBytes(Random rand, int size) {
        byte[] bytes = new byte[size];
        rand.nextBytes(bytes);
        return bytes;
    }
}
