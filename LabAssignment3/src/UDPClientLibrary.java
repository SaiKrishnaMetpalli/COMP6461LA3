import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import static java.nio.channels.SelectionKey.OP_READ;
import java.nio.channels.Selector;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class UDPClientLibrary {
	private String[] cmd_Arguments;
	private String router_Host="localhost";
	private int router_Port=3000;
	private int sequence_No=0;
	private int timeout = 3000;
	private int ack_Count = 0;
	static List<Long> receivedPackets = new ArrayList<>();

	public UDPClientLibrary(String[] arguments) {
		cmd_Arguments = arguments;
	}

	public void handleCommand() throws IOException {
		if (cmd_Arguments.length == 1 && cmd_Arguments[0].equals("help")) {
			String help_Text = "\n";
			help_Text += "httpc is a curl-like application but supports HTTP protocol only.\n";
			help_Text += "Usage: \n    httpc command [arguments]\nThe commands are:\n";
			help_Text += "    get     executes a HTTP GET request and prints the response.\n";
			help_Text += "    post    executes a HTTP POST request and prints the response.\n";
			help_Text += "    help    prints this screen.\n\n";
			help_Text += "Use \"httpc help [command]\" for more information about a command.\n";
			System.out.println(help_Text);
		} else if (cmd_Arguments.length == 1) {
			System.out.println("\n==========Invalid command");
		} else {
			switch (cmd_Arguments[0]) {
			case "help":
				switch (cmd_Arguments[1]) {
				case "get":
					String get_Help_Text = "\n";
					get_Help_Text += "usage: httpc get [-v] [-h key:value] URL\n\n";
					get_Help_Text += "Get executes a HTTP GET request for a given URL.\n\n";
					get_Help_Text += "    -v           Prints the detail of the response such as protocol, status, and headers.\n";
					get_Help_Text += "    -h key:value Associates headers to HTTP Request with the format 'key:value'.\n";
					System.out.println(get_Help_Text);
					break;
				case "post":
					String post_Help_Txt = "\n";
					post_Help_Txt += "usage: httpc post [-v] [-h key:value] [-d inline-data] [-f file] URL\n\n";
					post_Help_Txt += "Post executes a HTTP POST request for a given URL with inline data or from file.\n\n";
					post_Help_Txt += "    -v             Prints the detail of the response such as protocol, status, and headers.\n";
					post_Help_Txt += "    -h key:value   Associates headers to HTTP Request with the format 'key:value'.\n";
					post_Help_Txt += "    -d string      Associates an inline data to the body HTTP POST request.\n";
					post_Help_Txt += "    -f file        Associates the content of a file to the body HTTP POST request.\n\n";
					post_Help_Txt += "Either [-d] or [-f] can be used but not both.\n";
					System.out.println(post_Help_Txt);
					break;
				default:
					System.out.println("\n==========" + cmd_Arguments[1] + " command not found");
				}
				break;
			case "get":
				handleGetRequest();
				break;
			case "post":
				handlePostRequest();
				break;
			default:
				System.out.println("\n==========" + cmd_Arguments[0] + " command not found");
			}
		}		
		
	}
	
	
	private void handlePostRequest() throws IOException {
		boolean is_Verbose = false;
		boolean is_Header_Data = true;
		boolean is_Proceed = true;
		boolean is_contains_d = false;
		boolean is_contains_f = false;
		boolean is_Save_File = false;
		boolean is_Overwrite = false;
		String message = "";
		String host = "";
		String path = "/";
		int port_No=80;
		String data = "";
		String filename = "";
		String filename_Save = "";
		File file = null;
		String url = "";
		String output_Data = "";
		ArrayList<String> header_List = new ArrayList<String>();
		String boundry = "LA1";

		for (int i = 1; i < cmd_Arguments.length; i++) {
			if (cmd_Arguments[i].equals("-v")) {
				is_Verbose = true;
			} else if (cmd_Arguments[i].equals("-h")) {
				i++;
				if (cmd_Arguments[i].contains("://")) {
					is_Proceed = false;
					message = "\n==========The header is missing Key:Value pair ";
					break;
				} else if (cmd_Arguments[i].contains(":")) {
					header_List.add(cmd_Arguments[i].toString().trim());

				} else {
					is_Proceed = false;
					message = "\n==========The header: " + cmd_Arguments[i] + " is not in Key:Value pair ";
					break;
				}
			} else if ((cmd_Arguments[i].contains("-d")) || (cmd_Arguments[i].contains("--d"))) {
				if (is_contains_f) {
					message = "\n==========The post command cannot have both -d and -f in it.";
					is_Proceed = false;
					break;
				} else if (is_contains_d) {
					message = "\n==========The post command cannot have -d command multiple times.";
					is_Proceed = false;
					break;
				} else {
					is_contains_d = true;
					i++;
					data = cmd_Arguments[i];
				}
			} else if ((cmd_Arguments[i].contains("-f")) || (cmd_Arguments[i].contains("--f"))) {
				if (is_contains_d) {
					message = "\n==========The post command cannot have both -d and -f in it.";
					is_Proceed = false;
					break;
				} else if (is_contains_f) {
					message = "\n==========The post command cannot have -f command multiple times.";
					is_Proceed = false;
					break;
				} else {
					is_contains_f = true;
					i++;
					filename = cmd_Arguments[i].toString().trim();
					file = new File(filename);
					String line;
					String file_Content="";
					if (file.exists()) {
						BufferedReader bReader = new BufferedReader(new FileReader(file));
						try {
							
							while ((line = bReader.readLine()) != null) {
								file_Content += line;
							}
							
							data += "--" + boundry + "\r\n";
                            data += "Content-Disposition: form-data; name=\"file\"; filename=" + filename + "\r\n";
                            data += "Content-Type: text/plain" + "\r\n";
                            data += "Content-Length:" + file_Content.length() + "\r\n";
                            data += "\r\n";
                            data += file_Content + "\r\n";
                            data += "--" + boundry + "--" + "\r\n";
                            

						} catch (IOException e) {
							e.printStackTrace();
						}
						bReader.close();
					} else {
						is_Proceed = false;
						message = "\n==========File does not exists!";
						break;
					}

				}

			} else if (cmd_Arguments[i].contains("-o")) {
				is_Save_File = true;
				i++;
				if (cmd_Arguments[i].endsWith(".txt")) {
					filename_Save = cmd_Arguments[i];
				} else {
					is_Proceed = false;
					message = "\n==========The given filename: " + cmd_Arguments[i] + " should end with .txt";
					break;
				}
			} else if (cmd_Arguments[i].contains("http")) {
				url = cmd_Arguments[i];
				if (validateURL(url)) {
					int index_port=url.indexOf(":", 7);
					int index_path = url.indexOf("/", 7);					
					host = "localhost";
					try {
						port_No=Integer.parseInt(url.substring(index_port+1,index_path));
					} catch(Exception ex) {
						is_Proceed = false;
						message = "\n==========Port Number is not valid";
						break;						
					}
					path = url.substring(index_path);					
				} else {
					is_Proceed = false;
					message = "\n==========The URL provided is invalid";
					break;
				}
			} else if ((cmd_Arguments[i].contains("-r"))) {
				i++;				
				if(cmd_Arguments[i].equals("true")) {
					is_Overwrite=true;
				} else {
					is_Overwrite=false;
				}				
			} else {
				is_Proceed = false;

				message = "\n==========The POST command format is not correct";
			}
		}

		if (is_Proceed) {
			try {
				SocketAddress router_Socket = new InetSocketAddress(router_Host, router_Port);
				InetSocketAddress server_Socket = new InetSocketAddress(host, port_No);

				startHandshake(router_Socket, server_Socket);
				String msg_Send="";
				msg_Send+="POST " + path + " HTTP/1.0\\r\\n";
				msg_Send+="Host:" + host + "\r\n";
				msg_Send+="User-Agent:Concordia-HTTP/1.0\r\n";
				if (!header_List.isEmpty()) {
					for (String header : header_List) {
						msg_Send+=header + "\r\n";
					}
				}
				
				if (!(data.isEmpty())) {
					msg_Send+="Content-Length: " + data.length() + "\r\n";					
				}
				msg_Send+=data+"\r\n";
				processRequest(router_Socket, server_Socket, msg_Send);
				
			} catch (Exception ex) {
				ex.printStackTrace();
			}

		} else {
			System.out.println(message);
		}

	}

	public void handleGetRequest() throws IOException {
		boolean is_Verbose = false;
		boolean is_Proceed = true;
		boolean is_Header_Data = true;
		boolean is_Save_File = false;
		String message = "";
		String host = "";
		String path = "/";
		String url = "";
		int port_No=80;
		String filename_Save = "";
		String output_Data = "";
		boolean is_redirect = false;
		ArrayList<String> header_List = new ArrayList<String>();
		for (int i = 1; i < cmd_Arguments.length; i++) {
			if (cmd_Arguments[i].equals("-v")) {
				is_Verbose = true;
			} else if (cmd_Arguments[i].equals("-h")) {
				i++;
				if (cmd_Arguments[i].contains("://")) {
					is_Proceed = false;
					message = "\n==========The header is missing Key:Value pair";
					break;
				} else if (cmd_Arguments[i].contains(":")) {
					header_List.add(cmd_Arguments[i]);
				} else {
					is_Proceed = false;
					message = "\n==========The header: " + cmd_Arguments[i] + " is not in Key:Value pair";
					break;
				}
			} else if (cmd_Arguments[i].contains("-o")) {
				is_Save_File = true;
				i++;
				if (cmd_Arguments[i].endsWith(".txt")) {
					filename_Save = cmd_Arguments[i];
				} else {
					is_Proceed = false;
					message = "\n==========The given filename: " + cmd_Arguments[i] + " should end with .txt";
					break;
				}
			} else if (cmd_Arguments[i].contains("http")) {
				url = cmd_Arguments[i];
				if (validateURL(url)) {
					int index_port=url.indexOf(":", 7);
					int index_path = url.indexOf("/", 7);					
					host = "localhost";
					try {
						port_No=Integer.parseInt(url.substring(index_port+1,index_path));
					} catch(Exception ex) {
						is_Proceed = false;
						message = "\n==========Port Number is not valid";
						break;
					}
					path = url.substring(index_path);					
				} else {
					is_Proceed = false;
					message = "\n==========The URL provided is invalid";
					break;
				}
			} else {
				is_Proceed = false;
				message = "\n==========The get command format is invalid";
				break;
			}
		}
		
		if (is_Proceed) {
			try {
				SocketAddress router_Socket = new InetSocketAddress(router_Host, router_Port);
				InetSocketAddress server_Socket = new InetSocketAddress(host, port_No);

				startHandshake(router_Socket, server_Socket);
				String msg_Send="";
				msg_Send+="GET " + path + " HTTP/1.0\r\n";
				msg_Send+="Host:" + host + "\r\n";
				msg_Send+="User-Agent:Concordia-HTTP/1.0\r\n";
				if (!header_List.isEmpty()) {
					for (String header : header_List) {
						msg_Send+=header + "\r\n";
					}
				}
				processRequest(router_Socket, server_Socket, msg_Send);
				
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		} else {
			System.out.println(message);
		}		
	}
	
	private void processRequest(SocketAddress router_Socket, InetSocketAddress server_Socket, String msg_Send) throws IOException {
		try (DatagramChannel dt_Channel = DatagramChannel.open()) {
			sequence_No++;
			Packet p_Send = new Packet.Builder().setType(0).setSequenceNumber(sequence_No)
					.setPortNumber(server_Socket.getPort()).setPeerAddress(server_Socket.getAddress())
					.setPayload(msg_Send.getBytes()).create();
			dt_Channel.send(p_Send.toBuffer(), router_Socket);
			System.out.println("requesting router for communication...\n");

			
			dt_Channel.configureBlocking(false);
			Selector selector = Selector.open();
			dt_Channel.register(selector, OP_READ);
			selector.select(timeout);

			Set<SelectionKey> keys = selector.selectedKeys();
			if (keys.isEmpty()) {
				System.out.println("Timeout...........\nRetrying sending packets..........\n");
				resend(dt_Channel, p_Send, router_Socket);
			}
			
			ByteBuffer buf = ByteBuffer.allocate(Packet.MAX_LEN);
			SocketAddress router = dt_Channel.receive(buf);
			buf.flip();
			Packet resp = Packet.fromBuffer(buf);
			String payload = new String(resp.getPayload(), StandardCharsets.UTF_8);

			if (!receivedPackets.contains(resp.getSequenceNumber())) {
				receivedPackets.add(resp.getSequenceNumber());
				System.out.println(payload);				
				sequence_No++;
				Packet p_Ack = new Packet.Builder().setType(0).setSequenceNumber(sequence_No)
						.setPortNumber(server_Socket.getPort()).setPeerAddress(server_Socket.getAddress())
						.setPayload("data received successfully".getBytes()).create();
				dt_Channel.send(p_Ack.toBuffer(), router_Socket);

				dt_Channel.configureBlocking(false);
				selector = Selector.open();
				dt_Channel.register(selector, OP_READ);
				selector.select(timeout);

				keys = selector.selectedKeys();
				if (keys.isEmpty()) {
					resend(dt_Channel, p_Ack, router);
				}

				buf.flip();

				System.out.println("Terminating the connnection from server...");
				keys.clear();

				sequence_No++;
				Packet p_Close = new Packet.Builder().setType(0).setSequenceNumber(sequence_No)
						.setPortNumber(server_Socket.getPort()).setPeerAddress(server_Socket.getAddress())
						.setPayload("Done".getBytes()).create();
				dt_Channel.send(p_Close.toBuffer(), router_Socket);
				System.out.println("Confirmation sent to server.");
			}
		}
		
	}

	private void startHandshake(SocketAddress router_Socket, InetSocketAddress server_Socket) throws IOException {
		try (DatagramChannel dt_Channel = DatagramChannel.open()) {
			String msg = "Hello from client side";
			sequence_No++;
			// SYN
			Packet p_handshake = new Packet.Builder().setType(0).setSequenceNumber(sequence_No)
					.setPortNumber(server_Socket.getPort()).setPeerAddress(server_Socket.getAddress())
					.setPayload(msg.getBytes()).create();
			dt_Channel.send(p_handshake.toBuffer(), router_Socket);
			System.out.println("Sending Hello from client side");

			dt_Channel.configureBlocking(false);
			Selector selector = Selector.open();
			dt_Channel.register(selector, OP_READ);

			selector.select(timeout);

			Set<SelectionKey> keys = selector.selectedKeys();
			if (keys.isEmpty()) {
				System.out.println("Timeout...........\nRetrying sending packets..........\\n\"");
				resend(dt_Channel, p_handshake, router_Socket);
			}

			ByteBuffer buf = ByteBuffer.allocate(Packet.MAX_LEN);
//			buf.flip();
			Packet resp = Packet.fromBuffer(buf);
			String payload = new String(resp.getPayload(), StandardCharsets.UTF_8);
			System.out.println("Response from server received.........\n");
			receivedPackets.add(resp.getSequenceNumber());
			keys.clear();

		}
		
	}

	private void resend(DatagramChannel dt_Channel, Packet pkt, SocketAddress router_Socket) throws IOException {
		dt_Channel.send(pkt.toBuffer(), router_Socket);
		String payload = new String(pkt.getPayload(), StandardCharsets.UTF_8);
		System.out.println(payload);
		if (payload.equals("Received")) {
			ack_Count++;
		}

		dt_Channel.configureBlocking(false);
		Selector selector = Selector.open();
		dt_Channel.register(selector, OP_READ);
		selector.select(timeout);

		Set<SelectionKey> keys = selector.selectedKeys();
		if (keys.isEmpty() && ack_Count < 10) {
			System.out.println("Timeout...........\nRetrying sending packets..........\\n\"");
			resend(dt_Channel, pkt, router_Socket);

		} else {
			return;
		}
		
	}

	private boolean validateURL(String url) {
		return url.substring(0, 16).equals("http://localhost");
	}
}
