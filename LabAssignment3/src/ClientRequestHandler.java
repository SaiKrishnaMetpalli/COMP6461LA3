import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.DatagramChannel;
import java.nio.charset.StandardCharsets;
import java.util.Calendar;

public class ClientRequestHandler {

	private static int port;
	private ByteBuffer buf;

	private Socket client_Socket;
	private static boolean is_Debug_On;
	private static String dir_Path;

	private static String method = "";
	private static String file_Path = null;
	private static String content_Type = "";
	private static String content_Disposition = "";
	private static boolean list_All_Files = false;
	private static boolean is_Overwrite = false;
	private static int status_Code = 200;
	private static StringBuilder respond = new StringBuilder();
	private static StringBuilder main_Response_Data = new StringBuilder();
	static String data = "";

	public ClientRequestHandler(boolean print_Debug_Message, String directory_Path) {

		this.is_Debug_On = print_Debug_Message;
		this.dir_Path = directory_Path;

	}
	
	public static void serveRequestToServer(int port) {
		// TODO Auto-generated method stub
		try (DatagramChannel channel = DatagramChannel.open()) {
			channel.bind(new InetSocketAddress(port));
			System.out.println("\nstatus: "+ channel.isConnected() + " isOPEN: "+channel.isOpen());

			ByteBuffer buf = ByteBuffer.allocate(Packet.MAX_LEN).order(ByteOrder.BIG_ENDIAN);

			System.out.println("IN serveRequestToServer method ");
			for (;;) {
				buf.clear();
				SocketAddress router = channel.receive(buf);
				if (router != null) {
					// Parse a packet from the received raw data.
					buf.flip();
					Packet packet = Packet.fromBuffer(buf);
					System.out.println("\nPacket: "+ packet.toString());
					buf.flip();

					String requestPayload = new String(packet.getPayload(), StandardCharsets.UTF_8);
					System.out.println("\nrequestPayload: "+ requestPayload.toString());
					// Send the response to the router not the client.
					// The peer address of the packet is the address of the client already.
					// We can use toBuilder to copy properties of the current packet.
					// This demonstrate how to create a new packet from an existing packet.

					if (requestPayload.equals("Hi from Client")) {
						System.out.println(requestPayload);
						Packet resp = packet.toBuilder().setPayload("Hi from Server".getBytes()).create();
						channel.send(resp.toBuffer(), router);
						System.out.println("Sending Hi from Server");
					} else if (requestPayload.contains("GET") || requestPayload.contains("POST") ) {
						String responsePayload = processPayload(requestPayload).toString();  //converting strinBuilder to string

						Packet resp = packet.toBuilder().setPayload(responsePayload.getBytes()).create();
						channel.send(resp.toBuffer(), router);

					} else if (requestPayload.equals("Received")) {
						System.out.println(requestPayload);
						Packet respClose = packet.toBuilder().setPayload("Close".getBytes()).create();
						channel.send(respClose.toBuffer(), router);

					} else if (requestPayload.equals("Ok")) {

						System.out.println(requestPayload + " received..!");

					}
				}
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	private static StringBuilder processPayload(String requestPayload) throws Exception {
		// read request of client
		readRequest(requestPayload);
		// Processing the request
		if (is_Debug_On == true) {
			System.out.println("\n---------- Processing the request...\n");
			System.out.println("Type of the method : " + method);
		}

		processRequest();

		// making response for request
		if (is_Debug_On)
			System.out.println("\n---------- Making response...\n");
		makeResponse();
		return respond;
	}

	private static void readRequest(String requestPayload) throws Exception {

		if (is_Debug_On == true) {
			System.out.println(requestPayload);
		}
		if (requestPayload.equals("")) {
			return;
		}

		// deal with method
		if (requestPayload.toLowerCase().contains("get")) {
			method = "get";
			int path_start_index = requestPayload.indexOf("/"); // start searching "/" after "http://" in "GET
																// /files.txt
			// HTTP/1.0" (our received line) should return 4
			int index_of_space = requestPayload.indexOf(" ", path_start_index);
			file_Path = requestPayload.substring(path_start_index, index_of_space);
			System.out.println("FILEPATH: "+ file_Path);
		} else if (requestPayload.toLowerCase().contains("post")) {
			method = "post";
			int path_start_index = requestPayload.indexOf("/"); // start searching "/" after "http://" in "GET
																// /files.txt
			// HTTP/1.0" (our received line) should return 4
			int index_of_space = requestPayload.indexOf(" ", path_start_index);
			file_Path = requestPayload.substring(path_start_index, index_of_space);
		}

		if ("/".equals(file_Path.trim())) {
			file_Path = dir_Path;
			list_All_Files = true;
			System.out.println("\n\nHAVE TO PRINT TREE");
			return;
		}

		if ((method.equals("post")) && (requestPayload.contains("Content-Length"))) {

			if (requestPayload.contains("Overwrite")) {
				is_Overwrite = true;

			}
			data = requestPayload;
			if (is_Debug_On) {
				System.out.println("Storing the data from headers");
			}
		}

	}

	private static void processRequest() throws IOException, InterruptedException {

		if (200 != status_Code)
			return;

		int slash_count = 0;
		for (int i = 0; i < file_Path.length(); i++) {
			if (file_Path.charAt(i) == '/') {
				slash_count += 1;
			}
		}

		if (slash_count >= 2) {
			if (file_Path.length() > 3) {
				status_Code = 403;
				main_Response_Data.append(
						"Due to security reasons, It is not allowed to access other directories on this server!\r\n");
				return;
			}
		}
		if (list_All_Files == true) {
			listOfAllFiles(file_Path, main_Response_Data);
		} else {
			if (method.equals("get")) {
				File requested_file = new File(dir_Path + file_Path.trim());

				content_Type = requested_file.toURI().toURL().openConnection().getContentType();

				if (requested_file.getName().endsWith(".json")) {
					content_Type = "application/json";
				}

				if (content_Type.equals("text/plain")) {
					content_Disposition = "inline";
					// main_Response_Data.append("Content-Disposition: inline").append("\r\n");
				} else {
					content_Disposition = "attachment; filename=" + dir_Path + file_Path + ";";
					// main_Response_Data.append("Content-Disposition:
					// ").append(content_Disposition).append("\r\n");
				}
				if (is_Debug_On) {
					System.out.println("Content Disposition: " + content_Disposition);
					System.out.println("Content Type:        " + content_Type);

				}
				if (!content_Type.equals("text/plain") && !content_Type.equals("application/json")) {
					main_Response_Data.append("Cannot read ").append(content_Type).append(", type of file!");
				} else if (requested_file.exists() && requested_file.isFile()) {

					BufferedReader data_of_file = new BufferedReader(new FileReader(requested_file));
					String getLine;
					while (null != (getLine = data_of_file.readLine())) {
						main_Response_Data.append(getLine).append("\r\n");
					}
					data_of_file.close();
				} else {
					System.out.println("\nFile exist status: " + requested_file.exists());
					status_Code = 404;
				}
			} else {
				File requested_file = new File(dir_Path + file_Path.trim());
				PrintWriter out = null;
				if (requested_file.exists() && requested_file.isFile()) {
					if (is_Overwrite) {
						out = new PrintWriter(new FileOutputStream(requested_file, false));
					} else {
						out = new PrintWriter(new FileOutputStream(requested_file, true));
					}

				} else {
					out = new PrintWriter(requested_file);
				}
				out.append(data);
				out.append("\n");
				out.close();
			}
		}
	}

	private static void listOfAllFiles(String file_Path, StringBuilder respond_Body) {
		File current_directory = new File(file_Path);
		File[] filesList = current_directory.listFiles();
		if (null != filesList) {
			for (File file : filesList) {
				if (file.isFile()) {
					respond_Body.append(file.getName()).append("\r\n");
				}
			}
		}
	}
	
	private static void makeResponse() throws Exception {
		if (status_Code == 404) {
			respond.append("HTTP/1.1 404 NOT FOUND\r\n");
			main_Response_Data.append("The requested File was not found on the server.\r\n");
		} else if (status_Code == 403) {
			respond.append("HTTP/1.1 403 Forbidden\r\n");
		} else if (status_Code == 400) {
			respond.append("HTTP/1.1 400 Bad Request\r\n");
		} else {
			respond.append("HTTP/1.1 200 OK\r\n");
			if (method.equals("post"))
				main_Response_Data.append("Posted file successfully.");
		}

		respond.append("Connection: close\r\n");
		respond.append("Server: httpfs\n");
		respond.append("Date: ").append(Calendar.getInstance().getTime().toString()).append("\r\n");
		if(method.equals("get")) {
			respond.append("Content-Type: ").append(content_Type).append("\r\n");
			respond.append("Content-Disposition: ").append(content_Disposition).append("\r\n");
		}		
		respond.append("Content-Length: ").append(main_Response_Data.length()).append("\r\n");
		
		respond.append("\r\n");
		respond.append(main_Response_Data.toString());

	}

}
