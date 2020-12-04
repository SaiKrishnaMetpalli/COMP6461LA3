import java.io.IOException;

public class httpc {

	public static void main(String[] args) throws IOException {
		// TODO Auto-generated method stub
		if (args.length != 0) {
			UDPClientLibrary udp_Lib = new UDPClientLibrary(args);
			udp_Lib.handleCommand();
		} else {
			System.out.println("\n==========Invalid Command");
		}
	}

}
