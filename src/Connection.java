// This is a separate thread that services each new connecting client.
// A delightful program by Calvin Golas and Harrison Crisman

import java.io.*;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.TimeZone;

public class Connection implements Runnable{
	private Socket client;
	BufferedReader fromClient = null;
	BufferedWriter toClient = null;
	HashMap<String, BufferedWriter> clients;

	public Connection(Socket client, HashMap<String, BufferedWriter> clients) throws IOException {
		this.client = client;
		this.clients = clients;
		toClient = new BufferedWriter(new OutputStreamWriter(this.client.getOutputStream()));
		fromClient = new BufferedReader(new InputStreamReader(this.client.getInputStream()));
	}
	@Override
	public void run() {
		try {
			while (true) {
				String unParsedMessage = "";
				// Read a message from the client
				while (fromClient.ready()) {
					unParsedMessage += fromClient.readLine() + "\r\n";
				}
				if(unParsedMessage.length() == 0){
					continue;
				}
				System.out.println("Begin message:");
				System.out.println(unParsedMessage);
				System.out.println("End message");
				// extract the status code of the message we're receiving from the client
				unParsedMessage.replaceAll("\\r\\n", "\r\n");
				String type = unParsedMessage.substring(unParsedMessage.indexOf(":") + 1, unParsedMessage.indexOf("\r\n")).replaceAll(" ", "");
				//String type = unParsedMessage.substring(unParsedMessage.indexOf(":") + 1, unParsedMessage.indexOf("date")).trim();

				System.out.println("Message type:" + type);
				System.out.println("Unparsed Message: " + unParsedMessage);
				switch (type) {
					case "200": // new user name request/join
						if (!joinServer(unParsedMessage)) {
							return;
						}
						break;
					case "202": // general message
						publicMessage(unParsedMessage);
						break;
					case "203": // private message
						privateMessage(unParsedMessage);
						break;
					case "300": // leaving chat/exit request
						// Let everyone know that the user is exiting the chat
						// Remove the socket from the list
						sendExitMessage(toClient);
						break;
					default:
						System.out.println("Unknown Status Code!");
						break;
				}
			}
		} catch (IOException ioe) {
			System.err.println(ioe);
		} finally {
			// close streams and socket
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

	private Boolean joinServer(String request) throws java.io.IOException {
		// Check if the requested username is in the clients hash map
		request = request.substring(request.indexOf("\r\n") + 2); // Chop out the status code
		String date = request.substring(0, request.indexOf("\r"));
		request = request.substring(request.indexOf("\r\n") + 2);
		String requestedName = request.substring(0, request.indexOf("\r\n"));
		// If the username is already in the dictionary, we send back an error
		if (clients.containsKey(requestedName)) {
			// Send a 401 message
			toClient.write("status: 401\r\n");
			System.out.println("info" + date);
			toClient.write(date + "\r\n");
			toClient.write("from: " + requestedName + "\r\n\r\n\r\n");
			toClient.flush();
			// Close the socket and terminate the thread
			client.close();
			return false;
		} else { // If not, we give them that username and return a 201 message!
			toClient.flush();
			clients.put(requestedName, toClient);
			toClient.write("status: 201\r\n");
			System.out.println("info" + date);
			toClient.write(date + "\r\n");
			toClient.write("from: " + requestedName + "\r\n\r\n\r\n");
			toClient.flush();
			return true;
		}
	}

	// Sends a message out to all connected users in the server
	private void publicMessage(String request) {
		// Forward the message to all connected clients
		for (HashMap.Entry<String, BufferedWriter> user : clients.entrySet()) {
			BufferedWriter toUser = user.getValue();
			try {
				toUser.write(request);
				toUser.flush();
			} catch (IOException e) { // If the client disconnected without sending an exit request
				sendExitMessage(toUser);
			}
		}
	}

	private void privateMessage(String request) throws java.io.IOException {
		//Find the destination user
		String clipped = request.substring(request.indexOf("date:"));
		String date = clipped.substring(0, clipped.indexOf("\r"));
		clipped = request.substring(request.indexOf("to:") + 3).trim();
		String destUser = clipped.substring(0, clipped.indexOf("\r\n"));
		// See if the destination user is in the dictionary
		if (!clients.containsKey(destUser)) { // If user not found, send a 404 back
			sendFourOhFour(date, clients.get(destUser));
		}
		try {
			clients.get(destUser).write(request);
		} catch (IOException e) {
			sendFourOhFour(date, clients.get(destUser));
			sendExitMessage(clients.get(destUser));
		}
	}

	// Sends a 404 message to the client when they try to contact a user that doesn't exist or has disconnected
	private void sendFourOhFour(String date, BufferedWriter missingUser) {
		try {
			toClient.write("status: 404 \r\n");
			toClient.write(date + "\r\n");
			toClient.write("from: " + missingUser + "\r\n\r\n\r\n");
			toClient.flush();
		} catch (IOException e) {
			sendExitMessage(toClient);
		}
	}

	// If a user disconnects we message the new status of their existence to all clients connected to the server
	private void sendExitMessage(BufferedWriter user) {
		for (HashMap.Entry<String, BufferedWriter> possibleMatch : clients.entrySet()) {
			// If we find the matching user, we send the 301 message to everyone!
			if (possibleMatch.getValue().equals(user)) {
				String threeOhOne = "status: 301\r\ndate: " + getDate() + "\r\n" + possibleMatch.getKey() + " has exited the server!\r\n\r\n\r\n";
				clients.remove(possibleMatch.getKey());
				publicMessage(threeOhOne);
				return;
			}
		}
	}

	//Returns the current date and time in UTC
	private String getDate() {
		SimpleDateFormat dateFormatUtc = new SimpleDateFormat("yyyy-MMM-dd HH:mm:ss");
		dateFormatUtc.setTimeZone(TimeZone.getTimeZone("UTC"));

		// Returns time in UTC
		return dateFormatUtc.toString();
	}
}
