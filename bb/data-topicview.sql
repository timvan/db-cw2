INSERT INTO Person (name, username, stuId) VALUES ('su1', 'u1', 'u1');
INSERT INTO Person (name, username, stuId) VALUES ('su2', 'u2', 'u2');
INSERT INTO Person (name, username, stuId) VALUES ('su3', 'u3', 'u3');
INSERT INTO Person (name, username, stuId) VALUES ('su4', 'u4', 'u4');
INSERT INTO Person (name, username, stuId) VALUES ('su5', 'u5', 'u5');



INSERT INTO Forum (title) VALUES ('A Forum');
SELECT SLEEP(1);


-- user 1 has likes on topic and posts
INSERT INTO Topic (title, authorId, forumId) VALUES ('f1', 1, 1);
INSERT INTO Post (authorId, content, topicId) VALUES (1, "post id 1", 1);
INSERT INTO LikeTopic (personId, topicId) VALUES (3, 1);
INSERT INTO LikePost (personId, postId) VALUES (2, 1);
INSERT INTO LikePost (personId, postId) VALUES (3, 1);

-- user 2 has likes on topics and not on posts
INSERT INTO Topic (title, authorId, forumId) VALUES ('f2', 2, 1);
INSERT INTO Post (authorId, content, topicId) VALUES (2, "post id 2", 2);
INSERT INTO LikeTopic (personId, topicId) VALUES (1, 2);
INSERT INTO LikeTopic (personId, topicId) VALUES (4, 2);

-- user 3 has no likes on topics but likes on posts
INSERT INTO Topic (title, authorId, forumId) VALUES ('f3', 3, 1);
INSERT INTO Post (authorId, content, topicId) VALUES (3, "post id 3", 3);
INSERT INTO LikePost (personId, postId) VALUES (1, 3);
INSERT INTO LikePost (personId, postId) VALUES (4, 3);

-- user 4 has no likes
INSERT INTO Topic (title, authorId, forumId) VALUES ('f4', 4, 1);
INSERT INTO Post (authorId, content, topicId) VALUES (4, "post id 4", 4);

-- user 5 has content