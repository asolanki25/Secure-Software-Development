import javax.net.ssl.*;
import java.io.*;
import java.security.KeyStore;

public class SecureClient {
    public static void main(String[] args) {
        String host = "localhost";
        int port = 8443;
        String truststoreFile = "client.truststore";
        String truststorePassword = "password";

        try {
            
            KeyStore trustStore = KeyStore.getInstance("JKS");
            trustStore.load(new FileInputStream(truststoreFile), truststorePassword.toCharArray());

            
            TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance("SunX509");
            trustManagerFactory.init(trustStore);

            
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustManagerFactory.getTrustManagers(), null);

            
            SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();
            SSLSocket sslSocket = (SSLSocket) sslSocketFactory.createSocket(host, port);

            
            BufferedReader in = new BufferedReader(new InputStreamReader(sslSocket.getInputStream()));
            PrintWriter out = new PrintWriter(sslSocket.getOutputStream(), true);

            
            out.println("Hello, server!");

            
            String serverMessage = in.readLine();
            System.out.println("Server says: " + serverMessage);

            
            in.close();
            out.close();
            sslSocket.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}