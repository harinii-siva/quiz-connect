import org.java_websocket.server.WebSocketServer;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpHandler;
import java.net.InetSocketAddress;
import java.io.*;
import java.nio.file.*;
import java.sql.*;
import java.util.*;
import java.util.concurrent.*;


public class QuizServer extends WebSocketServer {

    private Connection conn;
    // sessionId -> ordered list of question IDs for that session
    private final ConcurrentMap<String, List<Integer>> sessions = new ConcurrentHashMap<>();
    // client -> user name
    private final ConcurrentMap<WebSocket, String> clientNames = new ConcurrentHashMap<>();
    // sessionId -> user name
    private final ConcurrentMap<String, String> sessionNames = new ConcurrentHashMap<>();
    // Store quiz results: sessionId -> ResultData
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
            System.out.println("Connected to Database: smart_quiz");
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    // Handler to accept video blob in request body and save to disk
    static class UploadHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try {
                if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                    exchange.sendResponseHeaders(405, -1);
                    return;
                }
                // parse session param
                String query = exchange.getRequestURI().getQuery();
                String sessionId = null;
                if (query != null) {
                    for (String kv : query.split("&")) {
                        String[] parts = kv.split("=", 2);
                        if (parts.length == 2 && parts[0].equals("session")) {
                            sessionId = parts[1];
                        }
                    }
                }
                if (sessionId == null || sessionId.isEmpty()) {
                    String resp = "Missing session param";
                    exchange.sendResponseHeaders(400, resp.length());
                    try (OutputStream os = exchange.getResponseBody()) { os.write(resp.getBytes()); }
                    return;
                }

                // read body bytes
                InputStream is = exchange.getRequestBody();
                byte[] data = is.readAllBytes();

                // ensure directory
                Path dir = Paths.get("uploads");
                if (!Files.exists(dir)) Files.createDirectories(dir);

                // create filename
                String filename = "session_" + sessionId + "_" + System.currentTimeMillis() + ".webm";
                Path file = dir.resolve(filename);
                Files.write(file, data, StandardOpenOption.CREATE_NEW);

                String resp = "Uploaded: " + filename;
                exchange.sendResponseHeaders(200, resp.getBytes().length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(resp.getBytes());
                }
                System.out.println("📹 Saved uploaded video: " + file.toAbsolutePath());
            } catch (Exception e) {
                e.printStackTrace();
                String resp = "Server error";
                exchange.sendResponseHeaders(500, resp.length());
                try (OutputStream os = exchange.getResponseBody()) { os.write(resp.getBytes()); }
            }
        }
    }

    @Override
    public void onOpen(WebSocket client, ClientHandshake handshake) {
        System.out.println("🟢 New client connected: " + client.getRemoteSocketAddress());
        client.send("Welcome to Smart Quiz!");
    }

    @Override
    public void onMessage(WebSocket client, String message) {
        System.out.println("📩 Message from client: " + message);

        try {
            // Handle name registration
            if (message.startsWith("NAME|")) {
                String name = message.substring(5).trim();
                clientNames.put(client, name);
                System.out.println("👤 Client registered as: " + name + " (" + client.getRemoteSocketAddress() + ")");
                return;
            }

            // Handle request for random questions
            if (message.startsWith("REQ_RANDOM|")) {
                String[] parts = message.split("\\|", 2);
                int n = Integer.parseInt(parts[1].trim());

                // Generate session id
                String sessionId = UUID.randomUUID().toString();
                List<Integer> qids = new ArrayList<>();

                // Get user name for this session
                String userName = clientNames.getOrDefault(client, "Unknown User");
                sessionNames.put(sessionId, userName);

                // Fetch n random question ids and text
                PreparedStatement ps = conn.prepareStatement("SELECT question_id, text FROM qst ORDER BY RAND() LIMIT ?");
                ps.setInt(1, n);
                ResultSet rs = ps.executeQuery();

                // store questions in session and send session id first
                sessions.put(sessionId, qids);
                client.send("SESSION:" + sessionId);

                System.out.println(" Starting quiz session for: " + userName + " (Session: " + sessionId + ", Questions: " + n + ")");

                while (rs.next()) {
                    int qId = rs.getInt("question_id");
                    String qText = rs.getString("text");
                    qids.add(qId);

                    // fetch options for this question and build single message
                    PreparedStatement ps2 = conn.prepareStatement("SELECT option_char, text FROM ans WHERE question_id=? ORDER BY option_char ASC");
                    ps2.setInt(1, qId);
                    ResultSet rs2 = ps2.executeQuery();

                    StringBuilder sb = new StringBuilder();
                    while (rs2.next()) {
                        String optionChar = rs2.getString("option_char");
                        String text = rs2.getString("text");
                        sb.append(optionChar).append(") ").append(text).append("\n");
                    }
                    rs2.close();
                    ps2.close();
                    
                    // send QID message
                    String toSend = "QID:" + qId + "\n" + qText + "\n" + sb.toString().trim();
                    client.send(toSend);
                }
                rs.close();
                ps.close();
                
                // store session mapping
                sessions.put(sessionId, qids);
                return;
            }

            // Client asking for results for session
            if (message.startsWith("REQ_RESULTS|")) {
                String[] parts = message.split("\\|", 2);
                String sessionId = parts[1].trim();
                List<Integer> qids = sessions.get(sessionId);
                if (qids == null) {
                    client.send("ERROR|Unknown session");
                    return;
                }
                // For each qid, find correct answer and send as: RESULT:<qid>:<correctChar>
                for (int qid : qids) {
                    PreparedStatement ps = conn.prepareStatement("SELECT correct_answer FROM qst WHERE question_id=?");
                    ps.setInt(1, qid);
                    ResultSet rs = ps.executeQuery();
                    if (rs.next()) {
                        String correct = rs.getString("correct_answer");
                        client.send("RESULT:" + qid + ":" + correct);
                    }
                    rs.close();
                    ps.close();
                }
                client.send("RESULTS_DONE");
                return;
            }

            // Handle final score submission
            if (message.startsWith("FINAL_SCORE|")) {
                String[] parts = message.split("\\|");
                if (parts.length >= 4) {
                    String sessionId = parts[1].trim();
                    int score = Integer.parseInt(parts[2].trim());
                    int total = Integer.parseInt(parts[3].trim());
                    
                    String userName = sessionNames.getOrDefault(sessionId, clientNames.getOrDefault(client, "Unknown User"));
                    
                    // Store result
                    sessionResults.put(sessionId, new ResultData(userName, score, total));
                    
                    System.out.println("📊 QUIZ COMPLETED");
                    System.out.println("   User: " + userName);
                    System.out.println("   Session: " + sessionId);
                    System.out.println("   Score: " + score + "/" + total + " (" + String.format("%.1f", (score*100.0/total)) + "%)");
                    System.out.println("   ---");
                    
                    // Print all results summary
                    printAllResults();
                }
                return;
            }

            // Otherwise treat as an answer submission: expected "questionId|OptionChar"
            String[] parts = message.split("\\|");
            if (parts.length < 2) {
                client.send("ERROR|Bad message format. Use questionId|OptionChar or REQ_RANDOM|N or REQ_RESULTS|sessionId");
                return;
            }

            int questionId;
            try {
                questionId = Integer.parseInt(parts[0].trim());
            } catch (NumberFormatException ex) {
                client.send("ERROR|Bad question id");
                return;
            }
            String answer = parts[1].trim();

            String userName = clientNames.getOrDefault(client, "Unknown User");

            // check correctness
            PreparedStatement ps = conn.prepareStatement("SELECT correct_answer FROM qst WHERE question_id=?");
            ps.setInt(1, questionId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                String correct = rs.getString("correct_answer");
                if (correct.equalsIgnoreCase(answer)) {
                    System.out.println("✅ " + userName + " answered Question " + questionId + " correctly: " + answer);
                    client.send("✅ Correct!");
                } else {
                    System.out.println(" " + userName + " answered Question " + questionId + " incorrectly: " + answer + " (correct: " + correct + ")");
                    client.send(" Wrong! Correct answer: " + correct);
                }
            } else {
                client.send(" Question not found!");
            }
            rs.close();
            ps.close();

        } catch (Exception e) {
            e.printStackTrace();
            client.send("Error processing your request.");
        }
    }

    private void printAllResults() {
        if (sessionResults.isEmpty()) {
            return;
        }
        
        System.out.println("\n" + "=".repeat(60));
        System.out.println("📈 ALL QUIZ RESULTS SUMMARY");
        System.out.println("=".repeat(60));
        
        List<Map.Entry<String, ResultData>> sortedResults = new ArrayList<>(sessionResults.entrySet());
        sortedResults.sort((a, b) -> Long.compare(b.getValue().timestamp, a.getValue().timestamp));
        
        int count = 1;
        for (Map.Entry<String, ResultData> entry : sortedResults) {
            ResultData data = entry.getValue();
            double percentage = (data.score * 100.0) / data.totalQuestions;
            
            System.out.println(count + ". " + data.userName);
            System.out.println("   Score: " + data.score + "/" + data.totalQuestions + 
                             " (" + String.format("%.1f", percentage) + "%)");
            System.out.println("   Session: " + entry.getKey());
            System.out.println("   Time: " + new java.util.Date(data.timestamp));
            System.out.println();
            count++;
        }
        System.out.println("=".repeat(60) + "\n");
    }

    @Override
    public void onClose(WebSocket client, int code, String reason, boolean remote) {
        String userName = clientNames.getOrDefault(client, "Unknown User");
        System.out.println("🔴 Client disconnected: " + userName + " - " + reason + " (" + client.getRemoteSocketAddress() + ")");
        clientNames.remove(client);
    }

    @Override
    public void onError(WebSocket client, Exception ex) {
        System.err.println(" WebSocket Error:");
        ex.printStackTrace();
    }

    @Override
    public void onStart() {
        System.out.println("🚀 Quiz WebSocket Server started successfully on port " + getPort());
        System.out.println("📋 Server ready to accept quiz sessions...\n");
    }

    public static void main(String[] args) throws Exception {
        // start WS server on 12345
        QuizServer server = new QuizServer(12345);
        server.start();
        System.out.println("✅ Smart Quiz Server running on port 12345");

        // start HTTP endpoint for upload on port 12346
        HttpServer http = HttpServer.create(new InetSocketAddress(12346), 0);
        http.createContext("/uploadVideo", new UploadHandler());
        http.setExecutor(Executors.newCachedThreadPool());
        http.start();
        System.out.println("✅ HTTP upload endpoint started at: http://localhost:12346/uploadVideo");
        System.out.println("\n" + "=".repeat(60));
        System.out.println("🎓 Smart Quiz Server is ready!");
        System.out.println("=".repeat(60) + "\n");
    }
}