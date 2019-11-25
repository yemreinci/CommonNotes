package tk.commonnotes.server;

import android.graphics.Path;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import tk.commonnotes.ot.Message;
import tk.commonnotes.ot.Operation;

public class Manager {
    class LogItem {
        int clientId;
        Operation operation;

        public LogItem(int clientId, Operation operation) {
            this.clientId = clientId;
            this.operation = operation;
        }
    }

    private LinkedList<LogItem> logs;
    private List<Client> clients;
    private HashMap<Integer, Integer> numExecuted;

    public Manager() {
        logs = new LinkedList<LogItem>();
        numExecuted = new HashMap<Integer, Integer>();
        clients = new LinkedList<Client>();
    }

    public void addClient(Client client) {
        clients.add(client);
    }

    public void broadcastOperation(int fromClientId, Operation operation) {
        for (Client client: clients) {
            int clientId = client.getId();

            if (clientId == fromClientId) {
                continue;
            }

            int numExecutedFromClient = 0;

            if (numExecuted.containsKey(clientId)) {
                numExecutedFromClient = numExecuted.get(clientId);
            }

            try {
                client.sendMessage(new Message(operation, numExecutedFromClient, fromClientId < clientId));
            } catch (IOException e) {
                System.out.println("E - sending message to " + clientId + " failed");
                e.printStackTrace();
            }
        }
    }

    public synchronized void handleMessage(int clientId, Message message) {
        System.out.println("I - received message from " + clientId + " " + message);

        int numExecutedFromClient = 0;
        if (numExecuted.containsKey(clientId)) {
            numExecutedFromClient = numExecuted.get(clientId);
        }

        int numNotExecuted = logs.size() - numExecutedFromClient - message.getNumExecuted();

        Iterator<LogItem> iter = logs.iterator();
        LinkedList<Operation> nonExecutedOperations = new LinkedList<Operation>();
        LinkedList<Boolean> priorities = new LinkedList<Boolean>();
        while (numNotExecuted > 0 && iter.hasNext()) {
            LogItem item = iter.next();
            if (item.clientId != clientId) {
                numNotExecuted--;
                nonExecutedOperations.addFirst(item.operation);
                priorities.addFirst(item.clientId < clientId);
            }
        }

        Operation transformedOperation = message.getOperation();
        for (Operation operation: nonExecutedOperations) {
            boolean pri = priorities.pop();
            transformedOperation = transformedOperation.transform(operation, true);
        }

        logs.addFirst(new LogItem(clientId, transformedOperation));
        numExecuted.put(clientId, numExecutedFromClient + 1);

        System.out.println("I - transformed operation: " + transformedOperation);

        broadcastOperation(clientId, transformedOperation);
    }
}
