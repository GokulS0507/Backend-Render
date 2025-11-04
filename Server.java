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

    // --- IMPORTANT CHANGE: Render assigns PORT dynamically ---
    private static final int PORT = Integer.parseInt(System.getenv().getOrDefault("PORT", "8000"));
    private static final String FRONTEND_DIR = "./frontend/";
    private static final String PETS_JSON_PATH = "./pets.json";

    public static void main(String[] args) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);

        server.createContext("/pets", new PetsHandler());
        server.createContext("/", new StaticFileHandler());

        server.start();
        System.out.println("🚀 Server is live on PORT: " + PORT);
    }

    static class PetsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            // Allow only GET requests
            if (!"GET".equals(exchange.getRequestMethod())) {
                sendResponse(exchange, 405, "Method Not Allowed", "text/plain");
                return;
            }

            try {
                Path path = Paths.get(PETS_JSON_PATH);
                String jsonResponse = Files.lines(path).collect(Collectors.joining("\n"));

                // ✅ CORS for frontend access
                exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
                sendResponse(exchange, 200, jsonResponse, "application/json");
            } catch (IOException e) {
                sendResponse(exchange, 500, "{\"error\":\"Unable to load pets.json\"}", "application/json");
            }
        }
    }

    static class StaticFileHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String requestPath = exchange.getRequestURI().getPath();
            if (requestPath.equals("/")) requestPath = "/index.html";

            String filePath = FRONTEND_DIR + requestPath.substring(1);
            File file = new File(filePath);

            if (!file.exists()) {
                sendResponse(exchange, 404, "404 Not Found", "text/plain");
                return;
            }

            byte[] fileBytes = Files.readAllBytes(file.toPath());
            exchange.getResponseHeaders().set("Content-Type", getContentType(requestPath));
            exchange.sendResponseHeaders(200, fileBytes.length);

            try (OutputStream os = exchange.getResponseBody()) {
                os.write(fileBytes);
            }
        }

        private String getContentType(String path) {
            if (path.endsWith(".html")) return "text/html";
            if (path.endsWith(".css")) return "text/css";
            if (path.endsWith(".js")) return "application/javascript";
            return "text/plain";
        }
    }

    private static void sendResponse(HttpExchange exchange, int statusCode, String response, String type) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", type);
        exchange.sendResponseHeaders(statusCode, response.getBytes().length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(response.getBytes());
        }
    }
}
