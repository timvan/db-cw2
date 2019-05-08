INSERT INTO Person (name, username, stuId) VALUES ('StudentUser', 'stdusr1', 'S1');
INSERT INTO Person (name, username) VALUES ('PublicUser', 'pubusr1');
INSERT INTO Person (name, username, stuId) VALUES ('StudentUser2', 'stdusr2', 'S2');
INSERT INTO Person (name, username, stuId) VALUES ('StudentUser3', 'stdusr3', 'S3');

INSERT INTO Forum (title) VALUES ('A Forum');
SELECT SLEEP(1);
INSERT INTO Forum (title) VALUES ('Another Forum');

INSERT INTO Topic (title, authorId, forumId) VALUES ('A Topic on A Forum', 1, 2);
SELECT SLEEP(1);
INSERT INTO Topic (title, authorId, forumId) VALUES ('Another Topic on A Forum', 2, 2);
SELECT SLEEP(1);

INSERT INTO Post (authorId, content, topicId) VALUES (1, "A Post on A Topic on A Forum", 1);
SELECT SLEEP(1);
INSERT INTO Post (authorId, content, topicId) VALUES (2, "Another Post on A Topic on A Forum", 1);
SELECT SLEEP(1);

INSERT INTO Post (authorId, content, topicId) VALUES (1, "A Post on A Topic on Another Forum", 2);
SELECT SLEEP(1);
INSERT INTO Post (authorId, content, topicId) VALUES (2, "Another Post on A Topic on Another Forum", 2);
SELECT SLEEP(1);

INSERT INTO LikeTopic (personId, topicId) VALUES (1, 1);
SELECT SLEEP(1);
INSERT INTO LikeTopic (personId, topicId) VALUES (2, 1);
SELECT SLEEP(1);
INSERT INTO LikeTopic (personId, topicId) VALUES (3, 1);
SELECT SLEEP(1);
INSERT INTO LikeTopic (personId, topicId) VALUES (4, 2);
SELECT SLEEP(1);
INSERT INTO LikeTopic (personId, topicId) VALUES (1, 2);

INSERT INTO LikePost (personId, postId) VALUES (1, 1);
INSERT INTO LikePost (personId, postId) VALUES (2, 1);
INSERT INTO LikePost (personId, postId) VALUES (3, 1);
INSERT INTO LikePost (personId, postId) VALUES (4, 1);
