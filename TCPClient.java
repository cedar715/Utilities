import java.io.OutputStream;
import java.net.Socket;

public class TcpClient {
    public static void main(String[] args) {
        String host = "127.0.0.1"; // Replace with the server's IP address if needed
        int port = 5141;
        String message = "Hello, Spring Boot!";

        try (Socket socket = new Socket(host, port);
             OutputStream out = socket.getOutputStream()) {
            out.write(message.getBytes());
            out.flush();
            System.out.println("Message sent: " + message);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
