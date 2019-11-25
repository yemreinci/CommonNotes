package tk.commonnotes.ot;

import androidx.annotation.NonNull;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.io.IOException;

public class Message implements Serializable {

    private Operation operation;
    private boolean priority;

    /**
     * Number of executed requested from the server
     */
    private int numExecuted;

    public Message(Operation operation, int numExecuted) {
        this.operation = operation;
        this.numExecuted = numExecuted;
    }

    public Message(Operation operation, int numExecuted, boolean priority) {
        this(operation, numExecuted);
        this.priority = priority;
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

    public boolean hasPriority() {
        return priority;
    }
}
