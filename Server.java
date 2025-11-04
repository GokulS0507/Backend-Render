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

/**
 * Pure Java HTTP Server using com.sun.net.httpserver.HttpServer.
 * Serves static files from ./frontend/ and JSON data from ./pets.json.
 */
public class Server {

    // --- Configuration ---
    private static final int PORT = 8000;
    private static final String FRONTEND_DIR = "./frontend/";
    private static final String PETS_JSON_PATH = "./pets.json";

    public static void main(String[] args) throws IOException {
        
        // Instructions to run:
        // 1. Compile: javac Server.java
        // 2. Run: java Server
        
        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);
        
        // API Context: /pets
        server.createContext("/pets", new PetsHandler());
        
        // Static File Context: / (Handles all other requests: index.html, styles.css, script.js)
        server.createContext("/", new StaticFileHandler());
        
        server.setExecutor(null); // Use default executor
        server.start();
        
        System.out.println("🚀 Pet Adoption Server started on port " + PORT + ". Access at http://localhost:" + PORT);
    }

    /**
     * Handler for the /pets endpoint. Reads pets.json and returns JSON.
     */
    static class PetsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"GET".equals(exchange.getRequestMethod())) {
                sendResponse(exchange, 405, "Method Not Allowed", "text/plain");
                return;
            }

            try {
                // Read pets.json content from the root directory
                Path path = Paths.get(PETS_JSON_PATH);
                String jsonResponse = Files.lines(path).collect(Collectors.joining("\n"));

                // Set headers (including CORS for frontend)
                exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
                sendResponse(exchange, 200, jsonResponse, "application/json");

            } catch (IOException e) {
                System.err.println("Error reading pets.json: " + e.getMessage());
                sendResponse(exchange, 500, "{\"error\": \"Server failed to load pet data.\"}", "application/json");
            }
        }
    }

    /**
     * Handler for static files (HTML, CSS, JS) from the /frontend directory.
     */
    static class StaticFileHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String requestPath = exchange.getRequestURI().getPath();
            
            // Default to index.html for root path or missing file extension
            if (requestPath.equals("/")) {
                requestPath = "/index.html";
            }

            // Path translation: /styles.css -> ./frontend/styles.css
            // We use substring(1) to remove the leading "/"
            String filePath = FRONTEND_DIR + requestPath.substring(1); 
            File file = new File(filePath);
            
            if (!file.exists() || file.isDirectory()) {
                sendResponse(exchange, 404, "404 Not Found", "text/plain");
                return;
            }
            
            // Read file bytes
            byte[] fileBytes = Files.readAllBytes(file.toPath());
            
            // Determine content type based on extension
            String contentType = getContentType(requestPath);
            
            exchange.sendResponseHeaders(200, fileBytes.length);
            exchange.getResponseHeaders().set("Content-Type", contentType);
            
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(fileBytes);
            }
        }
        
        private String getContentType(String path) {
            if (path.endsWith(".html")) return "text/html";
            if (path.endsWith(".css")) return "text/css";
            if (path.endsWith(".js")) return "application/javascript";
            // Default or unknown file type
            return "text/plain"; 
        }
    }

    /**
     * Utility method to send an HTTP response with explicit content type.
     */
    private static void sendResponse(HttpExchange exchange, int statusCode, String response, String contentType) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.sendResponseHeaders(statusCode, response.getBytes().length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(response.getBytes());
        }
    }
}