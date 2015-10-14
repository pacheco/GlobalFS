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
public class BenchCreate extends BenchWorker {
    private final Random rand = new Random();
    private final FileSystemClient fs;
    private final String pathPrefix;
    private final ByteBuffer fileData;

    public BenchCreate(FileSystemClient fs, String pathPrefix, int fileSizeBytes, int durationMillis, OutputStream log, Histogram histogram) {
        super(durationMillis, log, histogram);
        this.fs = fs;
        this.pathPrefix = pathPrefix;
        if (fileSizeBytes > 0) {
            byte[] data = new byte[fileSizeBytes];
            fileData = ByteBuffer.wrap(data);
            fileData.mark();
        } else {
            fileData = null;
        }
    }

    @Override
    public int doOperation() throws Exception {
        try {
            String path = pathPrefix + Math.abs(rand.nextLong());
            fs.mknod(path, 0755, 0);
            if (fileData != null) {
                FileHandle fh = fs.open(path, UnixConstants.O_WRONLY | UnixConstants.O_APPEND);
                fileData.reset();
                fs.write(path, fh, 0, fileData);
                fs.release(path, fh, 0);
            }
        } catch (FSError e) {
            return e.errorCode;
        } catch (TException e) {
            throw e;
        }
        return 0;
    }
}
