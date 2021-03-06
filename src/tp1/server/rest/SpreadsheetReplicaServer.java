package tp1.server.rest;

import org.glassfish.jersey.jdkhttp.JdkHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;
import tp1.discovery.Discovery;
import tp1.kafka.sync.SyncPoint;
import tp1.resources.rest.SpreadsheetReplicatedResource;
import tp1.util.InsecureHostnameVerifier;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import java.net.InetAddress;
import java.net.URI;
import java.util.logging.Logger;

import static tp1.clients.sheet.SpreadsheetClient.SERVICE;
import static tp1.discovery.Discovery.DISCOVERY_PERIOD;

public class SpreadsheetReplicaServer {

    private static Logger Log = Logger.getLogger(UsersRestServer.class.getName());

    static {
        System.setProperty("java.net.preferIPv4Stack", "true");
        System.setProperty("java.util.logging.SimpleFormatter.format", "%4$s: %5$s\n");
    }

    public static final int PORT = 8080;

    public static void main(String[] args) {
        try {
            String domain = args.length > 0 ? args[0] : "domain0";

            String ip = InetAddress.getLocalHost().getHostAddress();

            HttpsURLConnection.setDefaultHostnameVerifier(new InsecureHostnameVerifier());

            SyncPoint sp = SyncPoint.getInstance();

            String serverURI = String.format("https://%s:%s/rest", ip, PORT);

            ResourceConfig config = new ResourceConfig();
            SpreadsheetReplicatedResource resource = new SpreadsheetReplicatedResource(domain, sp);
            config.register(resource);

            JdkHttpServerFactory.createHttpServer(URI.create(serverURI), config, SSLContext.getDefault());

            Discovery.init(domain, SERVICE ,serverURI);
            Discovery.startCollectingAnnouncements();

            while(Discovery.getLocalUsersClient() == null) {
                try {
                    Thread.sleep(DISCOVERY_PERIOD);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            Discovery.startSendingAnnouncements();

            resource.registerInKafka();

            Log.info(String.format("%s Server ready @ %s\n",  SERVICE, serverURI));
        } catch( Exception e) {
            Log.severe(e.getMessage());
        }
    }
}
