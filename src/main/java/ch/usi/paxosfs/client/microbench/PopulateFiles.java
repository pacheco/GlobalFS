package ch.usi.paxosfs.client.microbench;

import ch.usi.paxosfs.replica.DebugCommands;
import ch.usi.paxosfs.rpc.Debug;
import ch.usi.paxosfs.rpc.FileHandle;
import ch.usi.paxosfs.rpc.FuseOps;
import ch.usi.paxosfs.rpc.Response;
import org.apache.thrift.TException;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;
import org.apache.thrift.transport.TTransportException;

import java.io.IOException;
import java.util.*;

/**
 * Created by pacheco on 24/11/14.
 */
public class PopulateFiles {
    private static Integer MAX_BLOCKS_PER_COMMAND = 1024; // maximum number of blocks per writeBlocks call
    private static Random rand = new Random();
    private static String replicaAddr;
    private static int blockSize;
    private static int nBlocks;
    private static byte partition;

    private static class Worker implements Runnable {
        private final String path;
        private final String localPath;
        private final String globalPath;
        private FuseOps.Client c;
        private int id;
        private Map<Byte, Long> instanceMap = new HashMap<>();

        public Worker(int id, String path) throws IOException {
            this.id = id;
            this.path = path;
            this.localPath = path + "/f" + Integer.toString(id);
            this.globalPath = "/f" + Integer.toString(id);
            this.instanceMap = new HashMap<>();

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
        }

        @Override
        public void run() {
            FileHandle globalFh;
            FileHandle localFh;

            /* create local partition directory */
            try {
                c.mkdir(path, 0, 0, 0, new HashMap<Byte, Long>());
            } catch (TException e) {
                //e.printStackTrace();
            }

            /* remove/create files */
            try {
                Response r = c.unlink(localPath, instanceMap);
                instanceMap.putAll(r.getInstanceMap());
            } catch (TException e) {
                //e.printStackTrace();
            }
            try {
                Response r = c.unlink(globalPath, instanceMap);
                instanceMap.putAll(r.getInstanceMap());
            } catch (TException e) {
                //e.printStackTrace();
            }
            try {
                Response r = c.mknod(localPath, 0, 0, 0, 0, instanceMap);
                instanceMap.putAll(r.getInstanceMap());
            } catch (TException e) {
                e.printStackTrace();
            }
            try {
                Response r = c.mknod(globalPath, 0, 0, 0, 0, instanceMap);
                instanceMap.putAll(r.getInstanceMap());
            } catch (TException e) {
                e.printStackTrace();
            }

            try {
                Map<String, String> populateCommand = new HashMap<>();
                populateCommand.put("name", localPath);
                populateCommand.put("nBlocks", Integer.toString(nBlocks));
                populateCommand.put("blockSize", Integer.toString(blockSize));
                Debug debugCmd = new Debug();
                debugCmd.setType(DebugCommands.POPULATE_FILE.getId());
                debugCmd.setData(populateCommand);
                Response r = c.debug(debugCmd);
                instanceMap.putAll(r.getInstanceMap());
            } catch (TException e) {
                e.printStackTrace();
            }

            try {
                Map<String, String> populateCommand = new HashMap<>();
                populateCommand.put("name", globalPath);
                populateCommand.put("nBlocks", Integer.toString(nBlocks));
                populateCommand.put("blockSize", Integer.toString(blockSize));
                Debug debugCmd = new Debug();
                debugCmd.setType(DebugCommands.POPULATE_FILE.getId());
                debugCmd.setData(populateCommand);
                Response r = c.debug(debugCmd);
                instanceMap.putAll(r.getInstanceMap());
            } catch (TException e) {
                e.printStackTrace();
            }

            /* add blocks */
//            for (int i = 0; i < nBlocks;) {
//                // write blocks in multiple calls, otherwise the paxos command is too big
//                List<DBlock> blocks = new ArrayList<>();
//                Integer toWrite = Math.min(nBlocks - i, MAX_BLOCKS_PER_COMMAND);
//                for (int j = 0; j < toWrite; j++) {
//                    DBlock b = new DBlock(null, 0, blockSize, new HashSet<Byte>());
//                    b.setId(UUIDUtils.longToBytes(rand.nextLong()));
//                    blocks.add(b);
//                }
//                try {
//                    Response r = c.writeBlocks(localPath, localFh, 0, blocks, instanceMap);
//                    instanceMap.putAll(r.getInstanceMap());
//                    r = c.writeBlocks(globalPath, globalFh, 0, blocks, instanceMap);
//                    instanceMap.putAll(r.getInstanceMap());
//                } catch (TException e) {
//                    e.printStackTrace();
//                }
//                i += toWrite;
//            }
//
//            try {
//                Response r = c.release(globalPath, globalFh, 0, instanceMap);
//                instanceMap.putAll(r.getInstanceMap());
//                r = c.release(localPath, localFh, 0, instanceMap);
//                instanceMap.putAll(r.getInstanceMap());
//            } catch (TException e) {
//                e.printStackTrace();
//            }
        }
    }


    public static void main(String[] args) throws InterruptedException, IOException {
        /*
		 * Get cmdline parameters
		 */
        if (args.length != 5) {
            System.err.println("Populate <replicaAddr> <partition> <nFiles> <nBlocks> <blockSize>");
            return;
        }

        replicaAddr = args[0];
        partition = Byte.parseByte(args[1]);
        int nFiles = Integer.parseInt(args[2]);
        nBlocks = Integer.parseInt(args[3]);
        blockSize = Integer.parseInt(args[4]);

        // create nFile workers
        List<Thread> workers = new LinkedList<>();
        System.out.println("> Starting threads");
        for (int i = 0; i < nFiles; i++) {
            Thread w = new Thread(new Worker(i, "/" + partition));
            workers.add(w);
            w.start();
        }
        System.out.println("> Waiting threads");
        for (Thread w : workers) {
            w.join();
        }
    }
}

