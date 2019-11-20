// This is a separate thread that services each new connecting client.
// A delightful program by Calvin Golas and Harrison Crisman

import java.net.*;
import java.io.*;
import java.util.ArrayList;

public class Connection implements Runnable{
	private Socket client;
	BufferedReader fromClient = null;
	BufferedWriter toClient = null;
	ArrayList<Socket> clients;
	Socket currentSock;

	public Connection(Socket client, ArrayList<Socket> clients) {
		this.client = client;
		this.clients = clients;
	}
	@Override
	public void run() {
		try {
			fromClient = new BufferedReader(new InputStreamReader(client.getInputStream()));
			while (true) {
				//Makes our toClient point back to the current connection
				toClient = new BufferedWriter(new OutputStreamWriter(client.getOutputStream()));
				// Read a message from the client
				String message = fromClient.readLine();
				// Send the message to all clients connected to the server.
				System.out.println(clients.toString());
				for (int i = 0; i < clients.size(); i++) {
					Socket currentSock = clients.get(i);
					toClient = new BufferedWriter(new OutputStreamWriter(currentSock.getOutputStream()));
					toClient.write(message + "\r\n");
					toClient.flush();
				}
			}
		} catch (SocketException se) {
			//Handle when we have a disconnected client on our hands.
			clients.remove(currentSock);
		} catch (IOException ioe) {
			System.err.println(ioe);
		} finally {
			// close streams and socket
			if (currentSock != null) {
				try {
					currentSock.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			if (fromClient != null) {
				try {
					fromClient.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			if (toClient != null) {
				try {
					toClient.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

}
