package tk.commonnotes.server;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.LinkedList;
import java.util.ListIterator;

import tk.commonnotes.ot.Message;
import tk.commonnotes.ot.operation.DeleteNote;
import tk.commonnotes.ot.operation.Operation;
import tk.commonnotes.ot.operation.Replace;

public class ClientHandler implements Runnable {
    private ObjectInputStream objectInputStream;
    private ObjectOutputStream objectOutputStream;
    private int id;
    private final Manager manager;
    private LinkedList<Replace> sentReplaces;
    private int numAcknowledged = 0;
    private int numExecuted = 0;
    private boolean dead = false;

    public ClientHandler(int id,
                         Manager manager,
                         ObjectInputStream objectInputStream,
                         ObjectOutputStream objectOutputStream)
            throws IOException {

        this.id = id;
        this.manager = manager;
        this.objectInputStream = objectInputStream;
        this.objectOutputStream = objectOutputStream;
        this.sentReplaces = new LinkedList<Replace>();
    }

    private void sendCurrentText() throws IOException {
        objectOutputStream.writeObject(manager.getText().toString());
        objectOutputStream.flush();
    }

    public int getId() {
        return id;
    }

    @Override
    public void run() {
        try {
            // register for the note and send the current state of the text
            synchronized (manager) {
                manager.register(this);

                sendCurrentText();

                if (manager.isDeleted()) { // note is already deleted
                    sendOperation(new DeleteNote());
                    die();
                }
            }

            while (true) {
                Message message = (Message) objectInputStream.readObject();

                if (message == null) {
                    throw new NullPointerException();
                }

                Operation operation = message.getOperation();

                synchronized (manager) {
                    System.out.println("D - handling op from " + id);

                    if (operation.getType().equals("delete-note")) {
                        manager.setDeleted();
                    }
                    else if (operation.getType().equals("replace")) {
                        Replace receivedOp = (Replace) operation;

                        receivedOp.apply(manager.getText());

                        // Remove acknowledged messages
                        while (numAcknowledged < message.getNumExecuted()) {
                            sentReplaces.removeFirst();
                            numAcknowledged++;
                        }

                        // Transform concurrent operations
                        for (ListIterator<Replace> iter = sentReplaces.listIterator(); iter.hasNext(); ) {
                            Replace sentOp = (Replace) iter.next();

                            Replace t1 = receivedOp.transform(sentOp, false);
                            Replace t2 = sentOp.transform(receivedOp, true);

                            receivedOp = t1;
                            iter.set(t2); // sentOp
                        }

                        numExecuted++;
                    }

                    // Send operation to every client
                    manager.broadcastOperation(id, operation);

                    System.out.println("D - done handling op from " + id);
                }

            }
        } catch (ClassNotFoundException | IOException | NullPointerException e) {
            System.out.println("E - error in client handler loop.");
            e.printStackTrace();
            die();
        }
    }

    private void die() {
        // Gracefully exit
        if (!isDead()) {
            System.out.println("I - client " + id + " is dying.");
            try {
                objectInputStream.close();
                objectOutputStream.close();
            } catch (IOException e) {
                System.out.println("E - error when closing streams");
                e.printStackTrace();
            } finally {
                dead = true;
                System.out.println("I - client " + id + " is dead");
            }
        }
    }

    public void sendOperation(Operation operation) {
        if (!isDead()) {
            try {
                Message msg = new Message(operation, numExecuted);

                if (operation.getType().equals("replace")) {
                    sentReplaces.add((Replace) operation);
                }

                objectOutputStream.writeObject(msg);
            } catch (IOException e) {
                e.printStackTrace();
                die();
            }
        }
    }

    public boolean isDead() {
        return dead;
    }
}
