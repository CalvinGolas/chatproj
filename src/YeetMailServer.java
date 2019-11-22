// A delightful implementation of the YeetMail Protocol
// By Calvin Golas and Harrison Crisman
import java.net.*;
import java.io.*;
import java.nio.Buffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.*;
public class YeetMailServer {
	// The port our server listens on!
	public static final int DEFAULT_PORT = 7331;
	// Construct a thread pool for concurrency
	public static final Executor exec = Executors.newCachedThreadPool();

	public static void main(String[] args) throws IOException {
		ServerSocket sock = null;
		// This is gonna store the clients
		HashMap<String, BufferedWriter> clients = new HashMap<String, BufferedWriter>();
		try{
			// Establish the server socket connection.
			sock = new ServerSocket(DEFAULT_PORT);

			while(true) {
				// Listens for connections and then accepts and appends them to the list.
				Socket pairing = null;
				pairing = sock.accept();
				if(pairing != null) {
					//clients.add(new BufferedWriter(new OutputStreamWriter(pairing.getOutputStream())));
					Runnable task = new Connection(pairing, clients);
					exec.execute(task);
				}
			}
		}
		catch(IOException ioe) {
			System.err.println(ioe);
		}
		finally{
			if (sock != null)
				sock.close();
		}
	}
}
