package ch.usi.paxosfs.client.microbench;

import org.apache.thrift.TException;

import java.io.IOException;
import java.util.Random;

/**
 * Created by pacheco on 24/11/14.
 */
public interface MicroBench {
    void setup(String replicaAddr, byte partition, String logPrefix, Random rand) throws TException;
    Thread startWorker(int workerId, long durationMillis, int globals) throws IOException;
}
