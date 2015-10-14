package ch.usi.paxosfs.client.microbench;

import ch.usi.paxosfs.client.FileSystemClient;
import ch.usi.paxosfs.replica.ReplicaManagerException;
import org.HdrHistogram.ConcurrentHistogram;
import org.apache.commons.cli.*;
import org.apache.thrift.TException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Created by pacheco on 10/12/15.
 */
public class FixedLoadBench {
    private enum Type {
        CREATE,
        WRITE,
        POPULATE,
        READ
    }

    public static Options cmdlineOpts() {
        Options opts = new Options();
        Option.Builder optionBuilder;

        // Bench type
        OptionGroup benchType = new OptionGroup();
        benchType.setRequired(true);
        optionBuilder = Option.builder().longOpt("create").hasArg(false).desc("Create benchmark");
        benchType.addOption(optionBuilder.build());
        optionBuilder = Option.builder().longOpt("write").hasArg(false).desc("Write benchmark");
        benchType.addOption(optionBuilder.build());
        optionBuilder = Option.builder().longOpt("populate").hasArg(false).desc("Populate (do before read benchmark)");
        benchType.addOption(optionBuilder.build());
        optionBuilder = Option.builder().longOpt("read").hasArg(false).desc("Read benchmark");
        benchType.addOption(optionBuilder.build());
        opts.addOptionGroup(benchType);

        // File path prefix
        optionBuilder = Option.builder("f").desc("File path prefix (e.g. /tmp/fs/1/prefix_")
                .longOpt("file-prefix")
                .hasArg(true)
                .argName("FILE_PREFIX")
                .required(true)
                .type(String.class);
        opts.addOption(optionBuilder.build());

        // Operation size
        optionBuilder = Option.builder("s").desc("Size of the operation (file/write/read size) in bytes")
                .longOpt("size")
                .hasArg(true)
                .argName("BYTES")
                .required(true)
                .type(Number.class);
        opts.addOption(optionBuilder.build());

        // Number of threads
        optionBuilder = Option.builder("t").desc("Number of threads")
                .longOpt("threads")
                .hasArg(true)
                .argName("THREADS")
                .required(true)
                .type(Number.class);
        opts.addOption(optionBuilder.build());

        // Duration
        optionBuilder = Option.builder("d").desc("Benchmark duration in seconds")
                .longOpt("duration")
                .hasArg(true)
                .argName("SECS")
                .required(true)
                .type(Number.class);
        opts.addOption(optionBuilder.build());

        // Log path prefix
        optionBuilder = Option.builder("o").desc("Output path prefix (e.g. /tmp/cli3_)")
                .longOpt("out")
                .hasArg(true)
                .argName("OUT_PREFIX")
                .required(true)
                .type(String.class);
        opts.addOption(optionBuilder.build());

        // filesystem options <n_partitions> <zoohost> <storage> <replica_id> <closest_partition>
        optionBuilder = Option.builder().desc("filesystem: number of partitions")
                .longOpt("fs-partitions")
                .hasArg(true)
                .argName("N")
                .required(true)
                .type(Number.class);
        opts.addOption(optionBuilder.build());

        optionBuilder = Option.builder().desc("filesystem: storage config file")
                .longOpt("fs-storage")
                .hasArg(true)
                .argName("CFG_FILE")
                .required(true)
                .type(File.class);
        opts.addOption(optionBuilder.build());

        optionBuilder = Option.builder().desc("filesystem: closest partition (for sending global commands)")
                .longOpt("fs-partition")
                .hasArg(true)
                .argName("P")
                .required(true)
                .type(Number.class);
        opts.addOption(optionBuilder.build());

        optionBuilder = Option.builder().desc("filesystem: replica to connect to")
                .longOpt("fs-replica")
                .hasArg(true)
                .argName("ID")
                .required(true)
                .type(Number.class);
        opts.addOption(optionBuilder.build());

        optionBuilder = Option.builder().desc("filesystem: zookeeper host:port")
                .longOpt("fs-zookeeper")
                .hasArg(true)
                .argName("HOST:PORT")
                .required(true)
                .type(String.class);
        opts.addOption(optionBuilder.build());

        return opts;
    }

    private static void printHelp(Options opts) {
        HelpFormatter formatter = new HelpFormatter();
        formatter.setOptionComparator(null);
        formatter.setWidth(120);
        formatter.printHelp("FixedLoadBench --populate | --read | --write | --create", opts, false);
    }

    public static void main(String[] rawArgs) throws InterruptedException {
        Options opts = cmdlineOpts();
        CommandLineParser parser = new DefaultParser();
        try {
            CommandLine args = parser.parse(opts, rawArgs);
            int nPartitions = ((Number) args.getParsedOptionValue("fs-partitions")).intValue();
            String storage = ((File) args.getParsedOptionValue("fs-storage")).getAbsolutePath();
            int partition = ((Number) args.getParsedOptionValue("fs-partition")).intValue();
            int replicaId = ((Number) args.getParsedOptionValue("fs-replica")).intValue();
            String zooHost = (String) args.getParsedOptionValue("fs-zookeeper");

            // start the file system
            FileSystemClient fs = new FileSystemClient(nPartitions, zooHost, storage, replicaId, new Integer(partition).byteValue());
            fs.start();
            fs.waitUntilReady();

            // run the benchmark
            String pathPrefix = (String) args.getParsedOptionValue("file-prefix");
            String logPrefix = (String) args.getParsedOptionValue("out");
            ConcurrentHistogram histogram = new ConcurrentHistogram(3);

            int durationSec = ((Number) args.getParsedOptionValue("duration")).intValue();

            int nThreads = ((Number) args.getParsedOptionValue("threads")).intValue();
            List<Thread> workers = new ArrayList<>(nThreads);

            int size = ((Number) args.getParsedOptionValue("size")).intValue();

            // create workers
            for (int i = 0; i < nThreads; i++) {
                FileOutputStream logOut = new FileOutputStream(logPrefix + i);
                if (args.hasOption("create")) {
                    workers.add(new Thread(new BenchCreate(fs, pathPrefix, size, durationSec*1000, logOut, histogram)));
                } else if (args.hasOption("write")) {
                    workers.add(new Thread(new BenchWrite(fs, pathPrefix, size, durationSec*1000, logOut, histogram)));
                } else if (args.hasOption("populate")) {
                    throw new RuntimeException("TODO:");
                } else if (args.hasOption("read")) {
                    throw new RuntimeException("TODO:");
                }
            }
            // start workers
            for (int i = 0; i < nThreads; i++) {
                System.out.println("Starting thread " + i);
                workers.get(i).start();
            }
            // wait for workers to finish
            try {
                for (int i = 0; i < nThreads; i++) {
                    workers.get(i).join();
                    System.out.println("Thread " + i + " done");
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            System.exit(0);
        } catch (ParseException | FileNotFoundException | ReplicaManagerException | TException e) {
            e.printStackTrace();
            printHelp(opts);
        }
    }
}
