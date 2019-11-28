package tk.commonnotes.server;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;

import tk.commonnotes.common.message.Message;
import tk.commonnotes.common.Replace;

public class ClientHandler implements Runnable {
    private ObjectInputStream objectInputStream;
    private ObjectOutputStream objectOutputStream;
    private int id;
    private final Manager manager;
    private ArrayList<Replace> outgoing;
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
        this.outgoing = new ArrayList<Replace>();
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
                    for (int i = message.getNumExecuted(); i < outgoing.size(); i++) {
                        Replace t1 = op.transform(outgoing.get(i), false);
                        Replace t2 = outgoing.get(i).transform(op, true);

                        op = t1;
                        outgoing.set(i, t2);
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

    public void sendOperation(Replace op) throws IOException {
        if (!isDead()) {
            Message msg = new Message(op, numExecuted);

            outgoing.add(op);
            objectOutputStream.writeObject(msg);
        }
    }

    public boolean isDead() {
        return dead;
    }
}
