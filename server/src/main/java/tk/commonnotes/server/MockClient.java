package tk.commonnotes.server;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import tk.commonnotes.ot.Message;
import tk.commonnotes.ot.operation.Replace;

public class MockClient implements Runnable {
    int numExecuted = 0;
    StringBuilder text = new StringBuilder();

    public void run() {
        try {
            Socket sock = new Socket("52.174.25.75", 9000);
            ObjectOutputStream out = new ObjectOutputStream(sock.getOutputStream());
            ObjectInputStream in = new ObjectInputStream(sock.getInputStream());
            final Random r = new Random();

            HashMap<String, Object> request = new HashMap<>();
            request.put("type", "listNotes");

            out.writeObject(request);
            out.flush();

            List<HashMap<String, Object>> notes = (List) in.readObject();

            int noteId = -1;
            if (notes.size() > 0) {
                noteId = (int) notes.get(r.nextInt(notes.size())).get("noteId");
            }

            if (notes.size() == 0 || (notes.size() < 10 && r.nextInt(100) < 30)) {
                sock = new Socket("52.174.25.75", 9000);
                out = new ObjectOutputStream(sock.getOutputStream());
                in = new ObjectInputStream(sock.getInputStream());

                request = new HashMap<>();

                request.put("type", "newNote");

                out.writeObject(request);
                out.flush();

                HashMap<String, Object> response = (HashMap<String, Object>) in.readObject();

                if (r.nextInt(100) < 30) {
                    noteId = (int) response.get("note" +
                            "Id");
                }

            }
//            else if (notes.size() > 2 && r.nextInt(100) < 20) {
//                sock = new Socket("52.174.25.75", 9000);
//                out = new ObjectOutputStream(sock.getOutputStream());
//                in = new ObjectInputStream(sock.getInputStream());
//
//                request = new HashMap<>();
//                noteId = (int) notes.get(r.nextInt(notes.size())).get("noteId");
//
//                request.put("type", "deleteNote");
//                request.put("noteId", noteId);
//
//                out.writeObject(request);
//                out.flush();
//
//                return;
//            }

            assert noteId != -1;

            sock = new Socket("52.174.25.75", 9000);
            out = new ObjectOutputStream(sock.getOutputStream());
            in = new ObjectInputStream(sock.getInputStream());

            request = new HashMap<>();
            request.put("type", "connectNote");
            request.put("noteId", noteId);

            out.writeObject(request);
            out.flush();

            String choices[] = {"a", "b", "ab", "", "c", "\n", "def", "e"};

            text.append((String) in.readObject());

            for (int i = 0; i < r.nextInt(100); i++) {
                int bi = r.nextInt(text.length() + 1);
                int ei = bi + r.nextInt(text.length() + 1 - bi);
                String str = choices[r.nextInt(choices.length)];
                Replace op = new Replace(bi, ei, str);
                op.apply(text);
                Message msg = new Message(op, 0);
                out.writeObject(msg);
                out.flush();

                try {Thread.sleep(r.nextInt(100));} catch (Exception e) {}
            }

        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws Exception {
        Random r = new Random();
        for (int i = 0; i < 100; i++) {
            System.out.println("session " + i );
            int nCli = r.nextInt(40);

            Thread[] clis = new Thread[nCli];

            for (int j = 0; j < nCli; j++) {
                clis[j] = new Thread(new MockClient());
            }

            for (int j = 0; j < nCli; j++) {
                clis[j].start();
            }

            for (int j = 0; j < nCli; j++) {
                clis[j].join();
            }
            System.out.println("done");
            try{
                Thread.sleep(5000);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

}
