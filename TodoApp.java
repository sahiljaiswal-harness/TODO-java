import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class TodoApp {

    static class TodoItem {
        UUID id;
        String title;
        String description;
        boolean completed;
        boolean deleted;
        Instant createdAt;
        Instant updatedAt;
    }

    private static final Map<UUID, TodoItem> todos = new ConcurrentHashMap<>();

    public static void main(String[] args) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(8000), 0);

        server.createContext("/todos", new TodoHandler());
        server.setExecutor(null);
        server.start();
        System.out.println("Server running on http://localhost:8000");
    }

    static class TodoHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String method = exchange.getRequestMethod();
            String path = exchange.getRequestURI().getPath();
            String[] pathParts = path.split("/");

            if (method.equalsIgnoreCase("GET") && pathParts.length == 2) {
                handleList(exchange);
            } else if (method.equalsIgnoreCase("POST") && pathParts.length == 2) {
                handleCreate(exchange);
            } else if (pathParts.length == 3) {
                UUID id = UUID.fromString(pathParts[2]);

                switch (method.toUpperCase()) {
                    case "GET" -> handleGet(exchange, id);
                    case "PUT" -> handleUpdate(exchange, id);
                    case "DELETE" -> handleSoftDelete(exchange, id);
                    default -> sendResponse(exchange, 405, "Method Not Allowed");
                }
            } else {
                sendResponse(exchange, 404, "Not Found");
            }
        }

        private void handleList(HttpExchange exchange) throws IOException {
            List<TodoItem> list = todos.values().stream()
                    .filter(todo -> !todo.deleted)
                    .collect(Collectors.toList());

            String json = toJson(list);
            sendResponse(exchange, 200, json);
        }

        private void handleCreate(HttpExchange exchange) throws IOException {
            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            Map<String, String> data = parseJson(body);

            TodoItem todo = new TodoItem();
            todo.id = UUID.randomUUID();
            todo.title = data.getOrDefault("title", "");
            todo.description = data.getOrDefault("description", "");
            todo.completed = Boolean.parseBoolean(data.getOrDefault("completed", "false"));
            todo.deleted = false;
            todo.createdAt = Instant.now();
            todo.updatedAt = Instant.now();

            todos.put(todo.id, todo);
            sendResponse(exchange, 201, toJson(todo));
        }

        private void handleGet(HttpExchange exchange, UUID id) throws IOException {
            TodoItem todo = todos.get(id);
            if (todo == null || todo.deleted) {
                sendResponse(exchange, 404, "Not Found");
                return;
            }
            sendResponse(exchange, 200, toJson(todo));
        }

        private void handleUpdate(HttpExchange exchange, UUID id) throws IOException {
            TodoItem todo = todos.get(id);
            if (todo == null || todo.deleted) {
                sendResponse(exchange, 404, "Not Found");
                return;
            }

            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            Map<String, String> data = parseJson(body);

            todo.title = data.getOrDefault("title", todo.title);
            todo.description = data.getOrDefault("description", todo.description);
            todo.completed = Boolean.parseBoolean(data.getOrDefault("completed", String.valueOf(todo.completed)));
            todo.updatedAt = Instant.now();

            sendResponse(exchange, 200, toJson(todo));
        }

        private void handleSoftDelete(HttpExchange exchange, UUID id) throws IOException {
            TodoItem todo = todos.get(id);
            if (todo == null || todo.deleted) {
                sendResponse(exchange, 404, "Not Found");
                return;
            }
            todo.deleted = true;
            todo.updatedAt = Instant.now();
            sendResponse(exchange, 204, "");
        }

        private void sendResponse(HttpExchange exchange, int status, String response) throws IOException {
            byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(status, bytes.length);
            OutputStream os = exchange.getResponseBody();
            os.write(bytes);
            os.close();
        }

        // JSON parsing
        private Map<String, String> parseJson(String json) {
            Map<String, String> map = new HashMap<>();
            json = json.trim().replaceAll("[{}\"]", "");
            String[] parts = json.split(",");
            for (String part : parts) {
                String[] kv = part.trim().split(":", 2);
                if (kv.length == 2) {
                    map.put(kv[0].trim(), kv[1].trim());
                }
            }
            return map;
        }

        // JSON builder
        private String toJson(Object obj) {
            if (obj instanceof List<?>) {
                return ((List<?>) obj).stream()
                        .map(this::toJson)
                        .collect(Collectors.joining(",", "[", "]"));
            } else if (obj instanceof TodoItem todo) {
                return String.format("""
                        {
                          "id":"%s",
                          "title":"%s",
                          "description":"%s",
                          "completed":%s,
                          "deleted":%s,
                          "createdAt":"%s",
                          "updatedAt":"%s"
                        }""",
                        todo.id, todo.title, todo.description,
                        todo.completed, todo.deleted,
                        todo.createdAt, todo.updatedAt);
            } else {
                return "{}";
            }
        }
    }
}
