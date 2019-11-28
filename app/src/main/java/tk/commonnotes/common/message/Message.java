package tk.commonnotes.common.message;

import tk.commonnotes.common.Replace;

public final class Message extends AbstractMessage {

    private Replace operation;
    private boolean priority;

    /**
     * Number of executed requested from the server
     */
    private int numExecuted;

    public Message(Replace operation, int numExecuted) {
        this.operation = operation;
        this.numExecuted = numExecuted;
    }

    public Message(Replace operation, int numExecuted, boolean priority) {
        this(operation, numExecuted);
        this.priority = priority;
    }

    public Replace getOperation() {
        return operation;
    }

    public int getNumExecuted() {
        return numExecuted;
    }

    @Override
    public String toString() {
        return String.format("Message(%d, %s)", numExecuted, operation.toString());
    }

    public boolean hasPriority() {
        return priority;
    }
}
