package uk.ac.bris.cs.databases.cwk2;

import java.sql.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import uk.ac.bris.cs.databases.api.*;

/**
 *
 * @author csxdb
 */
public class API implements APIProvider {

    private final Connection c;
    private final int longStringLength = 100; //Database varchar length
    private final int shortStringLength = 10; //Database varchar length

    public API(Connection c) {
        this.c = c;
    }

    /* A.1 */

    @Override
    public Result<Map<String, String>> getUsers() {

        String sql = "SELECT username, name FROM Person";
        try (PreparedStatement ps = c.prepareStatement (sql)) {
            ResultSet rs = ps.executeQuery ();
            HashMap<String, String> users = new HashMap<> ();

            while (rs.next ()) {
                String username = rs.getString ("username");
                String name = rs.getString ("name");
                users.put (username, name);
            }
            return Result.success (users);

        } catch (SQLException e) {
            return Result.fatal (e.getMessage());
        }
    }

    @Override
    public Result<PersonView> getPersonView(String username) {
        if (username == null || username.trim().isEmpty()) { return Result.failure ("getPersonView: Username cannot be empty"); }

        String sql = "SELECT name, stuId FROM Person WHERE username = ?";
        try (PreparedStatement ps = c.prepareStatement (sql)) {
            ps.setString (1, username);
            ResultSet rs = ps.executeQuery ();

            if (!rs.next ()) {
                return Result.failure ("getPersonView: Username does not match any existing user");
            }
            String name = rs.getString ("name");
            String studentId = rs.getString ("stuId");
            if(studentId == null) studentId = "";

            return Result.success ( new PersonView (name, username, studentId));
        } catch (SQLException e){
            return Result.fatal (e.getMessage());
        }
    }

    @Override
    public Result addNewPerson(String name, String username, String studentId) {
        if (name == null || name.trim().isEmpty()) { return Result.failure ("addNewPerson: Name cannot be empty"); }
        if (name.length() > longStringLength) { return Result.failure("addNewPerson: Name too long"); }
        if (username == null || username.trim().isEmpty()) { return Result.failure ("addNewPerson: Username cannot be empty"); }
        if (username.length() > shortStringLength) { return Result.failure("addNewPerson: Username too long");}
        if (studentId != null && studentId.trim().isEmpty()) { return Result.failure ("addNewPerson: Student Id cannot be empty"); }
        if (studentId != null && studentId.length() > shortStringLength) { return Result.failure("addNewPerson: Student ID too long");}
        name = name.trim();
        username = username.trim();

        Result usernameResult = usernameExists(username);
        if (usernameResult.isSuccess()) return Result.failure ("addNewPerson: username already exist");
        if (usernameResult.isFatal()) return usernameResult;

        String sql = "INSERT INTO Person (name, username, stuId) "
                    + "VALUES (?, ?, ?)";

        try (PreparedStatement ps = c.prepareStatement (sql)) {
            ps.setString (1, name);
            ps.setString (2, username);
            ps.setString (3, studentId);

            // TODO - here we check that the User has been created and throw Error if not???
            if(ps.executeUpdate () != 1) throw new SQLException();

            c.commit ();
            return Result.success ();

        } catch (SQLException e) {
            try {
                c.rollback ();
                return Result.fatal (e.getMessage());
            } catch (SQLException f) {
                return Result.fatal (f.getMessage());
            }
        }
    }

    /* A.2 */

    @Override
    public Result<List<SimpleForumSummaryView>> getSimpleForums() {

        String sql = "SELECT id, title FROM Forum ORDER BY title ASC";

        try (PreparedStatement ps = c.prepareStatement (sql)) {
            ResultSet rs = ps.executeQuery ();

            ArrayList<SimpleForumSummaryView> forums = new ArrayList<> ();
            while (rs.next ()) {
                int id = rs.getInt ("id");
                String title = rs.getString ("title");
                forums.add (new SimpleForumSummaryView (id, title));
            }
            return Result.success (forums);
        }
        catch (SQLException e) {
            return Result.fatal (e.getMessage ());
        }
    }

    @Override
    public Result createForum(String title) {
        if (title == null || title.trim().isEmpty()) return Result.failure ("createForum: Title cannot be empty");
        if (title.length() > longStringLength) return Result.failure("createForum: Title too long");

        // check forum title does not already exist
        Result forumExists = forumTitleExists (title);
        if (forumExists.isSuccess ()) return Result.failure("createForum: Forum title already exists");
        if (forumExists.isFatal ()) return forumExists;

        String sql = "INSERT INTO Forum (title) VALUES (?)";
        try (PreparedStatement ps = c.prepareStatement (sql)) {
            ps.setString (1, title);

            // TODO - here we check that the Forum has been created and throw Error if not???
            if(ps.executeUpdate () != 1) throw new SQLException();

            c.commit();
            return Result.success ();

        } catch (SQLException e) {
            try {
                c.rollback();
                return Result.fatal (e.getMessage ());
            } catch (SQLException f) {
                return Result.fatal (f.getMessage ());
            }
        }
    }

    /* A.3 */

    @Override
    public Result<List<ForumSummaryView>> getForums() {

        String sql = "SELECT Forum.id, Forum.title, Topic.title, Topic.id, Post.postedAt" +
                " FROM Forum" +
                " LEFT JOIN Topic" +
                " ON Forum.id = Topic.forumId" +
                " LEFT JOIN Post ON Topic.id = Post.topicId" +
                " ORDER BY Forum.title ASC, Post.postedAt DESC";

        try (PreparedStatement ps = c.prepareStatement (sql)) {
            ResultSet rs = ps.executeQuery ();
            ArrayList<ForumSummaryView> forums = new ArrayList<> ();

            int currentForumId = -1;
            while (rs.next ()) {

                int forumId = rs.getInt ("Forum.id");
                if (forumId != currentForumId) {
                    int topicId = rs.getInt("Topic.id");
                    String forumTitle = rs.getString ("Forum.title");
                    String topicTitle = rs.getString("Topic.title");
                    SimpleTopicSummaryView topic = newSimpleTopicSummaryView (topicId, forumId, topicTitle);
                    forums.add (new ForumSummaryView (forumId, forumTitle, topic));
                    currentForumId = forumId;
                }
            }
            return Result.success (forums);
        }
        catch (SQLException e) {
            return Result.fatal (e.getMessage ());
        }
    }

    @Override
    public Result<ForumView> getForum(int id) {

        String sql = "SELECT Forum.id, Forum.title, Topic.id, Topic.title " +
                " FROM Forum" +
                " LEFT JOIN Topic" +
                " ON Forum.id = Topic.forumId" +
                " WHERE Forum.id = ?";

        try (PreparedStatement ps = c.prepareStatement (sql)) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery ();

            if (!rs.next()) return Result.failure("getForum: Forum does not exist");
            int forumId = rs.getInt ("Forum.id");
            String forumTitle = rs.getString ("Forum.title");
            ArrayList<SimpleTopicSummaryView> topics = new ArrayList<> ();

            do {
                int topicId = rs.getInt ("Topic.id");
                String topicTitle = rs.getString ("Topic.title");
                if (topicTitle != null && topicId != 0) {
                    topics.add (new SimpleTopicSummaryView (topicId, forumId, topicTitle));
                }
            } while (rs.next ());

            return Result.success (new ForumView (forumId, forumTitle, topics));
        }
        catch (SQLException e) {
            return Result.fatal(e.getMessage());
        }
    }

    @Override
    public Result<SimpleTopicView> getSimpleTopic(int topicId) {

        // TODO - Question: NULL POINTER ERROR: should we check for null posts or do we assume that Topics all have at least one post?

        String sql = "SELECT Post.id, Post.content, Post.postedAt, Person.name, Topic.title" +
                " FROM Topic" +
                " LEFT JOIN Post" +
                " ON Topic.id = Post.topicId" +
                " LEFT JOIN Person" +
                " ON Post.authorId = Person.id" +
                " WHERE Topic.id = ?" +
                " ORDER BY Post.postedAt ASC";

        try (PreparedStatement ps = c.prepareStatement (sql)) {
            ps.setInt(1, topicId);
            ResultSet rs = ps.executeQuery ();
            ArrayList<SimplePostView> posts = new ArrayList<>();
            int postNumber = 1;

            if (!rs.next()) return Result.failure("getSimpleTopic: No topic with this id");
            String topicTitle = rs.getString("Topic.title");
            do {
                int postId = rs.getInt ("Post.id");
                String personName = rs.getString("Person.name");
                String postContent = rs.getString("Post.content");
                String postPostedAt = rs.getString("Post.postedAt");
                posts.add (new SimplePostView(postNumber++, personName, postContent, postPostedAt) );

            } while (rs.next ());


            return Result.success(new SimpleTopicView(topicId, topicTitle, posts));
        }
        catch (SQLException e) {
            return Result.fatal(e.getMessage ());
        }
    }


    @Override
    public Result<PostView> getLatestPost(int topicId) {

        String sql = "SELECT Forum.id, Topic.id, Post.id, Person.name," +
                " Person.username, Post.content, Post.postedAt, likePost.id" +
                " FROM Forum " +
                " LEFT JOIN Topic ON Topic.forumId = Forum.id" +
                " LEFT JOIN Post ON Post.topicId = Topic.id" +
                " LEFT JOIN LikePost ON Post.id = LikePost.postId" +
                " JOIN Person ON Post.authorId = Person.id" +
                " WHERE Topic.id = ?" +
                " ORDER BY Post.postedAt DESC";

        try (PreparedStatement ps = c.prepareStatement (sql)) {
            ps.setString(1, Integer.toString(topicId));
            ResultSet rs = ps.executeQuery ();

            if (!rs.next()) {
                return Result.failure("getLatestPost: No topic with this id");
            }

            int forumId = rs.getInt("Forum.id");
            String postAuthorName = rs.getString("Person.name");
            String postAuthorUserName = rs.getString("Person.username");
            String postContent = rs.getString("Post.content");
            String postPostedAt = rs.getString("Post.postedAt");
            int postLikes = 0;
            int postNumber = 0;
            int currentPostId = -1;

            do {
                // count amount of posts
                int postId =  rs.getInt ("Post.id");
                if (currentPostId != postId) {
                    currentPostId = postId;
                    postNumber++;
                }
                // count latest post likes
                if (postNumber == 1 && rs.getInt ("likePost.id") > 0) {
                    postLikes++;
                }
            } while (rs.next ());

            return Result.success(new PostView(
                    forumId,
                    topicId,
                    postNumber,
                    postAuthorName,
                    postAuthorUserName,
                    postContent,
                    postPostedAt,
                    postLikes
            ));
        }
        catch (SQLException e) {
            return Result.fatal(e.getMessage ());
        }
    }

    @Override
    public Result createPost(int topicId, String username, String text) {
        // TODO handler methods are checking text is null of equals("") therefore for DRY should this be empty
        if (text == null || text.trim().isEmpty()) { Result.failure("createPost: Text cannot be empty"); }
        if (username == null || username.trim().isEmpty()) { Result.failure("createPost: Username cannot be empty"); }
        if (username != null && username.length() > shortStringLength) { Result.failure("createPost: Username too long"); }

        // check user exists
        Result usernameResult = usernameExists(username);
        if (!usernameResult.isSuccess()){
            if (usernameResult.isFatal()) return usernameResult;
            return Result.failure ("createPost: " + usernameResult.getMessage());
        }
        int userId = (int) usernameResult.getValue();

        // check topic exists
        Result topicResult = topicExists(topicId);
        if (!topicResult.isSuccess()){
            if (topicResult.isFatal()) return topicResult;
            return Result.failure ("createPost: " + topicResult.getMessage());
        }

        String sql = "INSERT INTO Post (authorId, content, topicId) VALUES (?, ?, ?)";
        try (PreparedStatement ps = c.prepareStatement (sql)) {
            ps.setInt (1, userId);
            ps.setString (2, text);
            ps.setInt (3, topicId);

            // TODO - here we check that the Topic has been created and throw Error if not???
            if(ps.executeUpdate () != 1) throw new SQLException();

            c.commit();

        } catch (SQLException e) {
            try {
                c.rollback();
                return Result.fatal(e.getMessage());
            } catch (SQLException f) {
                return Result.fatal(f.getMessage());
            }
        }

        return Result.success ();
    }

    @Override
    public Result createTopic(int forumId, String username, String title, String text) {
        // TODO handler methods are checking text is null of equals("") therefore for DRY should this be empty
        if (username == null || username.trim().isEmpty()) { Result.failure("createTopic: Username cannot be empty"); }
        if (username != null && username.length() > shortStringLength) { Result.failure("createTopic: username too long"); }
        if (title == null || title.trim().isEmpty()) { Result.failure("createTopic: Title cannot be empty"); }
        if (title != null && title.length() > longStringLength) { Result.failure("createTopic: Title too long"); }
        if (text == null || text.trim().isEmpty()) { Result.failure("createTopic: Text cannot be empty"); }

        // TODO should topic titles be unique?? atm it's not no evidence it should be in API provider

        // check user exists
        Result usernameResult = usernameExists(username);
        if (!usernameResult.isSuccess()) {
            if (usernameResult.isFatal()) return usernameResult;
            return Result.failure ("createTopic: " + usernameResult.getMessage());
        }

        int userId = (int) usernameResult.getValue();

        // check forum exists
        Result forumResult = forumExists(forumId);
        if (!forumResult.isSuccess()) {
            if (forumResult.isFatal()) return  forumResult;
            return Result.failure("createTopic: " + forumResult.getMessage());
        }

        try {
            int newTopicId;

            // create topic
            String newTopicSql = "INSERT INTO Topic (title, authorId, forumId) VALUES (?,?,?)";
            try(PreparedStatement ps = c.prepareStatement(newTopicSql)) {
                ps.setString(1, title);
                ps.setInt(2, userId);
                ps.setInt(3, forumId);

                if(ps.executeUpdate () != 1) throw new SQLException();
                ResultSet rs = ps.getGeneratedKeys();
                if(!rs.next()) throw new SQLException();
                newTopicId = rs.getInt(1);

            } catch (SQLException e) {
                throw e;
            }

            // create post
            String newPostsql = "INSERT INTO Post (authorId, topicId, content) VALUES (?,?,?)";
            try(PreparedStatement ps = c.prepareStatement(newPostsql)) {
                ps.setInt(1, userId);
                ps.setInt(2, newTopicId);
                ps.setString(3, text);

                if(ps.executeUpdate () != 1) throw new SQLException();

            } catch (SQLException e) {
              throw e;
            }

            c.commit();
            return Result.success();

        } catch (SQLException e) {
            try {
                c.rollback();
                return Result.fatal (e.toString ());
            } catch (SQLException f) {
                return Result.fatal (f.toString ());
            }
        }
    }

    @Override
    public Result<Integer> countPostsInTopic(int topicId) {

        String sql = "SELECT count(1) AS c FROM Topic JOIN Post ON Post.topicId = Topic.id WHERE Topic.id = ?";
        try (PreparedStatement ps = c.prepareStatement (sql)){
            ps.setInt(1, topicId);
            ResultSet rs = ps.executeQuery ();

            int postCount = rs.getInt ("c");

            // TODO here we assumed that every Topic has one or more posts... therefore a count of zero means the topicId does not exist
            if(postCount == 0) return Result.failure("createPost: topic does not exist");

            return Result.success (postCount);

        } catch (SQLException e) {
            return Result.failure("createPost: failed to count posts");
        }
    }


    /* B.1 */

    @Override
    public Result likeTopic(String username, int topicId, boolean like) {
        if (username == null || username.trim().isEmpty()) { Result.failure("likeTopic: Username cannot be empty"); }

        // check db if user exists
        Result usernameResult = usernameExists(username);
        if (!usernameResult.isSuccess()){
            if (usernameResult.isFatal()) return usernameResult;
            return Result.failure ("likeTopic: " + usernameResult.getMessage());
        }
        int userId = (int) usernameResult.getValue();

        // check db if topic exists
        Result topicResult = topicExists(topicId);
        if (!topicResult.isSuccess()){
            if (topicResult.isFatal()) return topicResult;
            return Result.failure ("likeTopic: " + topicResult.getMessage());
        }

        // check if like already exists in db
        String sql = "SELECT * FROM LikeTopic WHERE personId = ? AND topicId = ?";
        try (PreparedStatement ps = c.prepareStatement(sql)){
            ps.setInt(1, userId);
            ps.setInt(2, topicId);
            ResultSet rs = ps.executeQuery();
            // Return success like in db and action is like - no-op
            // Return success if no like in db and action is not like - no-op
            if(like){
                if(rs.next()) return Result.success();
            } else {
                if(!rs.next()) return Result.success();
            }

        } catch (SQLException e) {
            return Result.fatal(e.getMessage());
        }

        if(like) sql = "INSERT INTO LikeTopic (personId, topicId) VALUES (?, ?)";
        else sql = "DELETE FROM LikeTopic WHERE personId = ? AND topicId = ?";
        try (PreparedStatement ps = c.prepareStatement(sql)){
            ps.setInt(1, userId);
            ps.setInt(2, topicId);

            if(ps.executeUpdate() != 1) throw new SQLException();
            c.commit();
            return Result.success();

        } catch (SQLException e) {
            try{
                c.rollback();
                return Result.fatal(e.getMessage());
            } catch (SQLException f) {
                return Result.fatal(f.getMessage());
            }
        }
    }

    @Override
    public Result likePost(String username, int topicId, int post, boolean like) {
        if (username == null || username.trim().isEmpty()) { Result.failure("likeTopic: Username cannot be empty"); }

        // check db if user exists
        Result usernameResult = usernameExists(username);
        if (!usernameResult.isSuccess()){
            if (usernameResult.isFatal()) return usernameResult;
            return Result.failure ("createPost: " + usernameResult.getMessage());
        }
        int userId = (int) usernameResult.getValue();

        // check db if topic exists
        Result topicResult = topicExists(topicId);
        if (!topicResult.isSuccess()){
            if (topicResult.isFatal()) return topicResult;
            return Result.failure ("createPost: " + topicResult.getMessage());
        }

        // check post exists and get post id from topic's post number
        // post does not exist if post number > number of posts in topic
        String sql = "SELECT Post.id FROM Topic" +
                " LEFT JOIN Post ON Post.topicId = Topic.id" +
                " WHERE Topic.id = ?" +
                " ORDER BY Post.id ASC";
        int postId;
        try (PreparedStatement ps = c.prepareStatement(sql, ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE)){
            ps.setInt(1, topicId);
            ResultSet rs = ps.executeQuery();

            if(rs.absolute(post) == false){
                return  Result.failure("likePost: Post does not exist");
            }
            postId = rs.getInt("Post.id");

        } catch (SQLException e) {
            return Result.fatal(e.getMessage());
        }

        // check if like already in db
        sql = "SELECT * FROM LikePost WHERE personId = ? AND postId = ?";
        try (PreparedStatement ps = c.prepareStatement(sql)){
            ps.setInt(1, userId);
            ps.setInt(2, postId);
            ResultSet rs = ps.executeQuery();
            // Return success like in db and action is like - no-op
            // Return success if no like in db and action is not like - no-op
            if(like){
                if(rs.next()) return Result.success();
            } else {
                if(!rs.next()) return Result.success();
            }
        } catch (SQLException e) {
            return Result.fatal(e.getMessage());
        }

        if(like) sql = "INSERT INTO LikePost (personId, postId) VALUES (?, ?)";
        else sql = "DELETE FROM LikePost WHERE personId = ? AND postId = ?";
        try (PreparedStatement ps = c.prepareStatement(sql)){
            ps.setInt(1, userId);
            ps.setInt(2, postId);

            if(ps.executeUpdate() != 1) throw new SQLException();
            c.commit();
            return Result.success();
        } catch (SQLException e) {
            try{
                c.rollback();
                return Result.fatal(e.getMessage());
            } catch (SQLException f) {
                return Result.fatal(f.getMessage());
            }
        }
    }

    @Override
    public Result<List<PersonView>> getLikers(int topicId) {
        // check topic exists
        Result topicResult = topicExists(topicId);
        if (!topicResult.isSuccess()){
            if (topicResult.isFatal()) return topicResult;
            return Result.failure ("getLikers: " + topicResult.getMessage());
        }
        // TODO - Join instead Topic -> liketopic -> Person : remove query above
        String sql = "SELECT Person.name, Person.username, Person.stuId FROM Person" +
                " LEFT JOIN LikeTopic" +
                " ON Person.id = LikeTopic.personId" +
                " WHERE LikeTopic.topicId = ?" +
                " ORDER BY name ASC";

        try (PreparedStatement ps = c.prepareStatement (sql)) {
            ps.setInt (1, topicId);
            ResultSet rs = ps.executeQuery ();
            List<PersonView> users = new ArrayList<> ();

            while (rs.next ()) {
                String name = rs.getString ("Person.name");
                String username = rs.getString ("Person.username");
                String studentId = rs.getString ("Person.stuId");
                users.add (new PersonView (name, username, studentId));
            }
            return Result.success (users);
        } catch (SQLException e) {
            return Result.fatal (e.getMessage ());
        }
    }

    @Override
    public Result<TopicView> getTopic(int topicId) {

        String sql = "SELECT Forum.id, Forum.title, Topic.title, Post.content, Post.id, Post.postedAt, Person.name, Person.username, LikePost.id FROM Forum" +
                " JOIN Topic" +
                " ON Forum.id = Topic.forumId" +
                " LEFT JOIN Post" +
                " ON Topic.id = Post.topicId" +
                " LEFT JOIN Person" +
                " ON Post.authorId = Person.id" +
                " LEFT JOIN LikePost" +
                " ON Post.id = LikePost.postId" +
                " WHERE Topic.id = ?" +
                " ORDER BY Post.id ASC";

        try (PreparedStatement ps = c.prepareStatement (sql)) {
            ps.setInt (1, topicId);
            ResultSet rs = ps.executeQuery ();
            ArrayList<PostView> posts = new ArrayList<> ();

            if (rs.next ()) {
                int forumId = rs.getInt("Forum.id");
                String forumName = rs.getString ("Forum.title");
                String topicTitle = rs.getString ("Topic.title");
                String authorName = rs.getString ("Person.name");
                String authorUsername = rs.getString ("Person.username");
                String postText = rs.getString ("Post.content");
                String postPostedAt = rs.getString ("Post.postedAt");

                int currentPostId = rs.getInt ("Post.id");
                int postNumber = 1;
                int postLikes = 0;

                do  {
                    int postId = rs.getInt ("Post.id");
                    if (postId != currentPostId) {
                        posts.add (new PostView (forumId, topicId, postNumber, authorName, authorUsername, postText, postPostedAt, postLikes));
                        currentPostId = postId;
                        postNumber++;
                        postLikes = 0;
                    }
                    else if (postId == currentPostId) {
                        authorName = rs.getString ("Person.name");
                        authorUsername = rs.getString ("Person.username");
                        postText = rs.getString ("Post.content");
                        postPostedAt = rs.getString ("Post.postedAt");
                        if (rs.getInt ("LikePost.id") > 0) {
                            postLikes++;
                        }
                    }
                    if (rs.isLast ()) {
                        authorName = rs.getString ("Person.name");
                        authorUsername = rs.getString ("Person.username");
                        postText = rs.getString ("Post.content");
                        postPostedAt = rs.getString ("Post.postedAt");
                        if (rs.getInt ("LikePost.id") > 0) {
                            postLikes++;
                        }
                        posts.add (new PostView (forumId, topicId, postNumber, authorName, authorUsername, postText, postPostedAt, postLikes));
                    }
                } while (rs.next ());

                return Result.success (new TopicView (forumId, topicId, forumName, topicTitle, posts));
            }
            return Result.failure ("getTopic: Topic doesn't exist");

        } catch (SQLException e) {
            return Result.fatal (e.getMessage ());
        }
    }

    /* B.2 */

    @Override
    public Result<List<AdvancedForumSummaryView>> getAdvancedForums() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Result<AdvancedPersonView> getAdvancedPersonView(String username) {

        String countLikesSql = "SELECT COUNT(LikeTopic.id) as countLikes, T.id, T.name, T.username, T.stuId FROM" +
        "        (SELECT * FROM Person" +
        "                WHERE username = ?) AS T" +
        " LEFT JOIN Topic" +
        " ON Topic.authorID = T.id" +
        " LEFT JOIN LikeTopic" +
        " ON Topic.id = LikeTopic.topicId" +
        " UNION" +
        " SELECT COUNT(LikePost.id) as countLikes, T.id, T.name, T.username, T.stuId FROM" +
        "        (SELECT * FROM Person" +
                        " WHERE username = ?) AS T" +
        " LEFT JOIN Post" +
        " ON Post.authorID = T.id" +
        " LEFT JOIN LikePost" +
        " ON Post.id = LikePost.postId";

        int userId = 0;
        String name = "";
        String studentId = "";
        int topicLikes = 0;
        int postLikes = 0;
        List<TopicSummaryView> topicLiked = new ArrayList<> ();

        try (PreparedStatement ps = c.prepareStatement (countLikesSql)) {
            ps.setString (1, username);
            ps.setString (2, username);
            ResultSet rs = ps.executeQuery ();

            while (rs.next ()) {
                if (rs.isFirst ()) {
                    topicLikes = rs.getInt ("countLikes");
                    if (rs.getString ("stuId") != null) {
                        studentId = rs.getString ("stuId");
                    }
                    name = rs.getString ("name");
                    userId = rs.getInt ("id");

                }
                if (rs.isLast ()) {
                    postLikes = rs.getInt ("countLikes");
                }
            }
        } catch (SQLException e) {
            return Result.fatal (e.getMessage ());
        }
        String sql = "SELECT Person.name, Person.username, Forum.id, FilTopic.id, FilTopic.title,  Post.content, Post.id, Post.postedAt, FilTopic.postedAt, LikeTopic.topicId, LikeTopic.personId" +
        " FROM Forum" +
        " JOIN (" +
        "        SELECT Topic.id, Topic.title, Topic.forumId, Topic.postedAt FROM Topic" +
        "        JOIN LikeTopic" +
        "        ON Topic.id = LikeTopic.topicId" +
        "        WHERE LikeTopic.personId = 1" +
        ") AS FilTopic" +
        " ON Forum.id = FilTopic.forumId" +
        " JOIN Post" +
        " ON Post.topicId = FilTopic.id" +
        " JOIN Person" +
        " ON Post.authorId = Person.id" +
        " JOIN LikeTopic ON FilTopic.id = LikeTopic.topicId" +
        " ORDER BY Forum.id ASC, FilTopic.id ASC, Post.postedAt DESC";

        try (PreparedStatement ps = c.prepareStatement (sql)) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery ();

            int postCount = 1;
            int topicLikesCount = 1;
            int currentPostId = -1;
            int currentTopicId = -1;
            int forumId = -1;
            String topicCreatedAt = "";
            String lastPostTime = "";
            String lastPostName = "";
            String topicTitle = "";
            String postCreatorName = "";
            String postCreatorUsername = "";

            while (rs.next ()) {
                int topicId = rs.getInt ("FilTopic.id");
                int postId = rs.getInt("Post.id");
                if (rs.isFirst ()) {
                    currentTopicId = topicId;
                    currentPostId = postId;
                }
                if (topicId != currentTopicId) {
                    topicLiked.add (new TopicSummaryView (topicId, forumId, topicTitle, postCount, topicCreatedAt,
                            lastPostTime, lastPostName, topicLikesCount / postCount, postCreatorName, postCreatorUsername));

                    currentTopicId = topicId;
                    topicLikesCount = 1;
                    postCount = 1;
                }
                else if (topicId == currentTopicId) {
                    if (postCount == 1) {
                        lastPostName = rs.getString ("Post.content");
                        lastPostTime = rs.getString ("Post.postedAt");
                    }
                    if (postId == currentPostId) {
                        topicLikesCount++;
                    }
                    else {
                        currentPostId = postId;
                        postCount++;
                    }
                    forumId = rs.getInt ("Forum.id");
                    topicTitle = rs.getString ("FilTopic.title");
                    topicCreatedAt =  rs.getString ("FilTopic.postedAt");
                    postCreatorName = rs.getString ("Person.name");
                    postCreatorUsername = rs.getString ("Person.username");
                }
                if (rs.isLast()) {
                    forumId = rs.getInt ("Forum.id");
                    topicTitle = rs.getString ("FilTopic.title");
                    topicCreatedAt =  rs.getString ("FilTopic.postedAt");
                    postCreatorName = rs.getString ("Person.name");
                    postCreatorUsername = rs.getString ("Person.username");
                    topicLiked.add (new TopicSummaryView (topicId, forumId, topicTitle, postCount, topicCreatedAt,
                            lastPostTime, lastPostName, topicLikesCount, postCreatorName, postCreatorUsername));
                }
            }
            return Result.success (new AdvancedPersonView (name, username, studentId, topicLikes, postLikes, topicLiked));
        } catch (SQLException e) {
            return Result.fatal (e.getMessage ());
        }
    }

    @Override
    public Result<AdvancedForumView> getAdvancedForum(int id) {
        throw new UnsupportedOperationException("Not supported yet.");
    }


    /* -------------------- custom helpers -------------------- */

    private Result<Integer> usernameExists(String username) {
        String sql = "SELECT id" +
                " FROM Person" +
                " WHERE username = ?";

        try (PreparedStatement ps = c.prepareStatement (sql)){
            ps.setString(1, username);
            ResultSet rs = ps.executeQuery ();
            if(rs.next ()) {
                int id = rs.getInt("id");
                return Result.success(id);
            }
            return Result.failure("Username not found");
        } catch (SQLException e) {
            return Result.fatal (e.getMessage()) ;
        }
    }

    private Result<Integer> forumExists(int forumId) {
        String sql = "SELECT id" +
                " FROM Forum WHERE id = ?";

        try (PreparedStatement ps = c.prepareStatement (sql)){
            ps.setInt(1, forumId);
            ResultSet rs = ps.executeQuery();
            if(rs.next ()) return Result.success(forumId);
            return Result.failure("Forum does not exist");
        } catch (SQLException e) {
            return Result.fatal(e.getMessage());
        }
    }

    private Result<Integer> forumTitleExists(String title) {
        String sql = "SELECT id" +
                " FROM Forum WHERE title = ?";

        try (PreparedStatement ps = c.prepareStatement (sql)){
            ps.setString(1, title);
            ResultSet rs = ps.executeQuery ();
            if (rs.next ()) {
                int id = rs.getInt ("id");
                return Result.success (id);
            }
            return Result.failure ("Forum title does not exist");
        } catch (SQLException e) {
            return Result.fatal (e.getMessage ());
        }
    }

    private Result<Integer> topicExists(int topicId) {
        String sql = "SELECT id" +
                " FROM Topic WHERE id = ?";

        try(PreparedStatement ps = c.prepareStatement (sql)) {
            ps.setInt(1, topicId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return Result.success(topicId);
            }
            return Result.failure("Topic does not exist");
        } catch (SQLException e) {
            return Result.fatal (e.getMessage());
        }
    }

    private SimpleTopicSummaryView newSimpleTopicSummaryView (int topicId, int forumId, String topicTitle) {
        SimpleTopicSummaryView lastTopic;
        if (topicTitle == null) {
            return null;
        }
        return new SimpleTopicSummaryView (topicId, forumId, topicTitle);
    }

}
