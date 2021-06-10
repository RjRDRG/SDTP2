package tp1.resources.rest;

import jakarta.inject.Singleton;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import tp1.api.User;
import tp1.api.service.rest.RestUsers;
import tp1.api.service.util.Result;
import tp1.clients.sheet.SpreadsheetClient;
import tp1.impl.UsersImpl;

import java.util.*;

import static tp1.api.service.util.Result.mapError;

@Singleton
public class UsersRestResource implements RestUsers {

	String domainId;
	UsersImpl impl;

	public UsersRestResource(String domainId) {
		this.domainId = domainId;
		this.impl = new UsersImpl();
	}

	@Override
	public String createUser(User user) {
		Result<String> result = impl.createUser(user);
		if(!result.isOK())
			throw new WebApplicationException(mapError(result.error()));
		else
			return result.value();
	}


	@Override
	public User getUser(String userId, String password) {
		Result<User> result = impl.getUser(userId, password);
		if(!result.isOK())
			throw new WebApplicationException(mapError(result.error()));
		else
			return result.value();
	}


	@Override
	public User updateUser(String userId, String password, User user) {
		Result<User> result = impl.updateUser(userId, password, user);
		if(!result.isOK())
			throw new WebApplicationException(mapError(result.error()));
		else
			return result.value();
	}

	@Override
	public Response deleteUser(Long version, String userId, String password) {
		Result<User> result = impl.deleteUser(userId, password);
		if(!result.isOK())
			throw new WebApplicationException(mapError(result.error()));
		else {
			Response.ResponseBuilder responseBuilder =  Response.status(200)
					.encoding(MediaType.APPLICATION_JSON).entity(result.value());

			String header = SpreadsheetClient.SERVICE+domainId;
			String domainVersion = result.getOthers().get(header);

			if(domainVersion != null)
				responseBuilder = responseBuilder.header(header,domainVersion);

			return responseBuilder.build();
		}
	}

	@Override
	public List<User> searchUsers(String pattern) {
		Result<List<User>> result = impl.searchUsers(pattern);
		if(!result.isOK())
			throw new WebApplicationException(mapError(result.error()));
		else
			return result.value();
	}

}
