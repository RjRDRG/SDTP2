package tp1.resources;

import jakarta.inject.Singleton;
import jakarta.jws.WebService;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response.Status;
import tp1.api.User;
import tp1.api.service.rest.RestUsers;
import tp1.api.service.soap.SoapUsers;
import tp1.api.service.soap.UsersException;
import tp1.clients.sheet.SpreadsheetClient;
import tp1.clients.sheet.SpreadsheetRetryClient;
import tp1.discovery.Discovery;
import tp1.server.WebServiceType;

import java.net.URI;
import java.util.*;
import java.util.logging.Logger;

import static tp1.server.WebServiceType.SOAP;

@WebService(
		serviceName = SoapUsers.NAME,
		targetNamespace = SoapUsers.NAMESPACE,
		endpointInterface = SoapUsers.INTERFACE
)
@Singleton
public class UsersResource implements RestUsers, SoapUsers {

	private String domainId;

	private WebServiceType type;

	public static Discovery discovery;

	private final Map<String, User> users = new HashMap<>();

	private static Logger Log = Logger.getLogger(UsersResource.class.getName());

	public UsersResource(String domainId, WebServiceType type) {
		this.domainId = domainId;
		this.type = type;
	}




	public static void setDiscovery(Discovery discovery) {
		UsersResource.discovery = discovery;
	}

	private SpreadsheetClient cachedSpreadsheetClient;
	private SpreadsheetClient getLocalSpreadsheetClient() {

		if(cachedSpreadsheetClient == null) {
			String serverUrl = discovery.knownUrisOf(domainId, SpreadsheetClient.SERVICE).stream()
					.findAny()
					.map(URI::toString)
					.orElse(null);

			if(serverUrl != null) {
				try {
					cachedSpreadsheetClient = new SpreadsheetRetryClient(serverUrl);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}

		return cachedSpreadsheetClient;
	}

	public static void throwWebAppException(WebServiceType type, Status status) throws UsersException {
		if(type == SOAP)
			throw new UsersException(status.name());
		else
			throw new WebApplicationException(status);
	}




	@Override
	public String createUser(User user) throws UsersException {
		if(user.getUserId() == null || user.getPassword() == null || user.getFullName() == null || 
				user.getEmail() == null) {
			throwWebAppException(type, Status.BAD_REQUEST );
		}

		synchronized ( this ) {
			String userId = user.getUserId();

			if(users.containsKey(userId)) {
				throwWebAppException(type, Status.CONFLICT);
			}

			users.put(userId, user);

			return userId;
		}
	}


	@Override
	public User getUser(String userId, String password) throws UsersException {
		User user = users.get(userId);

		if( user == null ) {
			throwWebAppException(type, Status.NOT_FOUND );
		}

		if(!user.getPassword().equals(password)) {
			throwWebAppException(type, Status.FORBIDDEN );
		}

		return user;
	}


	@Override
	public User updateUser(String userId, String password, User user) throws UsersException {
		if(userId == null || password == null) {
			throwWebAppException(type, Status.BAD_REQUEST );
		}

		synchronized ( this ) {
			User oldUser = users.get(userId);

			if( oldUser == null ) {
				throwWebAppException(type, Status.NOT_FOUND );
			}

			if( !oldUser.getPassword().equals( password)) {
				throwWebAppException(type, Status.FORBIDDEN );
			}

			User newUser = new User(userId,
					user.getFullName() == null ? oldUser.getFullName() : user.getFullName(),
					user.getEmail() == null ? oldUser.getEmail() : user.getEmail(),
					user.getPassword() == null ? oldUser.getPassword() : user.getPassword());

			users.put(userId, newUser);

			return newUser;
		}
	}


	@Override
	public User deleteUser(String userId, String password) throws UsersException {
		if(userId == null ) {
			throwWebAppException(type, Status.BAD_REQUEST );
		}

		synchronized ( this ) {
			User user = users.get(userId);

			if( user == null ) {
				throwWebAppException(type, Status.NOT_FOUND );
			}

			if( !user.getPassword().equals( password)) {
				throwWebAppException(type, Status.FORBIDDEN );
			}

			try {
				getLocalSpreadsheetClient().deleteUserSpreadsheets(userId, password);
			} catch (Exception e) {
			}

			return users.remove(userId);
		}
	}


	@Override
	public List<User> searchUsers(String pattern) throws UsersException {

		if (users.isEmpty()) {
			return new ArrayList<>();
		}

		if (pattern == null || pattern.isEmpty()) {
			return new ArrayList<>(users.values());
		}

		synchronized (this) {
			List<User> result = new LinkedList<>();
			for (User u : users.values()) {
				if (u.getFullName().toLowerCase().contains(pattern.toLowerCase()))
					result.add(u);
			}

			return result;
		}
	}

}
