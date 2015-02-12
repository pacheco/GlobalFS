package ch.usi.paxosfs.replica;

/**
 * Created by pacheco on 30/11/14.
 */
public enum DebugCommands {
    NULL(0),
    POPULATE_FILE(1);

    private final int id;

    DebugCommands(int i) {
        this.id = i;
    }

    public int getId() {
        return id;
    }
}
