package ch.usi.paxosfs.client.microbench;

import ch.usi.paxosfs.rpc.FSError;
import ch.usi.paxosfs.rpc.FuseOps;
import ch.usi.paxosfs.rpc.Response;
import fuse.Errno;
import org.apache.thrift.TException;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;
import org.apache.thrift.transport.TTransportException;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * Created by pacheco on 24/11/14.
 */
public class MicroBenchGetdir implements MicroBench {
    private String replicaAddr;
    private byte partition;
    private String logPrefix;
    private String path;
    private Random rand;
    private int globals;

    private class Worker implements Runnable {
        private final int globals;
        private FuseOps.Client c;
        private int id;
        private BufferedWriter out;
        private long workerDuration;
        private String path;
        private Map<Byte, Long> instanceMap = new HashMap<>();

        public Worker(int id, long durationMillis, String path, int globals) throws IOException {
            this.id = id;
            this.workerDuration = durationMillis;
            this.path = path;
            this.globals = globals;

            String replicaHost = replicaAddr.split(":")[0];
            int replicaPort = Integer.parseInt(replicaAddr.split(":")[1]);
            TTransport transport = new TSocket(replicaHost, replicaPort);
            try {
                transport.open();
            } catch (TTransportException e) {
                throw new RuntimeException(e);
            }
            TProtocol protocol = new TBinaryProtocol(transport);
            c = new FuseOps.Client(protocol);
            out = new BufferedWriter(new FileWriter(new File(logPrefix + this.id)));
        }

        private boolean doGlobal() {
            return rand.nextInt(100) < this.globals;
        }

        private String outputLine(long start, long now, int type) {
            return start + "\t" + now + "\t" + (now - start) + "\t" + type + "\n";
        }

        @Override
        public void run() {
            long benchStart = System.currentTimeMillis();
            long benchNow = System.currentTimeMillis();
            while ((benchNow - benchStart) < workerDuration) {
                boolean global = doGlobal(); // should we submit a global command?
                long start = System.currentTimeMillis();
                int type;
                try {
                    try {
                        if (global) {
                            type = 1;
                            Response r = c.getdir("/", instanceMap);
                            instanceMap.putAll(r.instanceMap);
                        } else {
                            type = 0;
                            Response r = c.getdir(path, instanceMap);
                            instanceMap.putAll(r.instanceMap);
                        }
                    } catch (FSError e) {
                        if (e.getErrorCode() == Errno.ETIMEDOUT) {
                            type = 2;
                            System.err.println("# " + e.getMessage());
                        } else if (e.getErrorCode() == Errno.EAGAIN) {
                            type = 3;
                            System.err.println("# " + e.getMessage());
                        } else {
                            throw e;
                        }
                    }
                    long end = System.currentTimeMillis();
                    benchNow = end;
                    try {
                        out.write(outputLine(start, end, type));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } catch (TException e) {
                    System.err.println("# Error (connection closed?)");
                }
            }
            try {
                out.flush();
                out.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public MicroBenchGetdir() {
    }

    @Override
    public void setup(String replicaAddr, byte partition, String logPrefix, Random rand, String[] benchArgs) throws TException {
        this.replicaAddr = replicaAddr;
        this.partition = partition;
        this.logPrefix = logPrefix;
        this.path = "/" + this.partition;
        this.rand = rand;
        this.globals = Integer.parseInt(benchArgs[0]);
		/*
		 * Create paths used by the benchmark
		 */
        String replicaHost = replicaAddr.split(":")[0];
        int replicaPort = Integer.parseInt(replicaAddr.split(":")[1]);
        TTransport transport = new TSocket(replicaHost, replicaPort);
        try {
            transport.open();
        } catch (TTransportException e) {
            throw new RuntimeException(e);
        }
        TProtocol protocol = new TBinaryProtocol(transport);
        FuseOps.Client c = new FuseOps.Client(protocol);
        try {
            c.mkdir(path, 0, 0, 0, new HashMap<Byte, Long>());
        } catch (TException e) {
            e.printStackTrace();
        }
        transport.close();
    }

    @Override
    public Thread startWorker(int workerId, long durationMillis) throws IOException {
        Thread t = new Thread(new Worker(workerId, durationMillis, path, globals));
        t.start();
        return t;
    }
}
