package tk.commonnotes;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

import tk.commonnotes.common.message.Message;
import tk.commonnotes.common.Replace;

public class MockClient {
    static int numExecuted = 0;
    static StringBuilder text = new StringBuilder();
    static Object lock = new Object();

    public static int newNote() throws Exception {
        Socket sock = new Socket("localhost", 8080);

        final ObjectOutputStream out = new ObjectOutputStream(sock.getOutputStream());
        final ObjectInputStream in = new ObjectInputStream(sock.getInputStream());

        HashMap<String, Object> request = new HashMap<>();

        request.put("type", "newNote");

        out.writeObject(request);
        out.flush();

        HashMap<String, Object> response = (HashMap<String, Object>) in.readObject();

        int noteId = (int) response.get("noteId");

        in.close();
        out.close();
        sock.close();

        return noteId;
    }

    public static void main(String[] args) throws Exception {
//        Socket sock = new Socket("52.174.25.75", 8001);
//        final int noteId = newNote();
        final int noteId = 0;
        Socket sock = new Socket("localhost", 8080);

        final ObjectOutputStream out = new ObjectOutputStream(sock.getOutputStream());
        final ObjectInputStream in = new ObjectInputStream(sock.getInputStream());
        final ArrayList<Replace> operations = new ArrayList<Replace>();
        final Random r = new Random(1337);

        final Runnable receiver = new Runnable() {
            @Override
            public void run() {
                try {
                    String initialText = (String) in.readObject();
                    text.append(initialText);
                    System.out.println("initial text: " + text);

                    while (true) {
                        Message message = (Message) in.readObject();

                        if (message == null) {
                            throw new NullPointerException();
                        }

                        Replace transformed = (Replace) message.getOperation();

                        synchronized (lock) {
                            System.out.println("text: " + text);
                            System.out.println("received: " + message);

                            for (int i = message.getNumExecuted(); i < operations.size(); i++) {
                                Replace t1 = (Replace) transformed.transform(operations.get(i), true);
                                Replace t2 = (Replace) operations.get(i).transform(transformed, false);

                                transformed = t1;
                                operations.set(i, t2);

                                System.out.println("   transformed: " + transformed);
                            }

                            System.out.println("transformed: " + transformed);

                            text.replace(transformed.bi, transformed.ei, transformed.str);
                            System.out.println(text.toString());

                            numExecuted++;
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };

        Runnable sender = new Runnable() {
            @Override
            public void run() {
                try {
                    HashMap<String, Object> request = new HashMap<>();
                    request.put("type", "connectNote");
                    request.put("noteId", noteId);

                    out.writeObject(request);
                    out.flush();

                    for (int j = 0; j < 20; j++) {
                        for (int i = 0; i < 50; i++) {
                            String[] choices = {"", "a", "b", "aa", "bb", "c", "dc", "e", "asd"};

                            Replace operation;
                            Message message;

                            synchronized (lock) {
                                int bi = r.nextInt(text.length() + 1);
                                int ei = bi + r.nextInt(text.length() + 1 - bi);

                                String str = choices[r.nextInt(choices.length)];
                                operation = new Replace(bi, ei, str);

                                operations.add(operation);
                                text.replace(bi, ei, str);
                                System.out.println(text.toString());

                                message = new Message(operation, numExecuted);

                                System.out.println("text: " + text);
                                System.out.println("sending: " + message);
                            }

                            out.writeObject(message);
                            out.flush();

                            Thread.sleep(2500);
                        }
                        out.flush();
                        Thread.sleep(2000);
                    }
                    Thread.sleep(2000);
                    System.out.println("last: " + text);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };

        Thread t1 = new Thread(receiver);
        Thread t2 = new Thread(sender);

        t1.start();
        t2.start();

        t1.join();
        t2.join();
    }

}
