package tk.commonnotes.app;


import android.util.Log;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.HashMap;
import tk.commonnotes.ot.Message;


/**
 * background process for editnote activity
 * sends and receives messages in background threads
 */
public class EditNoteBackground {
    private EditNote activity;
    private BlockingQueue<Message> messagesToSend;
    private Runnable backgroundJob;
    private Socket sock;

    public EditNoteBackground(final EditNote activity, final BlockingQueue<Message> messagesToSend) {
        this.activity = activity;
        this.messagesToSend = messagesToSend;

        backgroundJob = new Runnable() {
            @Override
            public void run() {
                try {
                    sock = new Socket(Config.serverAddress, Config.serverPort);

                    HashMap<String, Object> request = new HashMap<>();
                    request.put("type", "connectNote");
                    request.put("noteId", activity.noteId);

                    final ObjectOutputStream out = new ObjectOutputStream(sock.getOutputStream());

                    out.writeObject(request);
                    out.flush();

                    // sender job sends messages pushed to messages queue
                    Runnable sender = new Runnable() {
                        @Override
                        public void run() {
                            while (true) {
                                Message message = messagesToSend.pop();

                                try {
                                    out.writeObject(message);
                                    out.flush();
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }

                                if (message.getOperation().getType().equals("delete-note")) {
                                    activity.exit();
                                }
                            }
                        }
                    };

                    (new Thread(sender)).start();

                    ObjectInputStream in = new ObjectInputStream(sock.getInputStream());

                    final String initialText = (String) in.readObject();
                    Log.d("yunus", "initial text: " + initialText);

                    activity.setInitialText(initialText);

                    // receive loop
                    while (true) {
                        final Message message = (Message) in.readObject();

                        if (message == null) {
                            System.out.println("E - unexpected null message");
                            break;
                        }

                        activity.handleMessage(message);
                    }

                } catch (Exception e) {
                    Log.d("tcpsocket", e.toString());
                    e.printStackTrace();
                }
            }
        };
    }

    public void start() {
        Thread thread = new Thread(backgroundJob);
        thread.start();
    }

    public void stop() {
        if (sock != null) {
            try {
                sock.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
