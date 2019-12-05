package tk.commonnotes;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.HashMap;

import tk.commonnotes.ot.Message;
import tk.commonnotes.ot.operation.Replace;

public class Generator {
    static int numExecuted = 0;
    static StringBuilder text = new StringBuilder();
    static Object lock = new Object();

    public static int newNote() throws Exception {
        Socket sock = new Socket("52.174.25.75", 9000);

        final ObjectOutputStream out = new ObjectOutputStream(sock.getOutputStream());
        final ObjectInputStream in = new ObjectInputStream(sock.getInputStream());

        HashMap<String, Object> request = new HashMap<>();

        request.put("type", "newNote");

        out.writeObject(request);
        out.flush();

        System.out.println("create sent");

        HashMap<String, Object> response = (HashMap<String, Object>) in.readObject();

        int noteId = (int) response.get("noteId");

        in.close();
        out.close();
        sock.close();

        return noteId;
    }

    public static void main(String[] args) throws Exception {
//        Socket sock = new Socket("52.174.25.75", 8001);
//        Socket sock = new Socket("localhost", 8080);

        String[] notes = {
                    "TODO for 436 Project\n" +
                        "• Learn how to use TCP sockets\n" +
                        "    ◦ What are server and client sockets?\n" +
                        "    ◦ How to send and receive bytes over tcp connections?\n" +
                        "    ◦ How to serialize Java objects into tcp streams?" +
                        "• Implement the Android application\n" +
                        "    ◦ Build a release APK\n" +
                        "• Implement the server side\n" +
                        "• Prepare presentation\n" +
                        "    ◦ Add source code screenshots\n",

                    "Shopping List\n" +
                            "• Eggs\n" +
                            "• Carrots\n" +
                            "• Milk\n",

                    "Notes from 436 lecture\n" +
                            "\n" +
                            "A token is sent to a process Pi" +
                            " when current local state from Pi\n" +
                            "happened before some other local state in the candidate global state\n" +
                            "    • Once the monitor process for Pi" +
                            " has eliminated the current local" +
                            "state\n" +
                            "  – receive a new local state from the application process\n" +
                            "  – check for consistency conditions again. Note that all states on the candidate" +
                            "    cut satisfy local predicates. However, the states may not be mutually" +
                            "    concurrent, that is, the candidate cut may not be a consistent cut. ",

                "Operational Tranformation\n" +
                        "\n" +
                        "Operational transformation (OT) is a technology for supporting a range of collaboration functionalities in advanced collaborative software systems.\n" +
                        "OT was originally invented for consistency maintenance and concurrency control in collaborative editing of plain text documents.\n\n" +
                        "Two decades of research have extended its capabilities and expanded its applications to include:\n" +
                        "  • group undo,\n" +
                        "  • locking,\n" +
                        "  • conflict resolution,\n" +
                        "  • operation notification and compression,\n" +
                        "  • group-awareness,\n" +
                        "  • HTML/XML and tree-structured document editing,\n" +
                        "  • collaborative office productivity tools,\n" +
                        "  • application-sharing,\n" +
                        "  • and collaborative computer-aided media design tools (see OTFAQ).\n\n" +
                        " In 2009 OT was adopted as a core technique behind the collaboration features in Apache Wave and Google Docs."
        };

        for (int i = 0; i < notes.length; i++) {

            Socket sock = new Socket("52.174.25.75", 9000);
            System.out.println("socket created");

            ObjectOutputStream out = new ObjectOutputStream(sock.getOutputStream());
            ObjectInputStream in = new ObjectInputStream(sock.getInputStream());

            text.setLength(0);

            HashMap<String, Object> request = new HashMap<>();

            request.put("type", "newNote");

            out.writeObject(request);
            out.flush();

            System.out.println("create sent");

            HashMap<String, Object> response = (HashMap<String, Object>) in.readObject();

            int noteId = (int) response.get("noteId");

            System.out.println("noteid " + noteId);

            sock = new Socket("52.174.25.75", 9000);
            out = new ObjectOutputStream(sock.getOutputStream());
            in = new ObjectInputStream(sock.getInputStream());

            request = new HashMap<>();
            request.put("type", "connectNote");
            request.put("noteId", noteId);

            out.writeObject(request);
            out.flush();

            System.out.println("request sent");

            String initialText = (String) in.readObject();
            text.append(initialText);

            String str = notes[i];

            out.writeObject(new Message(new Replace(0, 0, str), 0));

            out.flush();
            out.close();
        }
    }

}
