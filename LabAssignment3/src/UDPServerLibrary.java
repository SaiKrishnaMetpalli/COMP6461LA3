import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.DatagramChannel;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class UDPServerLibrary {
	private String[] cmd_Arguments;
	private int port_No;
	private boolean print_Debug_Message;
	private String directory_Path;


	static File currentDir;
	static int timeout = 3000;
	static int port = 8080;
	List<String> clientRequestList;

	public UDPServerLibrary(String[] arguments) {
		// DEFAULT VALUES FOR SERVER
		cmd_Arguments = arguments;
		port_No = 8080;
		print_Debug_Message = false;
		directory_Path = ".";
	}

	public void handleCommand() throws IOException {
		boolean is_Continue = true;
		for (int i = 0; i < cmd_Arguments.length; i++) {
			switch (cmd_Arguments[i]) {
			case "help":
				StringBuilder httpfs_Help_Txt = new StringBuilder("\nhttpfs is a simple file server.\n\n");
				httpfs_Help_Txt.append("usage: httpfs [-v] [-p PORT] [-d PATH-TO-DIR]\n\n");
				httpfs_Help_Txt.append("    -v Prints debugging messages.\n");
				httpfs_Help_Txt.append("    -p Specifies the port number that the server will listen and serve at.\n");
				httpfs_Help_Txt.append("       Default is 8080.\n");
				httpfs_Help_Txt.append("    -d Specifies the directory that the server will use to read/write\n");
				httpfs_Help_Txt.append("       requested files. Default is the current directory when launching the\n");
				httpfs_Help_Txt.append("       application.\n\n");
				System.out.println(httpfs_Help_Txt.toString());
				is_Continue = true;
				break;
			case "-v":
				print_Debug_Message = true;
				is_Continue = true;
				break;
			case "-p":
				if (++i < cmd_Arguments.length) {
					try {
						port_No = Integer.parseInt(cmd_Arguments[i]);
						is_Continue = true;
					} catch (Exception exception) {
						System.out.println(cmd_Arguments[i] + " port number has to be an integer");
						is_Continue = false;
					}
				} else {
					System.out.println("\n==========Port number not found");
					is_Continue = false;
				}
				break;
			case "-d":
				if (++i < cmd_Arguments.length) {
					directory_Path = cmd_Arguments[i];
					is_Continue = true;
				} else {
					System.out.println("\n==========Directory path not found");
					is_Continue = false;
				}
				break;
			default:
				System.out.println(cmd_Arguments[i] + " command not found");
				is_Continue = false;
				break;
			}
		}

		if (is_Continue) {

			//here, now if the server starting command is valid than we are going to initiate the datagram channel through thread
			Runnable dataTransmission = () -> {
				try {
					ClientRequestHandler crh = new ClientRequestHandler(print_Debug_Message, directory_Path);
					crh.handleClientRequest(port);
				} catch (Exception e) {
					e.printStackTrace();
				}
			};
			Thread thread = new Thread(dataTransmission);
			thread.start();

		}

	}
}
