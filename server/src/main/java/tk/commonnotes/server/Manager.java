package tk.commonnotes.server;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import tk.commonnotes.ot.operation.Operation;
import tk.commonnotes.ot.operation.Replace;


/**
 * Manager class manages clients that are connected to a note
 */
public class Manager implements Runnable {
    private int noteId;
    private List<ClientHandler> clientHandlers;
    private boolean isDeleted;

    private StringBuilder text;

    public Manager(int noteId) {
        clientHandlers = new LinkedList<ClientHandler>();
        text = new StringBuilder();
        this.noteId = noteId;
        this.isDeleted = false;
    }

    public boolean isDeleted() {
        return isDeleted;
    }

    public void setDeleted() {
        isDeleted = true;
    }

    /**
     * Register clientHandler for broadcast operations
     */
    public void register(ClientHandler clientHandler) {
        clientHandlers.add(clientHandler);
    }

    /**
     * Send operation from fromClientId to every other client
     */
    public void broadcastOperation(int fromClientId, Operation operation) {
        for (Iterator<ClientHandler> iterator = clientHandlers.iterator(); iterator.hasNext(); ) {
            ClientHandler clientHandler = iterator.next();

            // remove dead clients
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

            if (isDeleted) { // no need for ACK if deleted
                break;
            }

            synchronized (this) {
                System.out.println("D - broadcasting ack");
                // send acknowledgement message to every client
                broadcastOperation(-1, new Replace());
                System.out.println("D - done broadcasting ack");
            }
        }
    }
}
