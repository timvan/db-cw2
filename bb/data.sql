INSERT INTO Person (name, username, stuId) VALUES ('StudentUser', 'stdusr1', 'S1');
INSERT INTO Person (name, username) VALUES ('PublicUser', 'pubusr1');
INSERT INTO Person (name, username, stuId) VALUES ('StudentUser2', 'stdusr2', 'S2');
INSERT INTO Person (name, username, stuId) VALUES ('StudentUser3', 'stdusr3', 'S3');

INSERT INTO Forum (title) VALUES ('Forum1');
SELECT SLEEP(1);
INSERT INTO Forum (title) VALUES ('Forum2');
SELECT SLEEP(1);
INSERT INTO Forum (title) VALUES ('Forum3');

INSERT INTO Topic (title, authorId, forumId) VALUES ('Forum2Topic1', 1, 2);
SELECT SLEEP(1);
INSERT INTO Topic (title, authorId, forumId) VALUES ('Forum2Topic2', 2, 2);
SELECT SLEEP(1);
INSERT INTO Topic (title, authorId, forumId) VALUES ('Forum3Topic1', 2, 3);
SELECT SLEEP(1);
INSERT INTO Topic (title, authorId, forumId) VALUES ('Forum3Topic2', 1, 3);

INSERT INTO Post (authorId, content, topicId) VALUES (1, "Post 1", 1);
SELECT SLEEP(1);
INSERT INTO Post (authorId, content, topicId) VALUES (2, "Post 2", 1);
SELECT SLEEP(1);
INSERT INTO Post (authorId, content, topicId) VALUES (1, "Post 1", 2);
SELECT SLEEP(1);
INSERT INTO Post (authorId, content, topicId) VALUES (1, "Post 2", 2);
SELECT SLEEP(1);
INSERT INTO Post (authorId, content, topicId) VALUES (2, "Post 3", 2);
SELECT SLEEP(1);
INSERT INTO Post (authorId, content, topicId) VALUES (1, "Post 1", 3);
SELECT SLEEP(1);
INSERT INTO Post (authorId, content, topicId) VALUES (2, "Post 2", 3);
SELECT SLEEP(1);
INSERT INTO Post (authorId, content, topicId) VALUES (1, "Post 1", 4);
SELECT SLEEP(1);
INSERT INTO Post (authorId, content, topicId) VALUES (2, "Post 2", 4);

INSERT INTO LikeTopic (personId, topicId) VALUES (1, 1);
SELECT SLEEP(1);
INSERT INTO LikeTopic (personId, topicId) VALUES (2, 1);
SELECT SLEEP(1);
INSERT INTO LikeTopic (personId, topicId) VALUES (1, 2);

INSERT INTO LikePost (personId, postId) VALUES (1, 2);
INSERT INTO LikePost (personId, postId) VALUES (3, 2);
INSERT INTO LikePost (personId, postId) VALUES (4, 2);
INSERT INTO LikePost (personId, postId) VALUES (1, 1);
INSERT INTO LikePost (personId, postId) VALUES (2, 1);
INSERT INTO LikePost (personId, postId) VALUES (1, 3);
INSERT INTO LikePost (personId, postId) VALUES (2, 3);
INSERT INTO LikePost (personId, postId) VALUES (3, 4);
INSERT INTO LikePost (personId, postId) VALUES (4, 4);

