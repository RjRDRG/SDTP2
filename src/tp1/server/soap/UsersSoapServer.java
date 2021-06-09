package tp1.server.soap;

import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsServer;
import jakarta.xml.ws.Endpoint;
import tp1.discovery.Discovery;
import tp1.resources.rest.UsersRestResource;
import tp1.server.rest.UsersRestServer;
import tp1.util.InsecureHostnameVerifier;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

import static tp1.clients.user.UsersClient.SERVICE;

public class UsersSoapServer {

    private static Logger Log = Logger.getLogger(UsersRestServer.class.getName());

    static {
        System.setProperty("java.net.preferIPv4Stack", "true");
        System.setProperty("java.util.logging.SimpleFormatter.format", "%4$s: %5$s\n");
    }

    public static final int PORT = 8080;
    public static final String SOAP_USERS_PATH = "/soap/users";

    public static void main(String[] args) {
        try {
            String domain = args.length > 0 ? args[0] : "domain0";

            String ip = InetAddress.getLocalHost().getHostAddress();
            String serverURI = String.format("https://%s:%s/soap", ip, PORT);

            HttpsURLConnection.setDefaultHostnameVerifier(new InsecureHostnameVerifier());

            HttpsConfigurator configurator = new HttpsConfigurator(SSLContext.getDefault());

            HttpsServer server = HttpsServer.create(new InetSocketAddress(ip, PORT), 0);

            server.setHttpsConfigurator(configurator);

            server.setExecutor(Executors.newCachedThreadPool());

            Endpoint soapUsersEndpoint = Endpoint.create(new UsersRestResource(domain));

            soapUsersEndpoint.publish(server.createContext (SOAP_USERS_PATH));

            server.start();

            Discovery.init(  domain, SERVICE, serverURI);
            Discovery.startSendingAnnouncements();
            Discovery.startCollectingAnnouncements();

            Log.info(String.format("%s Server ready @ %s\n", SERVICE, serverURI));
        } catch( Exception e) {
            Log.severe(e.getMessage());
        }
    }
}
