package tp1.resources.soap;

import jakarta.jws.WebService;
import tp1.api.User;
import tp1.api.service.soap.SoapUsers;
import tp1.api.service.soap.UsersException;
import tp1.api.service.util.Result;
import tp1.impl.UsersImpl;

import java.util.List;

@WebService(
		serviceName = SoapUsers.NAME,
		targetNamespace = SoapUsers.NAMESPACE,
		endpointInterface = SoapUsers.INTERFACE
)
public class UsersSoapResource implements SoapUsers {

	String domainId;
	UsersImpl impl;

	public UsersSoapResource(String domainId) {
		this.domainId = domainId;
		this.impl = new UsersImpl();
	}

	@Override
	public String createUser(User user) throws UsersException {
		Result<String> result = impl.createUser(user);
		if(!result.isOK())
			throw new UsersException(result.error().name());
		else
			return result.value();
	}


	@Override
	public User getUser(String userId, String password) throws UsersException {
		Result<User> result = impl.getUser(userId, password);
		if(!result.isOK())
			throw new UsersException(result.error().name());
		else
			return result.value();
	}


	@Override
	public User updateUser(String userId, String password, User user) throws UsersException {
		Result<User> result = impl.updateUser(userId, password, user);
		if(!result.isOK())
			throw new UsersException(result.error().name());
		else
			return result.value();
	}

	@Override
	public User deleteUser(String userId, String password) throws UsersException {
		Result<User> result = impl.deleteUser(userId, password);
		if(!result.isOK())
			throw new UsersException(result.error().name());
		else
			return result.value();
	}

	@Override
	public List<User> searchUsers(String pattern) throws UsersException {
		Result<List<User>> result = impl.searchUsers(pattern);
		if(!result.isOK())
			throw new UsersException(result.error().name());
		else
			return result.value();
	}

}
