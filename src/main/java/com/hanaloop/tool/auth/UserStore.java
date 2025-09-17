package com.hanaloop.tool.auth;

import org.yaml.snakeyaml.TypeDescription;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.LoaderOptions;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

public class UserStore {
    private static final Logger LOGGER = Logger.getLogger(UserStore.class.getName());
    public static class UsersWrapper {
        private List<User> users = new ArrayList<>();

        public List<User> getUsers() {
            return users;
        }

        public void setUsers(List<User> users) {
            this.users = users;
        }
    }

    private static List<User> cachedUsers;

    public static synchronized List<User> loadUsers() {
        if (cachedUsers != null) {
            LOGGER.fine("Returning cached users list");
            return cachedUsers;
        }

        // Optional external user DB via env var; falls back to classpath resource
        final String ENV_USER_DB_FILE = "USER_STORE_FILE";
        final String userDbResource = "/users.db.yml";

        UsersWrapper wrapper = null;
        
        // 1) Try external file if specified
        String externalPath = System.getenv(ENV_USER_DB_FILE);
        if (externalPath != null && !externalPath.isBlank()) {
            File extFile = new File(externalPath);
            try (InputStream in = new FileInputStream(extFile)) {

                wrapper = UserStore.loadUserFromInputStream(in, externalPath);
                if (wrapper != null && wrapper.getUsers() != null) {
                    LOGGER.info("Loaded " + wrapper.getUsers().size() + " user(s) from external file");
                }
            } catch (Exception ex) {
                LOGGER.log(Level.WARNING, "Failed to load users from external file '" + externalPath + "'", ex);
            }
        }

        // 2) Fall back to bundled resource if needed
        if (wrapper == null || wrapper.getUsers() == null) {
            try (InputStream in = UserStore.class.getResourceAsStream(userDbResource)) {
                
                wrapper = UserStore.loadUserFromInputStream(in, userDbResource);
                if (wrapper != null && wrapper.getUsers() != null) {
                    LOGGER.info("Loaded " + wrapper.getUsers().size() + " user(s) from " + userDbResource);
                }
            } catch (Exception ex) {
                LOGGER.log(Level.WARNING, "Failed to load users from resource: " + userDbResource, ex);
            }
        }

        if (wrapper == null || wrapper.getUsers() == null) {
            LOGGER.warning("No users loaded; defaulting to empty list.");
            cachedUsers = Collections.emptyList();
        } else {
            cachedUsers = wrapper.getUsers();
        }
        return cachedUsers;
    }

    public static UsersWrapper loadUserFromInputStream(InputStream in, String userDbResource) {
        LoaderOptions opts = new LoaderOptions();
        Constructor ctor = new Constructor(UsersWrapper.class, opts);
        TypeDescription td = new TypeDescription(UsersWrapper.class);
        td.addPropertyParameters("users", User.class);
        ctor.addTypeDescription(td);
        Yaml yaml = new Yaml(ctor);
        LOGGER.info("Loading users from resource: " + userDbResource);
        return yaml.load(in);
    }

    public static User findByCredentials(String userId, String password) {
        List<User> users = loadUsers();
        if (users == null) return null;
        for (User u : users) {
            if (Objects.equals(u.getUserId(), userId) && Objects.equals(u.getPassword(), password)) {
                LOGGER.fine("Credential match for userId='" + userId + "'");
                return u;
            }
        }
        LOGGER.fine("No credential match for userId='" + userId + "'");
        return null;
    }
}
