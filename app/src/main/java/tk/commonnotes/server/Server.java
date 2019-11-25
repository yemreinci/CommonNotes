package tk.commonnotes.server;

import java.io.IOException;
import java.net.*;
import java.util.*;

import tk.commonnotes.ot.Message;

public class Server {
	private ServerSocket serverSocket;
	private StringBuilder note;
	private List<Message> messages;
	private int idCount;
	private Manager manager;

	public Server() {
		try {
			serverSocket = new ServerSocket(8080, 10);
		} catch (IOException e) {
			e.printStackTrace();
		}

		manager = new Manager();
	}
	
	public void start() {
		while (true) {
			Socket clientSock = null;
			Client client = null;
			
			try {
				clientSock = serverSocket.accept();
			} catch (IOException e) {
				e.printStackTrace();
			}

			if (clientSock != null) {
				try {
					client = new Client(idCount++,
							manager,
							clientSock.getInputStream(),
							clientSock.getOutputStream());
				} catch (IOException e) {
					e.printStackTrace();
				}
			}

			if (client != null) {
				System.out.println("I - new client connected with id = " + client.getId());

				manager.addClient(client);
				(new Thread(client)).start();
			}
		}
	
	}

}

