package tk.commonnotes.server;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;

import tk.commonnotes.ot.Message;

public class Client implements Runnable {
    private InputStream inputStream;
    private OutputStream outputStream;
    private ObjectInputStream objectInputStream;
    private ObjectOutputStream objectOutputStream;
    private int id;
    private Manager manager;

    public Client(int id, Manager manager, InputStream inputStream, OutputStream outputStream)
            throws IOException {

        this.id = id;
        this.manager = manager;
        this.inputStream = inputStream;
        this.outputStream = outputStream;
        this.objectInputStream = new ObjectInputStream(inputStream);
        this.objectOutputStream = new ObjectOutputStream(outputStream);
    }

    public int getId() {
        return id;
    }

    @Override
    public void run() {
        while (true) {
            try {
                Message message = (Message) objectInputStream.readObject();

                if (message == null) {
                    throw new NullPointerException();
                }

                manager.handleMessage(id, message);
            } catch (ClassNotFoundException | IOException | NullPointerException e) {
                e.printStackTrace();
                break;
            }
        }
    }

    public void sendMessage(Message message) throws IOException {
        objectOutputStream.writeObject(message);
    }
}
