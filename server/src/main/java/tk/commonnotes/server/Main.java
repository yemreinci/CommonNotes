package tk.commonnotes.server;

import java.io.IOException;

public class Main {
	public static void main(String[] args) {
		int port = 8000; // default port

		// parse port argument
		if (args.length > 0) {
			try {
				port = Integer.parseInt(args[0]);
			} catch (NumberFormatException e) {
				System.err.println("Port should be an integer");
				return;
			}
		}

		Server server = null;

		try {
			server = new Server(port);
		} catch (IOException e) {
			System.err.print("E - failed to bind server");
			e.printStackTrace();
			return;
		}

		// start server for accepting connections
		server.start();
	}
}
