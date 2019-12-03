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
            synchronized (manager) {
                manager.register(this);
                sendCurrentText();
            }

            while (true) {
                Message message = (Message) objectInputStream.readObject();

                if (message == null) {
                    throw new NullPointerException();
                }

                Replace op = message.getOperation();

                synchronized (manager) {
                    while (numAcknowledged < message.getNumExecuted()) {
                        outgoing.removeFirst();
                        numAcknowledged++;
                    }

                    for (ListIterator<Replace> iter = outgoing.listIterator(); iter.hasNext(); ) {
                        Replace qOp = iter.next();

                        Replace t1 = op.transform(qOp, false);
                        Replace t2 = qOp.transform(op, true);

                        op = t1;
                        iter.set(t2);
                    }

                    numExecuted++;
                    manager.broadcastOperation(id, op);
                }

            }
        } catch (ClassNotFoundException | IOException | NullPointerException e) {
            e.printStackTrace();
            dead = true;
            System.out.println("E - client " + id + " is dead.");
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
                dead = true;
                System.out.println("E - client " + id + " is dead.");
            }
        }
    }

    public boolean isDead() {
        return dead;
    }
}
