package tk.commonnotes.server;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import tk.commonnotes.ot.Replace;

public class Manager implements Runnable {
    private int noteId;
    private List<ClientHandler> clientHandlers;

    private StringBuilder text;

    public Manager(int noteId) {
        clientHandlers = new LinkedList<ClientHandler>();
        text = new StringBuilder();
        this.noteId = noteId;
    }

    public void register(ClientHandler clientHandler) {
        clientHandlers.add(clientHandler);
    }

    public void broadcastOperation(int fromClientId, Replace operation) {
        operation.apply(text);

        for (Iterator<ClientHandler> iterator = clientHandlers.iterator(); iterator.hasNext(); ) {
            ClientHandler clientHandler = iterator.next();

            if (clientHandler.isDead()) {
                iterator.remove();
                continue;
            }

            int clientId = clientHandler.getId();

            if (clientId == fromClientId) {
                continue;
            }

            clientHandler.sendOperation(operation);
        }
    }

    public synchronized StringBuilder getText() {
        return text;
    }

    @Override
    public void run() {
        while (true) {
            try {
                Thread.sleep(20000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            synchronized (this) {
                // send acknowledgement message to every client
                broadcastOperation(-1, new Replace());
            }
        }
    }
}
