import java.io.IOException;

public class httpfs {

	public static void main(String[] args) throws IOException {
		// TODO Auto-generated method stub
		UDPServerLibrary udp_Server = new UDPServerLibrary(args);
		udp_Server.handleCommand();
	}

}
