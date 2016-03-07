package ch.usi.paxosfs.client.microbench;

import ch.usi.paxosfs.client.FileSystemClient;
import org.HdrHistogram.Histogram;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

/**
 * Created by pacheco on 10/14/15.
 */
public abstract class BenchWorker implements Runnable {
    private final long durationMillis;
    private Histogram histogram = null;
    private BufferedWriter log;

    public BenchWorker(long durationMillis, OutputStream log) {
        this(durationMillis, log, null);
    }

    public BenchWorker(long durationMillis, OutputStream log, Histogram histogram) {
        this.durationMillis = durationMillis;
        this.histogram = histogram;
        this.log = new BufferedWriter(new OutputStreamWriter(log));
    }

    public abstract int doOperation() throws Exception;

    private void doSample() throws Exception {
        long start = System.nanoTime() / 1000;
        int rv = doOperation();
        long end = System.nanoTime() / 1000;
        if (histogram != null) {
            this.histogram.recordValue(end - start);
        }

        log.write(String.format("%d %d %d\n", start, end, rv));
    }

    @Override
    public void run() {
        long start = System.nanoTime()/1000;
        try {
            long now = start;
            while (now - start < durationMillis*1000) {
                doSample();
                now = System.nanoTime()/1000;
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        try {
            log.flush();
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }
}
