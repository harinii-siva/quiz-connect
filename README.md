# Smart Quiz - Video Supervised Quiz Application

A real-time quiz application with video supervision to ensure academic integrity during online assessments. This application randomly selects questions from a database and records the user's webcam during the quiz session.

## Features

- **Video Supervision**: Records user's webcam during the quiz to prevent cheating
- **Random Questions**: Automatically selects random questions from a database
- **Real-time Scoring**: Provides immediate feedback on answers
- **Multiple Quiz Lengths**: Choose between 10, 20, or 30 questions
- **Results Summary**: Detailed results showing correct vs. user answers
- **Session Tracking**: Unique session IDs for each quiz attempt

## Project Structure

```
quiz-connect/
├── index.html          # Frontend quiz interface
├── QuizServer.java     # WebSocket server and HTTP upload handler
├── smart_quiz.sql      # Database schema and sample questions
├── *.jar              # Required Java libraries
└── README.md          # This file
```

## Prerequisites

- Java JDK 8 or higher
- MySQL database server
- Web browser with webcam access

## Setup

### 1. Database Configuration

1. Create a MySQL database using the provided schema:
   ```sql
   CREATE DATABASE IF NOT EXISTS `smart_quiz`;
   USE `smart_quiz`;
   ```
2. Execute the `smart_quiz.sql` file to create tables and populate sample questions:
   ```bash
   mysql -u root -p smart_quiz < smart_quiz.sql
   ```

### 2. Java Dependencies

The following JAR files are required and included in the project:
- Java-WebSocket-1.5.6.jar
- mysql-connector-j-8.0.33.jar (or mysql-connector-j-9.4.0.jar)
- slf4j-api-1.7.36.jar
- slf4j-simple-1.7.36.jar

### 3. Compiling the Server

Compile the Java server:
```bash
javac -cp ".;Java-WebSocket-1.5.6.jar;mysql-connector-j-8.0.33.jar;slf4j-api-1.7.36.jar" QuizServer.java
```

## Running the Application

### 1. Start the Server

Run the compiled server:
```bash
java -cp ".;Java-WebSocket-1.5.6.jar;mysql-connector-j-8.0.33.jar;slf4j-api-1.7.36.jar;slf4j-simple-1.7.36.jar" QuizServer
```

The server will start on two ports:
- WebSocket server: `ws://localhost:12345`
- HTTP upload endpoint: `http://localhost:12346/uploadVideo`

### 2. Open the Quiz Interface

Open `index.html` in a web browser to access the quiz interface.

## Sample Output

When running the application, you can expect output similar to the following in your terminal console:

```
Connected to Database: smart_quiz
 Smart Quiz Server running on port 12345
 Quiz WebSocket Server started successfully on port 12345
 Server ready to accept quiz sessions...

 HTTP upload endpoint started at: http://localhost:12346/uploadVideo

============================================================
 Smart Quiz Server is ready!
============================================================

 New client connected: /127.0.0.1:50138
 Message from client: NAME|hari
 Client registered as: hari (/127.0.0.1:50138)
 Message from client: REQ_RANDOM|10
 Starting quiz session for: hari (Session: 1ff7a387-3140-46d1-9d39-3bfddca3078c, Questions: 10)
 Message from client: 5|D
 hari answered Question 5 correctly: D
 Message from client: 24|A
 hari answered Question 24 correctly: A
 Message from client: REQ_RESULTS|1ff7a387-3140-46d1-9d39-3bfddca3078c
 Message from client: FINAL_SCORE|1ff7a387-3140-46d1-9d39-3bfddca3078c|0|10
 QUIZ COMPLETED
   User: hari
   Session: 1ff7a387-3140-46d1-9d39-3bfddca3078c
   Score: 2/10 (20.0%)
   ---

============================================================
 ALL QUIZ RESULTS SUMMARY
============================================================
1. hari
   Score: 2/10 (20.0%)
   Session: 1ff7a387-3140-46d1-9d39-3bfddca3078c
   Time: Wed Oct 22 16:22:20 IST 2025

============================================================

 Client disconnected: hari -  (/127.0.0.1:50138)
 New client connected: /127.0.0.1:7847
 Message from client: NAME|afrin
 Client registered as: afrin (/127.0.0.1:7847)
 Message from client: REQ_RANDOM|10
 Starting quiz session for: afrin (Session: 482244b3-112e-4a57-b21f-9dd66005393e, Questions: 10)
 Message from client: 31|C
 afrin answered Question 31 correctly: C
 Message from client: REQ_RESULTS|482244b3-112e-4a57-b21f-9dd66005393e
 Message from client: FINAL_SCORE|482244b3-112e-4a57-b21f-9dd66005393e|1|10
 QUIZ COMPLETED
   User: afrin
   Session: 482244b3-112e-4a57-b21f-9dd66005393e
   Score: 1/10 (10.0%)
   ---

============================================================
 ALL QUIZ RESULTS SUMMARY
============================================================
1. afrin
   Score: 1/10 (10.0%)
   Session: 482244b3-112e-4a57-b21f-9dd66005393e
   Time: Wed Oct 22 16:29:53 IST 2025

2. hari
   Score: 2/80 (20.0%)
   Session: 1ff7a387-3140-46d1-9d39-3bfddca3078c
   Time: Wed Oct 22 16:22:20 IST 2025

============================================================
```

This output shows:
- Server startup messages
- Client connections and registrations
- Quiz session initiations with unique session IDs
- Real-time answer feedback (correct/incorrect)
- Quiz completion with scores
- Results summary showing all quiz attempts (pulled from the `results` database table)
- Client disconnections

When a video is uploaded, you'll see additional confirmation messages in the console showing the path where the video was saved.

## How It Works

1. **User Registration**: Enter your name to begin
2. **Quiz Setup**: Choose the number of questions (10, 20, or 30)
3. **Webcam Access**: The application requests access to your webcam
4. **Question Delivery**: Questions are randomly selected from the database and sent via WebSocket
5. **Video Recording**: Your webcam is recorded automatically throughout the quiz session
6. **Answer Submission**: Answers are sent to the server in real-time as you select them
7. **Results**: After completing the quiz, results are displayed with correct answers
8. **Result Storage**: Quiz results are automatically saved to the `results` table in the database
9. **Video Upload**: When you click "Finish", the recorded video is automatically uploaded to the server in the background

## Video Uploads

Videos are automatically uploaded to the HTTP endpoint: `http://localhost:12346/uploadVideo` when a user completes a quiz session. The upload process happens in the background without any manual intervention required from the user.

- Videos are stored in the `uploads/` directory (automatically created when the first video is uploaded)
- Filename format: `session_<session_id>_<timestamp>.webm`
- Each video is associated with a unique quiz session ID
- You can check uploaded videos by looking in the uploads directory (after it's created)
- The server console will display confirmation when videos are saved

## Database Schema

### Questions Table (`qst`)
```sql
CREATE TABLE `qst` (
    `question_id` INT AUTO_INCREMENT PRIMARY KEY,
    `text` TEXT NOT NULL,
    `correct_answer` VARCHAR(1) NOT NULL
);
```

### Answers Table (`ans`)
```sql
CREATE TABLE `ans` (
    `answer_id` INT AUTO_INCREMENT PRIMARY KEY,
    `question_id` INT NOT NULL,
    `text` TEXT NOT NULL,
    `option_char` VARCHAR(1) NOT NULL,
    FOREIGN KEY (`question_id`) REFERENCES `qst`(`question_id`) ON DELETE CASCADE
);
```

### Results Table (`results`)
```sql
CREATE TABLE `results` (
    `id` INT AUTO_INCREMENT PRIMARY KEY,
    `session_id` VARCHAR(100),
    `user_name` VARCHAR(100),
    `score` INT,
    `total_questions` INT,
    `percentage` DECIMAL(5,2),
    `timestamp` DATETIME DEFAULT CURRENT_TIMESTAMP
);
```

## Troubleshooting

1. **Database Connection Issues**:
   - Verify MySQL is running
   - Check credentials in QuizServer.java
   - Ensure the `smart_quiz` database exists

2. **Webcam Not Working**:
   - Check browser permissions
   - Ensure no other application is using the webcam
   - Verify your browser supports MediaRecorder API

3. **WebSocket Connection Failed**:
   - Confirm the server is running
   - Check that port 12345 is not blocked by firewall
   - Verify the server address in index.html

4. **Video Upload Issues**:
   - Check that the HTTP upload endpoint is running on port 12346
   - Verify that no firewall is blocking port 12346
   - Ensure you click the "Finish" button to trigger the upload
   - Check browser console for JavaScript errors during upload
   - Verify that the session ID is being properly passed to the upload endpoint
