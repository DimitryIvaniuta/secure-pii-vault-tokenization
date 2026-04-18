package com.github.dimitryivaniuta.gateway.piivault.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Security properties holding local demo users for each vault role.
 */
@Validated
@ConfigurationProperties(prefix = "app.security")
public class AppSecurityProperties {

    private Users users = new Users();

    public Users getUsers() {
        return users;
    }

    public void setUsers(Users users) {
        this.users = users;
    }

    /**
     * Root object for configured users.
     */
    public static class Users {
        private Credential writer = new Credential();
        private Credential reader = new Credential();
        private Credential delete = new Credential();
        private Credential auditor = new Credential();
        private Credential tokenClient = new Credential();
        private Credential operations = new Credential();

        public Credential getWriter() {
            return writer;
        }

        public void setWriter(Credential writer) {
            this.writer = writer;
        }

        public Credential getReader() {
            return reader;
        }

        public void setReader(Credential reader) {
            this.reader = reader;
        }

        public Credential getDelete() {
            return delete;
        }

        public void setDelete(Credential delete) {
            this.delete = delete;
        }

        public Credential getAuditor() {
            return auditor;
        }

        public void setAuditor(Credential auditor) {
            this.auditor = auditor;
        }

        public Credential getTokenClient() {
            return tokenClient;
        }

        public void setTokenClient(Credential tokenClient) {
            this.tokenClient = tokenClient;
        }

        public Credential getOperations() {
            return operations;
        }

        public void setOperations(Credential operations) {
            this.operations = operations;
        }
    }

    /**
     * Simple username/password holder for local development users.
     */
    public static class Credential {
        @NotBlank
        private String username;

        @NotBlank
        private String password;

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }
    }
}
