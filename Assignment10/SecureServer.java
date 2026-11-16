import javax.net.ssl.*;
import java.io.*;
import java.security.KeyStore;

public class SecureServer {
    public static void main(String[] args) {
        int port = 8443;
        String keystoreFile = "server.keystore";
        String keystorePassword = "password";

        try {
            
            KeyStore keyStore = KeyStore.getInstance("JKS");
            keyStore.load(new FileInputStream(keystoreFile), keystorePassword.toCharArray());

            
            KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance("SunX509");
            keyManagerFactory.init(keyStore, keystorePassword.toCharArray());

            
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(keyManagerFactory.getKeyManagers(), null, null);

            
            SSLServerSocketFactory sslServerSocketFactory = sslContext.getServerSocketFactory();
            SSLServerSocket sslServerSocket = (SSLServerSocket) sslServerSocketFactory.createServerSocket(port);

            System.out.println("Secure server started. Waiting for client connection...");

            
            SSLSocket sslSocket = (SSLSocket) sslServerSocket.accept();

            
            BufferedReader in = new BufferedReader(new InputStreamReader(sslSocket.getInputStream()));
            PrintWriter out = new PrintWriter(sslSocket.getOutputStream(), true);

            
            String clientMessage = in.readLine();
            System.out.println("Client says: " + clientMessage);
            out.println("Hello, client!");

            
            in.close();
            out.close();
            sslSocket.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
