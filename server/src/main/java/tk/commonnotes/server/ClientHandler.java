package tk.commonnotes.server;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.LinkedList;
import java.util.ListIterator;

import tk.commonnotes.ot.Message;
import tk.commonnotes.ot.Replace;

public class ClientHandler implements Runnable {
    private ObjectInputStream objectInputStream;
    private ObjectOutputStream objectOutputStream;
    private int id;
    private final Manager manager;
    private LinkedList<Replace> outgoing;
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
        this.outgoing = new LinkedList<Replace>();
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
                System.out.println("D - sending current text to " + id);
                manager.register(this);
                sendCurrentText();
                System.out.println("D - done sending to " + id);
            }

            while (true) {
                Message message = (Message) objectInputStream.readObject();

                if (message == null) {
                    throw new NullPointerException();
                }

                Replace op = message.getOperation();

                synchronized (manager) {
                    System.out.println("D - handling op from " + id);

                    // Remove acknowledged messages
                    while (numAcknowledged < message.getNumExecuted()) {
                        outgoing.removeFirst();
                        numAcknowledged++;
                    }

                    // Transform concurrent operations
                    for (ListIterator<Replace> iter = outgoing.listIterator(); iter.hasNext(); ) {
                        Replace qOp = iter.next();

                        Replace t1 = op.transform(qOp, false);
                        Replace t2 = qOp.transform(op, true);

                        op = t1;
                        iter.set(t2);
                    }

                    numExecuted++;

                    // Send operation to every client
                    manager.broadcastOperation(id, op);

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

    public void sendOperation(Replace op) {
        if (!isDead()) {
            try {
                Message msg = new Message(op, numExecuted);

                outgoing.add(op);
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
