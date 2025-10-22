-- Create the database
CREATE DATABASE IF NOT EXISTS `smart_quiz`;
USE `smart_quiz`;

-- Drop tables if exist (drop dependent table first)
DROP TABLE IF EXISTS `ans`;
DROP TABLE IF EXISTS `qst`;

-- Create questions table
CREATE TABLE `qst` (
    `question_id` INT AUTO_INCREMENT PRIMARY KEY,
    `text` TEXT NOT NULL,
    `correct_answer` VARCHAR(1) NOT NULL
);

-- Create answers table
CREATE TABLE `ans` (
    `answer_id` INT AUTO_INCREMENT PRIMARY KEY,
    `question_id` INT NOT NULL,
    `text` TEXT NOT NULL,
    `option_char` VARCHAR(1) NOT NULL,
    FOREIGN KEY (`question_id`) REFERENCES `qst`(`question_id`) ON DELETE CASCADE
);

-- Insert 50 questions
INSERT INTO `qst` (`text`, `correct_answer`) VALUES
('What is 1 + 1?', 'B'),
('What is the capital of France?', 'A'),
('Which language runs in a browser?', 'C'),
('HTTP stands for?', 'A'),
('Which planet is known as the Red Planet?', 'D'),
('What is 5 * 6?', 'B'),
('Which animal is known as King of Jungle?', 'B'),
('Who wrote Hamlet?', 'A'),
('Which gas do plants absorb?', 'D'),
('What is 10 / 2?', 'B'),
('Which ocean is the largest?', 'B'),
('Who discovered gravity?', 'A'),
('Which year did World War II end?', 'C'),
('What is H2O?', 'A'),
('Which element has symbol Na?', 'C'),
('Who painted Mona Lisa?', 'A'),
('Which is the smallest continent?', 'D'),
('What is 7 + 8?', 'B'),
('Which planet has rings?', 'C'),
('What is the freezing point of water?', 'A'),
('Which country is known as Land of Rising Sun?', 'D'),
('Who invented the telephone?', 'B'),
('What is 12 * 12?', 'C'),
('Which is the largest mammal?', 'A'),
('Who wrote 1984?', 'D'),
('Which is the hardest natural substance?', 'B'),
('What is 15 - 7?', 'C'),
('Which planet is closest to the Sun?', 'A'),
('Who painted Starry Night?', 'D'),
('Which gas do humans exhale?', 'B'),
('What is 9 * 9?', 'C'),
('Which is the largest desert?', 'A'),
('Who developed the theory of relativity?', 'D'),
('What is 20 / 4?', 'B'),
('Which country invented pizza?', 'C'),
('Who discovered penicillin?', 'A'),
('Which is the smallest prime number?', 'D'),
('What is the square root of 64?', 'B'),
('Which is the largest bird?', 'C'),
('Who wrote The Odyssey?', 'A');

-- Insert answers
INSERT INTO `ans` (`question_id`, `text`, `option_char`) VALUES
(1,' 0','A'),(1,' 2','B'),(1,'3','C'),(1,' 1','D'),
(2,'  Paris','A'),(2,'   London','B'),(2,'   Berlin','C'),(2,'   Madrid','D'),
(3,'  Java','A'),(3,'   C++','B'),(3,'   JavaScript','C'),(3,'   Python','D'),
(4,'  HyperText Transfer Protocol','A'),(4,'   HighText Transfer Protocol','B'),(4,'   Hyperlink Transfer Protocol','C'),(4,'   None','D'),
(5,'  Earth','A'),(5,'   Venus','B'),(5,'   Jupiter','C'),(5,'   Mars','D'),
(6,'  25','A'),(6,'   30','B'),(6,'   35','C'),(6,'   36','D'),
(7,'  Tiger','A'),(7,'   Lion','B'),(7,'   Elephant','C'),(7,'   Leopard','D'),
(8,'  William Shakespeare','A'),(8,'   Charles Dickens','B'),(8,'   J.K. Rowling','C'),(8,'   Jane Austen','D'),
(9,'  Oxygen','A'),(9,'   Hydrogen','B'),(9,'   Nitrogen','C'),(9,'   Carbon Dioxide','D'),
(10,'  4','A'),(10,'   5','B'),(10,'   3','C'),(10,'   2','D'),
(11,'  Atlantic','A'),(11,'   Pacific','B'),(11,'   Indian','C'),(11,'   Arctic','D'),
(12,'  Isaac Newton','A'),(12,'   Albert Einstein','B'),(12,'   Galileo','C'),(12,'   Nikola Tesla','D'),
(13,'  1944','A'),(13,'   1943','B'),(13,'   1945','C'),(13,'   1946','D'),
(14,'  Water','A'),(14,'   Hydrogen','B'),(14,'   Oxygen','C'),(14,'   Helium','D'),
(15,' calcium','A'),(15,'   Potassium','B'),(15,'  sodium','C'),(15,'   Magnesium','D'),
(16,'  Leonardo da Vinci','A'),(16,'   Pablo Picasso','B'),(16,'   Van Gogh','C'),(16,'   Michelangelo','D'),
(17,'  Australia','A'),(17,'   Europe','B'),(17,'   Asia','C'),(17,'   Antarctica','D'),
(18,'  19','A'),(18,'   15','B'),(18,'   16','C'),(18,'   17','D'),
(19,'  Mars','A'),(19,'   Jupiter','B'),(19,'  Saturn','C'),(19,'   Venus','D'),
(20,'  0°C','A'),(20,'   100°C','B'),(20,'   50°C','C'),(20,'   -1°C','D'),
(21,'  Thailand','A'),(21,'   China','B'),(21,'   India','C'),(21,'japan','D'),
(22,' Thomas Edison','A'),(22,'    Alexander Graham Bell','B'),(22,'   Nikola Tesla','C'),(22,'   Isaac Newton','D'),
(23,'  169','A'),(23,'   121','B'),(23,'   144','C'),(23,'   196','D'),
(24,'  Blue Whale','A'),(24,'   Elephant','B'),(24,'   Giraffe','C'),(24,'   Hippopotamus','D'),
(25,' Mark Twain','A'),(25,'   Aldous Huxley','B'),(25,'   J.K. Rowling','C'),(25,'   George Orwell','D'),
(26,'  Quartz','A'),(26,' Diamond','B'),(26,'   Graphite','C'),(26,'   Gold','D'),
(27,'  6','A'),(27,'   7','B'),(27,'   8','C'),(27,'   5','D'),
(28,'  Mercury','A'),(28,'   Venus','B'),(28,'   Mars','C'),(28,'   Earth','D'),
(29,'  Michelangelo','A'),(29,'   Pablo Picasso','B'),(29,'   Leonardo da Vinci','C'),(29,'  Vincent Van Gogh  ','D'),
(30,' Oxygen','A'),(30,'  Carbon Dioxide','B'),(30,'   Nitrogen','C'),(30,'   Hydrogen','D'),
(31,'  100','A'),(31,'   121','B'),(31,' 81','C'),(31,'   64','D'),
(32,'  Sahara','A'),(32,'   Gobi','B'),(32,'   Kalahari','C'),(32,'   Atacama','D'),
(33,' Nikhola Tesla','A'),(33,'   Isaac Newton','B'),(33,'   Galileo','C'),(33,'  Albert Einstein','D'),
(34,'  4','A'),(34,'   5','B'),(34,'   6','C'),(34,'   3','D'),
(35,'  Germany','A'),(35,'   France','B'),(35,'   Italy','C'),(35,'   Australia','D'),
(36,'  Alexander Fleming','A'),(36,'   Louis Pasteur','B'),(36,'   Robert Koch','C'),(36,'   Joseph Lister','D'),
(37,'  2','A'),(37,'   3','B'),(37,'   5','C'),(37,'   1','D'),
(38,'  6','A'),(38,'   8','B'),(38,'   7','C'),(38,'   9','D'),
(39,'  peacock','A'),(39,'   Eagle','B'),(39,' Ostrich','C'),(39,'   Penguin','D'),
(40,'  Homer','A'),(40,'   Virgil','B'),(40,'   Plato','C'),(40,'   Socrates','D');
