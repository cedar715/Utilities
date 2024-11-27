import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class TcpClient {

    public static void main(String[] args) {
        String host = "localhost"; // Change to the server's IP if needed
        int port = 5141;           // The port your server is listening on

        try (Socket socket = new Socket(host, port);
             OutputStream out = socket.getOutputStream()) {

            // Send three messages one after another
            sendMessage(out, "Message 1: Hello, Spring Integration!");
            sendMessage(out, "Message 2: Sending another message");
            sendMessage(out, "Message 3: Final message for the server");

            // Optional: Pause for debugging to ensure all messages are sent before the program exits
            Thread.sleep(1000); 

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void sendMessage(OutputStream out, String message) throws Exception {
        // Convert the message to bytes and send
        byte[] rawBytes = (message + "\n").getBytes(StandardCharsets.UTF_8); // Appending \n as delimiter
        out.write(rawBytes); // Write the message
        out.flush();         // Ensure the message is sent immediately
        System.out.println("Sent: " + message);
    }
}
