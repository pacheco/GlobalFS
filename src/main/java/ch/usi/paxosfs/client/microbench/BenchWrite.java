package ch.usi.paxosfs.client.microbench;

import ch.usi.paxosfs.client.FileSystemClient;
import ch.usi.paxosfs.rpc.FSError;
import ch.usi.paxosfs.rpc.FileHandle;
import ch.usi.paxosfs.util.UnixConstants;
import org.HdrHistogram.Histogram;
import org.apache.thrift.TException;

import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.Random;

/**
 * Created by pacheco on 10/14/15.
 */
public class BenchWrite extends BenchWorker {
    private final Random rand = new Random();
    private final FileSystemClient fs;
    private final String path;
    private final FileHandle fh;
    private final ByteBuffer writeData;

    public BenchWrite(FileSystemClient fs, String pathPrefix, int writeSizeBytes, int durationMillis, OutputStream log, Histogram histogram) throws TException {
        super(durationMillis, log, histogram);
        this.fs = fs;
        this.path = pathPrefix + Math.abs(rand.nextLong());

        fs.mknod(path , 0755, 0);
        fh = fs.open(path, UnixConstants.O_WRONLY | UnixConstants.O_APPEND);
        byte[] data = new byte[writeSizeBytes];
        rand.nextBytes(data);
        writeData = ByteBuffer.wrap(data);
        writeData.mark();
    }

    @Override
    public int doOperation() throws Exception {
        try {
            writeData.reset();
            fs.write(path, fh, 0, writeData);
        } catch (FSError e) {
            return e.errorCode;
        } catch (TException e) {
            throw e;
        }
        return 0;
    }
}
