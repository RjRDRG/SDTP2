package tp1.impl;

import tp1.api.User;
import tp1.api.service.util.Result;
import tp1.discovery.Discovery;

import java.util.*;

import static tp1.api.service.util.Result.ErrorCode;

public class UsersImpl {

    private final Map<String, User> users;

    public UsersImpl() {
        this.users = new HashMap<>();
    }

    public Result<String> createUser(User user) {
        if(user.getUserId() == null || user.getPassword() == null || user.getFullName() == null ||
                user.getEmail() == null) {
            return Result.error(ErrorCode.BAD_REQUEST);
        }

        synchronized ( this ) {
            String userId = user.getUserId();

            if(users.containsKey(userId)) {
                return Result.error(ErrorCode.CONFLICT);
            }

            users.put(userId, user);

            return Result.ok(userId);
        }
    }



    public Result<User> getUser(String userId, String password) {
        User user = users.get(userId);

        if( user == null ) {
            return Result.error(ErrorCode.NOT_FOUND);
        }

        if(!user.getPassword().equals(password)) {
            return Result.error(ErrorCode.FORBIDDEN );
        }

        return Result.ok(user);
    }



    public Result<User> updateUser(String userId, String password, User user) {
        if(userId == null || password == null) {
            return Result.error(ErrorCode.BAD_REQUEST );
        }

        synchronized ( this ) {
            User oldUser = users.get(userId);

            if( oldUser == null ) {
                return Result.error(ErrorCode.NOT_FOUND );
            }

            if( !oldUser.getPassword().equals( password)) {
                return Result.error(ErrorCode.FORBIDDEN );
            }

            User newUser = new User(userId,
                    user.getFullName() == null ? oldUser.getFullName() : user.getFullName(),
                    user.getEmail() == null ? oldUser.getEmail() : user.getEmail(),
                    user.getPassword() == null ? oldUser.getPassword() : user.getPassword());

            users.put(userId, newUser);

            return Result.ok(newUser);
        }
    }

    public Result<User> deleteUser(String userId, String password) {
        if(userId == null ) {
            return Result.error(ErrorCode.BAD_REQUEST );
        }

        synchronized ( this ) {
            User user = users.get(userId);

            if( user == null ) {
                return Result.error(ErrorCode.NOT_FOUND );
            }

            if( !user.getPassword().equals( password)) {
                return Result.error(ErrorCode.FORBIDDEN );
            }

            try {
                Discovery.getLocalSpreadsheetClients().deleteUserSpreadsheets(userId, password).value();
            } catch (Exception e) {
                e.printStackTrace();
            }

            return Result.ok(users.remove(userId));
        }
    }

    public Result<List<User>> searchUsers(String pattern) {

        if (users.isEmpty()) {
            return Result.ok(new ArrayList<>());
        }

        if (pattern == null || pattern.isEmpty()) {
            return Result.ok(new ArrayList<>(users.values()));
        }

        synchronized (this) {
            List<User> result = new LinkedList<>();
            for (User u : users.values()) {
                if (u.getFullName().toLowerCase().contains(pattern.toLowerCase()))
                    result.add(u);
            }

            return Result.ok(result);
        }
    }

}

