// This is a separate thread that services each new connecting client.
// A delightful program by Calvin Golas and Harrison Crisman

import java.net.*;
import java.io.*;
public class Connection implements Runnable{
	private Socket client;
	public Connection(Socket client) {
		this.client = client;
	}
	@Override
	public void run() {
		try {
			fromClient = new BufferedInputStream(client.getInputStream());
			toClient = new BufferedOutputStream(client.getOutputStream());

		} catch (IOException e) {
			System.err.println(ioe);
		}
	}
}
