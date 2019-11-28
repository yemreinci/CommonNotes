package tk.commonnotes.server;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.*;
import java.util.*;

import tk.commonnotes.common.message.Message;

public class Server {
	private ServerSocket serverSocket;
	private StringBuilder note;
	private List<Message> messages;
	private int noteIdCount = 0, cliendIdCount = 0;
	private HashMap<Integer, Manager> managers;

	public Server() {
		managers = new HashMap<>();

		try {
			serverSocket = new ServerSocket(8080, 10);
		} catch (IOException e) {
			System.out.println("E - error when binding server socket");
			e.printStackTrace();
		}
	}

	public void handleNewClient(Socket sock) {
		System.out.println("I - a new client is connected");

		ObjectInputStream in;
		ObjectOutputStream out;

		try {
			in = new ObjectInputStream(sock.getInputStream());
			out = new ObjectOutputStream(sock.getOutputStream());
		} catch (IOException e) {
			System.out.println("E - error when opening streams");
			e.printStackTrace();
			return;
		}

		try {
			HashMap<String, Object> request = (HashMap<String, Object>) in.readObject();

			if ("newNote".equals(request.get("type"))) {
				HashMap<String, Object> response = new HashMap<>();

				managers.put(noteIdCount, new Manager(noteIdCount));
				response.put("noteId", noteIdCount);

				System.out.println("I - new note created: " + noteIdCount);

				++noteIdCount;

				out.writeObject(response);
				out.flush();

				// close connection
				out.close();
				in.close();
				sock.close();
			}
			else if ("connectNote".equals(request.get("type"))) {
				int noteId = (int) request.get("noteId");
				Manager manager = managers.get(noteId);
				ClientHandler clientHandler = new ClientHandler(cliendIdCount, manager, in, out);

				System.out.println("I - client " + cliendIdCount + " connected to note " + noteId);

				++cliendIdCount;

				(new Thread(clientHandler)).start();
			}
			else if("listNotes".equals(request.get("type"))) {
				System.out.println("I - listNotes request");

				LinkedList<HashMap<String, Object>> notes = new LinkedList<>();

				for (HashMap.Entry<Integer, Manager> item: managers.entrySet()) {
					int noteId = item.getKey();
					Manager manager = item.getValue();

					HashMap<String, Object> note = new HashMap<>();

					note.put("noteId", noteId);
					note.put("text", manager.getText().toString());

					notes.add(note);
				}

				out.writeObject(notes);
				out.flush();

				System.out.println("I - sent " + notes.size() + " notes");

				// close connection
				out.close();
				in.close();
				sock.close();
			}

		} catch (IOException e) {
			System.out.println("E - error when communicating.");
			e.printStackTrace();
			return;
		} catch (ClassNotFoundException e) {
			System.out.println("E - unexpected error happened.");
			e.printStackTrace();
			return;
		}
	}
	
	public void start() {
		while (true) {
			Socket clientSock = null;

			try {
				clientSock = serverSocket.accept();
			} catch (IOException e) {
				System.err.println("W - error when accepting");
				e.printStackTrace();
			}

			if (clientSock != null) {
				handleNewClient(clientSock);
			}
		}
	
	}

}

