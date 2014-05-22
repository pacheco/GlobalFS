package ch.usi.paxosfs.util;

import java.util.Random;
import java.util.Set;

public class Utils {
	public static Byte randomElem(Random rand, Set<Byte> partitions) {
		int item = rand.nextInt(partitions.size()); // In real life, the Random object should be rather more shared than this
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
}
