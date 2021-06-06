package tp1.server.proxy;

import com.google.gson.Gson;
import org.glassfish.jersey.jdkhttp.JdkHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;
import tp1.api.Spreadsheet;
import tp1.clients.sheet.SpreadsheetDropboxClient;
import tp1.discovery.Discovery;
import tp1.resources.SpreadsheetProxyResource;
import tp1.resources.SpreadsheetResource;
import tp1.server.WebServiceType;
import tp1.server.rest.UsersRestServer;
import tp1.util.InsecureHostnameVerifier;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import java.net.InetAddress;
import java.net.URI;
import java.util.logging.Logger;

import static tp1.clients.sheet.SpreadsheetClient.SERVICE;

public class SpreadsheetDropboxServer {

    private static Logger Log = Logger.getLogger(UsersRestServer.class.getName());

    static {
        System.setProperty("java.net.preferIPv4Stack", "true");
        System.setProperty("java.util.logging.SimpleFormatter.format", "%4$s: %5$s\n");
    }

    public static final int PORT = 8080;

    public static void main(String[] args) {
        try {
            String domain = args.length > 0 ? args[0] : "domain0";
            int port = args.length > 1 ? Integer.parseInt(args[1]) : PORT;

            String ip = InetAddress.getLocalHost().getHostAddress();

            HttpsURLConnection.setDefaultHostnameVerifier(new InsecureHostnameVerifier());

            ResourceConfig config = new ResourceConfig();
            config.register(new SpreadsheetProxyResource(domain, new SpreadsheetDropboxClient()));

            String serverURI = String.format("http://%s:%s/rest", ip, port);
            JdkHttpServerFactory.createHttpServer(URI.create(serverURI), config);

            Discovery.init( domain, SERVICE ,serverURI);
            Discovery.startSendingAnnouncements();
            Discovery.startCollectingAnnouncements();

            Log.info(String.format("%s Server ready @ %s\n",  SERVICE, serverURI));

            //More code can be executed here...
        } catch( Exception e) {
            Log.severe(e.getMessage());
        }
    }
}