package tp1.discovery;

import tp1.clients.sheet.SpreadsheetClient;
import tp1.clients.sheet.SpreadsheetMultiClient;
import tp1.clients.sheet.SpreadsheetRetryClient;
import tp1.clients.user.UsersClient;
import tp1.clients.user.UsersRetryClient;

import java.net.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * <p>A class to perform service discovery, based on periodic service contact endpoint 
 * announcements over multicast communication.</p>
 * 
 * <p>Servers announce their *name* and contact *uri* at regular intervals. The server actively
 * collects received announcements.</p>
 * 
 * <p>Service announcements have the following format:</p>
 * 
 * <p>&lt;service-name-string&gt;&lt;delimiter-char&gt;&lt;service-uri-string&gt;</p>
 */
public class Discovery {
	private static Logger Log = Logger.getLogger(Discovery.class.getName());

	static {
		// addresses some multicast issues on some TCP/IP stacks
		System.setProperty("java.net.preferIPv4Stack", "true");
		// summarizes the logging format
		System.setProperty("java.util.logging.SimpleFormatter.format", "%4$s: %5$s");
	}
	
	
	// The pre-aggreed multicast endpoint assigned to perform discovery. 
	static final InetSocketAddress DISCOVERY_ADDR = new InetSocketAddress("226.226.226.226", 2266);
	public static final int DISCOVERY_PERIOD = 1000;
	static final int DISCOVERY_TIMEOUT = 5000;

	private static final String URI_DELIMITER = "\t";
	private static final String DOMAIN_DELIMITER = ":";

	private static InetSocketAddress addr;
	private static String domainId;
	private static String serviceName;
	private static String serviceURI;
	private static ConcurrentHashMap<String, UsersClient> clientUserServer;
	private static ConcurrentHashMap<String, ConcurrentHashMap<String, SpreadsheetClient>> clientSheetsServer;

	private static MulticastSocket ms;

	/**
	 * @param  serviceName the name of the service to announce
	 * @param  serviceURI an uri string - representing the contact endpoint of the service being announced
	 */
	public static void init(InetSocketAddress addr, String domainId, String serviceName, String serviceURI) throws Exception {
		Discovery.addr = addr;
		Discovery.domainId = domainId;
		Discovery.serviceName = serviceName;
		Discovery.serviceURI  = serviceURI;
		Discovery.clientUserServer = new ConcurrentHashMap<>();
		Discovery.clientSheetsServer = new ConcurrentHashMap<>();
		Discovery.ms = new MulticastSocket(addr.getPort());
		Discovery.ms.joinGroup(addr, NetworkInterface.getByInetAddress(InetAddress.getLocalHost()));
	}

	/**
	 * @param  serviceName the name of the service to announce
	 * @param  serviceURI an uri string - representing the contact endpoint of the service being announced
	 */
	public static void init(String domainId, String serviceName, String serviceURI) throws Exception {
		init(DISCOVERY_ADDR, domainId, serviceName, serviceURI);
	}

	public static String getServiceURI() {
		return serviceURI;
	}

	/**
	 * Starts sending service announcements at regular intervals... 
	 */
	public static void startSendingAnnouncements() {
		Log.info(String.format("Starting Discovery announcements on: %s for: %s -> %s\n", addr, serviceName, serviceURI));
		byte[] announceBytes = (domainId+ DOMAIN_DELIMITER +serviceName+ URI_DELIMITER +serviceURI).getBytes();
		DatagramPacket announcePkt = new DatagramPacket(announceBytes, announceBytes.length, addr);

		try {
			new Thread(() -> {
				for (;;) {
					try {
						ms.send(announcePkt);
						Thread.sleep(DISCOVERY_PERIOD);
					} catch (Exception e) {
						e.printStackTrace();
						// do nothing
					}
				}
			}).start();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Starts collecting service announcements at regular intervals...
	 */
	public static void startCollectingAnnouncements() {
		try {
			new Thread(() -> {
				DatagramPacket pkt = new DatagramPacket(new byte[1024], 1024);
				for (;;) {
					try {
						pkt.setLength(1024);
						ms.receive(pkt);

						String msg = new String( pkt.getData(), 0, pkt.getLength());
						String[] msgElems = msg.split(URI_DELIMITER);

						if( msgElems.length == 2) {

							String domain = msgElems[0].split(DOMAIN_DELIMITER)[0];
							String service = msgElems[0].split(DOMAIN_DELIMITER)[1];
							String uri = msgElems[1];

							if(service.equals(SpreadsheetClient.SERVICE)) {
								if (!clientSheetsServer.containsKey(domain))
									clientSheetsServer.put(domain, new ConcurrentHashMap<>());

								if(!clientSheetsServer.get(domain).containsKey(uri))
									clientSheetsServer.get(domain).put(uri, new SpreadsheetRetryClient(uri, domain));
							}
							else if(service.equals(UsersClient.SERVICE)) {
								if (!clientUserServer.containsKey(domain))
									clientUserServer.put(domain, new UsersRetryClient(uri));
							}
						}
					} catch (Exception ignored) {}
				}
			}).start();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}


	public static UsersClient getLocalUsersClient() {
		return clientUserServer.get(Discovery.domainId);
	}

	public static SpreadsheetMultiClient getLocalSpreadsheetClients() {
		return new SpreadsheetMultiClient(clientSheetsServer.get(Discovery.domainId), Discovery.domainId);
	}

	public static SpreadsheetMultiClient getRemoteSpreadsheetClients(String domainId) {
		return new SpreadsheetMultiClient(clientSheetsServer.get(domainId), domainId);
	}

	public static void removeSpreadsheetClient(String domain, String uri) {
		if(clientSheetsServer.containsKey(domain))
			clientSheetsServer.get(domain).remove(uri);
	}
}
