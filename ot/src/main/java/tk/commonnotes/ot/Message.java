package tk.commonnotes.ot;

import java.io.Serializable;

import tk.commonnotes.ot.operation.Operation;


public final class Message implements Serializable {

    private Operation operation;

    /**
     * Number of executed messages
     */
    private int numExecuted;

    public Message(Operation operation, int numExecuted) {
        this.operation = operation;
        this.numExecuted = numExecuted;
    }

    public Operation getOperation() {
        return operation;
    }

    public int getNumExecuted() {
        return numExecuted;
    }

    @Override
    public String toString() {
        return String.format("Message(%d, %s)", numExecuted, operation.toString());
    }
}
