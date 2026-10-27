package com.silo.service;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Properties;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.silo.config.SupabaseAuth;
import com.silo.config.SupabaseConfig;

public class AuthService {

    public enum Role {
        FARMER,
        COORDINATOR,
        ADMIN
    }

    public enum Status {
        PENDING,
        ACTIVE,
        REJECTED
    }

    public static class Account {
        private final String name;
        private final String email;
        private final Role role;
        private final Status status;

        private Account(
                String name,
                String email,
                Role role,
                Status status) {
            this.name = name;
            this.email = email;
            this.role = role;
            this.status = status;
        }

        public String getName() {
            return name;
        }

        public String getEmail() {
            return email;
        }

        public Role getRole() {
            return role;
        }

        public Status getStatus() {
            return status;
        }

        public static Account fromSupabaseProfile(
                String name, String email, String role, String status) {

            Role parsedRole = Role.valueOf(role.toUpperCase());
            Status parsedStatus = Status.valueOf(status.toUpperCase());

            return new Account(name, email, parsedRole, parsedStatus);
        }
    }

    private static final int ITERATIONS = 210_000;
    private static final int KEY_LENGTH = 256;
    private static final int SALT_LENGTH = 16;
    private static final String SUPABASE_SIGNUP_URL = SupabaseConfig.SUPABASE_URL + "/auth/v1/signup";
    private static final String PUBLISHABLE_KEY = "sb_publishable_tJ6Y1LgDPmUDuukAVQEiMg_tWY505dW";

    private final Path file;
    private final Properties accounts = new Properties();
    private final SecureRandom random = new SecureRandom();
    private final HttpClient httpClient;
    private final ObjectMapper mapper;
    private final String apiKey;

    public AuthService(Path dataDirectory) {
    this.apiKey = SupabaseConfig.getPublishableKey(); 
           this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(15))
                .build();
        this.mapper = new ObjectMapper();

        try {
            Files.createDirectories(dataDirectory);
            file = dataDirectory.resolve("accounts.properties");

            if (Files.exists(file)) {
                try (InputStream in = Files.newInputStream(file)) {
                    accounts.load(in);
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException(
                    "Could not load account storage.", e
            );
        }
    }

    public synchronized Account registerFarmer(
            String name, String email, char[] password) {
        return register(name, email, password,
                Role.FARMER, Status.ACTIVE);
    }

    public synchronized Account applyAsCoordinator(
            String name, String email, char[] password) {
        return register(name, email, password,
                Role.COORDINATOR, Status.PENDING);
    }

    public synchronized boolean hasAdminAccount() {
        for (String prefix : getAccountPrefixes()) {
            String roleValue = accounts.getProperty(prefix + "role");
            if (Role.ADMIN.name().equals(roleValue)) {
                return true;
            }
        }
        return false;
    }

    public synchronized Account registerAdmin(
            String name, String email, char[] password) {
        return ensureAdminAccount(name, email, password);
    }

    public synchronized Account ensureAdminAccount(
            String name, String email, char[] password) {
        if (hasAdminAccount()) {
            throw new IllegalArgumentException(
                    "An administrator account already exists.");
        }
        return register(name, email, password,
                Role.ADMIN, Status.ACTIVE);
    }

    private Account register(
            String name,
            String email,
            char[] password,
            Role role,
            Status status) {

        String cleanName = name == null ? "" : name.trim();
        String cleanEmail = normalizeEmail(email);

        if (cleanName.length() < 2) {
            throw new IllegalArgumentException(
                    "Please enter your full name."
            );
        }

        if (!cleanEmail.matches(
                "^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
            throw new IllegalArgumentException(
                    "Please enter a valid email address."
            );
        }

        if (password == null || password.length < 8) {
            throw new IllegalArgumentException(
                    "Your password must contain at least 8 characters."
            );
        }

        // Register to Supabase Cloud Database first
        registerToSupabase(cleanEmail, password, cleanName, role, status);

        byte[] salt = new byte[SALT_LENGTH];
        random.nextBytes(salt);
        byte[] hash = hashPassword(password, salt);

        String prefix = nextAccountPrefix();

        accounts.setProperty(key(cleanEmail, "email"), prefix);
        accounts.setProperty(prefix + "name", cleanName);
        accounts.setProperty(prefix + "email", cleanEmail);
        accounts.setProperty(prefix + "role", role.name());
        accounts.setProperty(prefix + "status", status.name());
        accounts.setProperty(prefix + "salt",
                Base64.getEncoder().encodeToString(salt));
        accounts.setProperty(prefix + "hash",
                Base64.getEncoder().encodeToString(hash));

        save();

        return new Account(cleanName, cleanEmail, role, status);
    }

    private void registerToSupabase(String email, char[] password, String name, Role role, Status status) {
        try {
            String passwordText = new String(password);
            ObjectNode body = mapper.createObjectNode()
                    .put("email", email)
                    .put("password", passwordText);

            ObjectNode dataNode = body.putObject("data");
            dataNode.put("name", name);
            dataNode.put("role", role.name());
            dataNode.put("status", status.name());

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(SUPABASE_SIGNUP_URL))
                    .timeout(Duration.ofSeconds(20))
                    .header("apikey", apiKey)
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body)))
                    .build();

            HttpResponse<String> response = httpClient.send(
                    request,
                    HttpResponse.BodyHandlers.ofString()
            );

            JsonNode result = mapper.readTree(response.body());

            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                String errorMessage = result.path("msg").asText(
                        result.path("message").asText(
                                result.path("error_description").asText(
                                        result.path("error").asText("Supabase registration failed."))));
                throw new IllegalArgumentException(errorMessage);
            }
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException("Failed to connect to Supabase for registration: " + e.getMessage(), e);
        }
    }

    public synchronized Account login(
            String email, char[] password) {

        String cleanEmail = normalizeEmail(email);
        String prefix = accounts.getProperty(
                key(cleanEmail, "email"));

        // Fallback: If the account exists in Supabase but lacks a local properties profile yet, authenticate and create it.
        if (prefix == null) {
            try {
                SupabaseAuth supabaseAuth = new SupabaseAuth();
                JsonNode authResult = supabaseAuth.signIn(cleanEmail, password);

                if (authResult != null && authResult.hasNonNull("access_token")) {
                    String defaultName = cleanEmail.split("@")[0];
                    byte[] salt = new byte[SALT_LENGTH];
                    random.nextBytes(salt);
                    byte[] hash = hashPassword(password, salt);

                    prefix = nextAccountPrefix();
                    accounts.setProperty(key(cleanEmail, "email"), prefix);
                    accounts.setProperty(prefix + "name", defaultName);
                    accounts.setProperty(prefix + "email", cleanEmail);
                    accounts.setProperty(prefix + "role", Role.FARMER.name());
                    accounts.setProperty(prefix + "status", Status.ACTIVE.name());
                    accounts.setProperty(prefix + "salt", Base64.getEncoder().encodeToString(salt));
                    accounts.setProperty(prefix + "hash", Base64.getEncoder().encodeToString(hash));
                    save();

                    return new Account(defaultName, cleanEmail, Role.FARMER, Status.ACTIVE);
                }
            } catch (Exception e) {
                throw new IllegalArgumentException("Incorrect email or password.");
            }
        }

        if (prefix == null || password == null) {
            throw new IllegalArgumentException(
                    "Incorrect email or password."
            );
        }

        try {
            byte[] salt = Base64.getDecoder().decode(
                    accounts.getProperty(prefix + "salt"));
            byte[] expected = Base64.getDecoder().decode(
                    accounts.getProperty(prefix + "hash"));
            byte[] actual = hashPassword(password, salt);

            if (!MessageDigest.isEqual(expected, actual)) {
                throw new IllegalArgumentException(
                        "Incorrect email or password."
                );
            }

            Role role = Role.valueOf(
                    accounts.getProperty(prefix + "role"));

            Status status = Status.valueOf(
                    accounts.getProperty(prefix + "status", "ACTIVE"));

            if (status == Status.PENDING) {
                throw new IllegalArgumentException(
                        "Your coordinator application is pending approval."
                );
            }

            if (status == Status.REJECTED) {
                throw new IllegalArgumentException(
                        "Your coordinator application was not approved."
                );
            }

            return new Account(
                    accounts.getProperty(prefix + "name"),
                    cleanEmail, role, status);

        } catch (IllegalArgumentException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new IllegalStateException(
                    "Account data could not be read.", e
            );
        }
    }

    public synchronized List<Account> getCoordinatorApplications() {
        List<Account> result = new ArrayList<>();

        for (String prefix : getAccountPrefixes()) {
            String roleValue = accounts.getProperty(prefix + "role");
            String statusValue = accounts.getProperty(prefix + "status", "ACTIVE");

            if (Role.COORDINATOR.name().equals(roleValue)
                    && Status.PENDING.name().equals(statusValue)) {
                result.add(readAccount(prefix));
            }
        }

        return result;
    }

    public synchronized void approveCoordinator(String email) {
        updateCoordinatorStatus(email, Status.ACTIVE);
    }

    public synchronized void rejectCoordinator(String email) {
        updateCoordinatorStatus(email, Status.REJECTED);
    }

    private void updateCoordinatorStatus(
            String email, Status newStatus) {

        String cleanEmail = normalizeEmail(email);
        String prefix = accounts.getProperty(
                key(cleanEmail, "email"));

        if (prefix == null) {
            throw new IllegalArgumentException(
                    "No account exists for that email."
            );
        }

        if (!Role.COORDINATOR.name().equals(
                accounts.getProperty(prefix + "role"))) {
            throw new IllegalArgumentException(
                    "This account is not a coordinator account."
            );
        }

        accounts.setProperty(prefix + "status", newStatus.name());
        save();
    }

    private Account readAccount(String prefix) {
        return new Account(
                accounts.getProperty(prefix + "name"),
                accounts.getProperty(prefix + "email"),
                Role.valueOf(accounts.getProperty(prefix + "role")),
                Status.valueOf(accounts.getProperty(prefix + "status", "ACTIVE"))
        );
    }

    private List<String> getAccountPrefixes() {
        List<String> prefixes = new ArrayList<>();

        for (String property : accounts.stringPropertyNames()) {
            if (property.startsWith("lookup.")
                    && property.endsWith(".email")) {
                String prefix = accounts.getProperty(property);
                if (prefix != null && !prefixes.contains(prefix)) {
                    prefixes.add(prefix);
                }
            }
        }

        return prefixes;
    }

    private String nextAccountPrefix() {
        int index = 0;
        String prefix;

        do {
            prefix = "account." + index++ + ".";
        } while (accounts.containsKey(prefix + "email"));

        return prefix;
    }

    private byte[] hashPassword(char[] password, byte[] salt) {
        PBEKeySpec spec = new PBEKeySpec(
                password, salt, ITERATIONS, KEY_LENGTH);

        try {
            return SecretKeyFactory
                    .getInstance("PBKDF2WithHmacSHA256")
                    .generateSecret(spec)
                    .getEncoded();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(
                    "Password security is unavailable.", e
            );
        } finally {
            spec.clearPassword();
        }
    }

    private String normalizeEmail(String email) {
        return email == null
                ? ""
                : email.trim().toLowerCase(Locale.ROOT);
    }

    private String key(String email, String field) {
        String encoded = Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(email.getBytes(StandardCharsets.UTF_8));

        return "lookup." + encoded + "." + field;
    }

    private void save() {
        try (OutputStream out = Files.newOutputStream(file)) {
            accounts.store(out, "Silo local account records");
        } catch (IOException e) {
            throw new IllegalStateException(
                    "Could not save the account. Check the data folder.", e
            );
        }
    }
}