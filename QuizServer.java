import org.java_websocket.server.WebSocketServer;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.net.InetSocketAddress;
import java.io.*;
import java.nio.file.*;
import java.sql.*;
import java.util.*;
import java.util.concurrent.*;

public class QuizServer extends WebSocketServer {

    private Connection conn;
    private final ConcurrentMap<String, List<Integer>> sessions = new ConcurrentHashMap<>();
    private final ConcurrentMap<WebSocket, String> clientNames = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, String> sessionNames = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, ResultData> sessionResults = new ConcurrentHashMap<>();

    static class ResultData {
        String userName;
        int score;
        int totalQuestions;
        long timestamp;

        ResultData(String userName, int score, int totalQuestions) {
            this.userName = userName;
            this.score = score;
            this.totalQuestions = totalQuestions;
            this.timestamp = System.currentTimeMillis();
        }
    }

    public QuizServer(int port) {
        super(new InetSocketAddress(port));
        connectDB();
    }

    private void connectDB() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            conn = DriverManager.getConnection(
                "jdbc:mysql://localhost:3306/smart_quiz?serverTimezone=UTC",
                "root",
                "Root@123"
            );
            System.out.println("Connected to Database: smart_quiz ");
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    static class UploadHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(405, -1);
                return;
            }
            String query = exchange.getRequestURI().getQuery();
            String sessionId = null;
            if (query != null) {
                for (String kv : query.split("&")) {
                    String[] parts = kv.split("=", 2);
                    if (parts.length == 2 && parts[0].equals("session")) sessionId = parts[1];
                }
            }
            if (sessionId == null || sessionId.isEmpty()) {
                String resp = "Missing session param";
                exchange.sendResponseHeaders(400, resp.length());
                try (OutputStream os = exchange.getResponseBody()) { os.write(resp.getBytes()); }
                return;
            }

            InputStream is = exchange.getRequestBody();
            byte[] data = is.readAllBytes();
            Path dir = Paths.get("uploads");
            if (!Files.exists(dir)) Files.createDirectories(dir);
            String filename = "session_" + sessionId + "_" + System.currentTimeMillis() + ".webm";
            Path file = dir.resolve(filename);
            Files.write(file, data);
            String resp = "Uploaded: " + filename;
            exchange.sendResponseHeaders(200, resp.getBytes().length);
            try (OutputStream os = exchange.getResponseBody()) { os.write(resp.getBytes()); }
            System.out.println("Saved uploaded video: " + file.toAbsolutePath());
        }
    }

    @Override
    public void onOpen(WebSocket client, ClientHandshake handshake) {
        System.out.println("New client connected: " + client.getRemoteSocketAddress());
        client.send("Welcome to Smart Quiz!");
    }

    @Override
    public void onMessage(WebSocket client, String message) {
        System.out.println("Message from client: " + message);

        try {
            if (message.startsWith("NAME|")) {
                String name = message.substring(5).trim();
                clientNames.put(client, name);
                System.out.println(" Registered: " + name);
                return;
            }

            if (message.startsWith("REQ_RANDOM|")) {
                String[] parts = message.split("\\|", 2);
                int n = Integer.parseInt(parts[1].trim());
                String sessionId = UUID.randomUUID().toString();
                List<Integer> qids = new ArrayList<>();

                String userName = clientNames.getOrDefault(client, "Unknown User");
                sessionNames.put(sessionId, userName);

                PreparedStatement ps = conn.prepareStatement(
                    "SELECT question_id, text FROM qst ORDER BY RAND() LIMIT ?"
                );
                ps.setInt(1, n);
                ResultSet rs = ps.executeQuery();

                sessions.put(sessionId, qids);
                client.send("SESSION:" + sessionId);

                while (rs.next()) {
                    int qId = rs.getInt("question_id");
                    String qText = rs.getString("text");
                    qids.add(qId);

                    PreparedStatement ps2 = conn.prepareStatement(
                        "SELECT option_char, text FROM ans WHERE question_id=? ORDER BY option_char ASC"
                    );
                    ps2.setInt(1, qId);
                    ResultSet rs2 = ps2.executeQuery();
                    StringBuilder sb = new StringBuilder();
                    while (rs2.next()) {
                        sb.append(rs2.getString("option_char"))
                          .append(") ")
                          .append(rs2.getString("text"))
                          .append("\n");
                    }
                    client.send("QID:" + qId + "\n" + qText + "\n" + sb.toString().trim());
                    rs2.close();
                    ps2.close();
                }
                rs.close();
                ps.close();
                sessions.put(sessionId, qids);
                return;
            }

            if (message.startsWith("REQ_RESULTS|")) {
                String sessionId = message.split("\\|", 2)[1].trim();
                List<Integer> qids = sessions.get(sessionId);
                if (qids == null) {
                    client.send("ERROR|Unknown session");
                    return;
                }
                for (int qid : qids) {
                    PreparedStatement ps = conn.prepareStatement("SELECT correct_answer FROM qst WHERE question_id=?");
                    ps.setInt(1, qid);
                    ResultSet rs = ps.executeQuery();
                    if (rs.next()) {
                        client.send("RESULT:" + qid + ":" + rs.getString("correct_answer"));
                    }
                    rs.close();
                    ps.close();
                }
                client.send("RESULTS_DONE");
                return;
            }

          
            if (message.startsWith("FINAL_SCORE|")) {
                String[] parts = message.split("\\|");
                if (parts.length >= 4) {
                    String sessionId = parts[1].trim();
                    int score = Integer.parseInt(parts[2].trim());
                    int total = Integer.parseInt(parts[3].trim());

                    String userName = sessionNames.getOrDefault(sessionId, clientNames.getOrDefault(client, "Unknown User"));
                    double percentage = (score * 100.0) / total;

                    sessionResults.put(sessionId, new ResultData(userName, score, total));

             
                    try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO results (session_id, user_name, score, total_questions, percentage) VALUES (?, ?, ?, ?, ?)"
                    )) {
                        ps.setString(1, sessionId);
                        ps.setString(2, userName);
                        ps.setInt(3, score);
                        ps.setInt(4, total);
                        ps.setDouble(5, percentage);
                        ps.executeUpdate();
                        System.out.println("Result saved to DB for " + userName + " (" + score + "/" + total + ")");
                    }

                    System.out.println("QUIZ COMPLETED: " + userName + " | Score: " + score + "/" + total);
                }
                return;
            }

            // Normal answer handling
            String[] parts = message.split("\\|");
            if (parts.length < 2) {
                client.send("ERROR|Invalid format");
                return;
            }

            int questionId = Integer.parseInt(parts[0].trim());
            String answer = parts[1].trim();
            String userName = clientNames.getOrDefault(client, "Unknown User");

            PreparedStatement ps = conn.prepareStatement("SELECT correct_answer FROM qst WHERE question_id=?");
            ps.setInt(1, questionId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                String correct = rs.getString("correct_answer");
                if (correct.equalsIgnoreCase(answer)) {
                    client.send(" Correct!");
                } else {
                    client.send("Wrong! Correct: " + correct);
                }
            }
            rs.close();
            ps.close();

        } catch (Exception e) {
            e.printStackTrace();
            client.send("Server Error!");
        }
    }

    @Override
    public void onClose(WebSocket client, int code, String reason, boolean remote) {
        String name = clientNames.getOrDefault(client, "Unknown");
        System.out.println("Client disconnected: " + name);
        clientNames.remove(client);
    }

    @Override
    public void onError(WebSocket client, Exception ex) {
        ex.printStackTrace();
    }

    @Override
    public void onStart() {
        System.out.println(" Quiz Server running on port " + getPort());
    }

    public static void main(String[] args) throws Exception {
        QuizServer server = new QuizServer(12345);
        server.start();
        System.out.println("Smart Quiz Server running on port 12345");

        HttpServer http = HttpServer.create(new InetSocketAddress(12346), 0);
        http.createContext("/uploadVideo", new UploadHandler());
        http.setExecutor(Executors.newCachedThreadPool());
        http.start();
        System.out.println("HTTP upload endpoint: http://localhost:12346/uploadVideo");
    }
}