DROP TABLE IF EXISTS Post;
DROP TABLE IF EXISTS Topic;
DROP TABLE IF EXISTS Forum;
DROP TABLE IF EXISTS Person;

CREATE TABLE Person (
  id INTEGER PRIMARY KEY AUTO_INCREMENT
  , name VARCHAR(100) NOT NULL
  , username VARCHAR(10) NOT NULL UNIQUE
  , stuId VARCHAR(10) NULL
);

CREATE TABLE Forum (
  id INTEGER PRIMARY KEY AUTO_INCREMENT
  , title VARCHAR(100) NOT NULL
);

CREATE TABLE Topic (
  id INTEGER PRIMARY KEY AUTO_INCREMENT
  , title VARCHAR(100) NOT NULL
  , authorId INTEGER NOT NULL
  , forumId INTEGER NOT NULL
  , FOREIGN KEY (authorId) REFERENCES Person(id)
  , FOREIGN KEY (forumId) REFERENCES Forum(id)
);

CREATE TABLE Post (
  id INTEGER PRIMARY KEY AUTO_INCREMENT
  , postedAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP
  , authorId INTEGER NOT NULL
  , content TEXT NOT NULL
  , topicId INTEGER NOT NULL
  , likes INTEGER DEFAULT 0
  , FOREIGN KEY (authorId) REFERENCES Person(id)
  , FOREIGN KEY (topicId) REFERENCES Topic(id)
);
