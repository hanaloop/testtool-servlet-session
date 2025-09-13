package com.hanaloop.tool.auth;

import org.yaml.snakeyaml.TypeDescription;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class UserStore {
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
            return cachedUsers;
        }

        // Try multiple resource names to be permissive with the filename typo
        String[] candidateResources = new String[]{
                "/users.db.yam", // as given
                "/users.db.yml",
                "/users.db.yaml"
        };

        UsersWrapper wrapper = null;
        for (String res : candidateResources) {
            try (InputStream in = UserStore.class.getResourceAsStream(res)) {
                if (in == null) continue;

                Constructor ctor = new Constructor(UsersWrapper.class, null);
                TypeDescription td = new TypeDescription(UsersWrapper.class);
                td.addPropertyParameters("users", User.class);
                ctor.addTypeDescription(td);
                Yaml yaml = new Yaml(ctor);
                wrapper = yaml.load(in);
                if (wrapper != null && wrapper.getUsers() != null) {
                    break;
                }
            } catch (Exception ignored) {
            }
        }

        if (wrapper == null || wrapper.getUsers() == null) {
            cachedUsers = Collections.emptyList();
        } else {
            cachedUsers = wrapper.getUsers();
        }
        return cachedUsers;
    }

    public static User findByCredentials(String userId, String password) {
        List<User> users = loadUsers();
        if (users == null) return null;
        for (User u : users) {
            if (Objects.equals(u.getUserId(), userId) && Objects.equals(u.getPassword(), password)) {
                return u;
            }
        }
        return null;
    }
}

