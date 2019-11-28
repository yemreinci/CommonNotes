package tk.commonnotes.server;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import tk.commonnotes.common.Replace;

public class Manager {
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
        System.out.println("text: " + text);

        // TODO remove dead clients
        for (ClientHandler clientHandler: clientHandlers) {
            int clientId = clientHandler.getId();

            if (clientId == fromClientId) {
                continue;
            }

            try {
                clientHandler.sendOperation(operation);
            } catch (IOException e) {
                System.out.println("E - sending message to " + clientId + " failed");
                e.printStackTrace();
            }
        }
    }

    public synchronized StringBuilder getText() {
        return text;
    }


//    public synchronized void handleMessage(int clientId, Message message) {
//        System.out.println("I - received message from " + clientId + " " + message);
//
//        int numExecutedFromClient = 0;
//        if (numExecuted.containsKey(clientId)) {
//            numExecutedFromClient = numExecuted.get(clientId);
//        }
//
//        int numNotExecuted = logs.size() - numExecutedFromClient - message.getNumExecuted();
//
//        Iterator<LogItem> iter = logs.iterator();
//        LinkedList<Replace> nonExecutedOperations = new LinkedList<Replace>();
//        LinkedList<Boolean> priorities = new LinkedList<Boolean>();
//        while (numNotExecuted > 0 && iter.hasNext()) {
//            LogItem item = iter.next();
//            if (item.clientId != clientId) {
//                numNotExecuted--;
//                nonExecutedOperations.addFirst(item.operation);
//                priorities.addFirst(item.clientId < clientId);
//            }
//        }
//
//        Replace transformedOperation = message.getOperation();
//        for (Replace operation: nonExecutedOperations) {
//            boolean pri = priorities.pop();
//            transformedOperation = transformedOperation.transform(operation, true);
//        }
//
//        logs.addFirst(new LogItem(clientId, transformedOperation));
//        numExecuted.put(clientId, numExecutedFromClient + 1);
//
//        System.out.println("I - transformed operation: " + transformedOperation);
//
//        broadcastOperation(clientId, transformedOperation);
//    }
}
