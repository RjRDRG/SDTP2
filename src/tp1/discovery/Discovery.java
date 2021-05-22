package tp1.discovery;

import java.io.IOException;
import java.net.*;
import java.util.*;
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
	static final int DISCOVERY_PERIOD = 1000;
	static final int DISCOVERY_TIMEOUT = 5000;

	private static final String URI_DELIMITER = "\t";
	private static final String DOMAIN_DELIMITER = ":";

	private InetSocketAddress addr;
	private String domainId;
	private String serviceName;
	private String serviceURI;
	private Map<String, Set<URI>> servers;
	private Map<String, Long> timeStamps;
	private MulticastSocket ms;

	/**
	 * @param  serviceName the name of the service to announce
	 * @param  serviceURI an uri string - representing the contact endpoint of the service being announced
	 */
	public Discovery(InetSocketAddress addr, String domainId, String serviceName, String serviceURI) {
		this.addr = addr;
		this.domainId = domainId;
		this.serviceName = serviceName;
		this.serviceURI  = serviceURI;
		this.servers = new HashMap<String, Set<URI>>();
		this.timeStamps = new HashMap<String, Long>();
		this.ms = null;
	}

	/**
	 * @param  serviceName the name of the service to announce
	 * @param  serviceURI an uri string - representing the contact endpoint of the service being announced
	 */
	public Discovery(String domainId, String serviceName, String serviceURI) {
		this(DISCOVERY_ADDR, domainId, serviceName, serviceURI);
	}
	
	/**
	 * Starts sending service announcements at regular intervals... 
	 */
	public void startSendingAnnouncements() {
		Log.info(String.format("Starting Discovery announcements on: %s for: %s -> %s\n", addr, serviceName, serviceURI));

		byte[] announceBytes = (domainId+ DOMAIN_DELIMITER +serviceName+ URI_DELIMITER +serviceURI).getBytes();
		DatagramPacket announcePkt = new DatagramPacket(announceBytes, announceBytes.length, addr);

		try {
			if(ms == null) {
				ms = new MulticastSocket(addr.getPort());
				ms.joinGroup(addr, NetworkInterface.getByInetAddress(InetAddress.getLocalHost()));
			}

			// start thread to send periodic announcements
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
	public void startCollectingAnnouncements() {
		try {
			if(ms == null) {
				ms = new MulticastSocket(addr.getPort());
				ms.joinGroup(addr, NetworkInterface.getByInetAddress(InetAddress.getLocalHost()));
			}

			// start thread to collect announcements
			new Thread(() -> {
				DatagramPacket pkt = new DatagramPacket(new byte[1024], 1024);
				for (;;) {
					try {
						pkt.setLength(1024);
						ms.receive(pkt);

						String msg = new String( pkt.getData(), 0, pkt.getLength());
						String[] msgElems = msg.split(URI_DELIMITER);

						if( msgElems.length == 2) {	//periodic announcement

							String sn = msgElems[0], su = msgElems[1];

							if (!servers.containsKey(sn))
								servers.put(sn, new HashSet<URI>());

							servers.get(sn).add(URI.create(su));
							timeStamps.put(sn, System.currentTimeMillis());

						}
					} catch (IOException e) {
						// do nothing
					}
				}
			}).start();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Returns the known servers for a service.
	 * 
	 * @param  domain the domain of the service being discovered
	 * @param  service the name of the service being discovered
	 * @return an array of URI with the service instances discovered.
	 * 
	 */
	public Set<URI> knownUrisOf(String domain, String service) {
		return servers.get(domain+DOMAIN_DELIMITER+service);
	}
}
