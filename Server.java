import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Collectors;

public class Server {

    // ✅ IMPORTANT: Use Render/Docker assigned port if available, else 8000 (Local)
    private static final int PORT = Integer.parseInt(System.getenv().getOrDefault("PORT", "8000"));
    private static final String FRONTEND_DIR = "./frontend/";
    private static final String PETS_JSON_PATH = "./pets.json";

    public static void main(String[] args) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);

        server.createContext("/pets", new PetsHandler());
        server.createContext("/", new StaticFileHandler());

        server.setExecutor(null);
        server.start();

        System.out.println("🚀 Server running on port: " + PORT);
    }

    // ✅ Handle /pets → load pets.json
    static class PetsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"GET".equals(exchange.getRequestMethod())) {
                sendResponse(exchange, 405, "Method Not Allowed", "text/plain");
                return;
            }

            try {
                Path path = Paths.get(PETS_JSON_PATH);
                String json = Files.lines(path).collect(Collectors.joining("\n"));

                exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
                sendResponse(exchange, 200, json, "application/json");
            } catch (Exception e) {
                sendResponse(exchange, 500, "{\"error\":\"Unable to load pets.json\"}", "application/json");
            }
        }
    }

    // ✅ Optional: Serve index.html, CSS, JS (only if frontend folder exists)
    static class StaticFileHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String uriPath = exchange.getRequestURI().getPath();
            if (uriPath.equals("/")) uriPath = "/index.html";

            String filePath = FRONTEND_DIR + uriPath.substring(1);
            File file = new File(filePath);

            if (!file.exists()) {
                sendResponse(exchange, 404, "404 Not Found", "text/plain");
                return;
            }

            byte[] bytes = Files.readAllBytes(file.toPath());
            exchange.getResponseHeaders().set("Content-Type", getMimeType(uriPath));
            exchange.sendResponseHeaders(200, bytes.length);

            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        }

        private String getMimeType(String path) {
            if (path.endsWith(".html")) return "text/html";
            if (path.endsWith(".css")) return "text/css";
            if (path.endsWith(".js")) return "application/javascript";
            return "text/plain";
        }
    }

    private static void sendResponse(HttpExchange exchange, int status, String response, String type) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", type);
        exchange.sendResponseHeaders(status, response.getBytes().length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(response.getBytes());
        }
    }
}
