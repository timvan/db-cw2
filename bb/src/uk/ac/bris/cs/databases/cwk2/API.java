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
        if (username == null || username.isEmpty ()) { return Result.failure ("getPersonView: Username cannot be empty"); }

        String sql = "SELECT * FROM Person WHERE username = ?";
        try (PreparedStatement ps = c.prepareStatement (sql)) {
            ps.setString (1, username);
            ResultSet rs = ps.executeQuery ();

            if (!rs.next ()) {
                return Result.failure ("getPersonView: Username does not match any existing user");
            }
            String name = rs.getString ("name");
            String studentId = rs.getString ("stuId");

            return Result.success ( new PersonView (name, username, studentId));
        } catch (SQLException e){
            return Result.fatal (e.getMessage());
        }
    }

    @Override
    public Result addNewPerson(String name, String username, String studentId) {

        if (name == null || name.isEmpty ()) { return Result.failure ("addNewPerson: Name cannot be empty"); }
        if (username == null || username.isEmpty ()) { return Result.failure ("addNewPerson: Username cannot be empty"); }
        if (studentId != null && studentId.isEmpty ()) { return Result.failure ("addNewPerson: Student Id cannot be empty"); }

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
        if (title == null || title.isEmpty ()) return Result.failure ("createForum: Title cannot be empty");

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

        String sql = "SELECT *" +
                " FROM Forum" +
                " LEFT JOIN Topic" +
                " ON Forum.id = Topic.forumId" +
                " ORDER BY Forum.title ASC, Topic.id DESC";

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
                    SimpleTopicSummaryView topic = getSimpleTopicSummaryView (topicId, forumId, topicTitle);
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

            if (!rs.next()) return Result.failure("getSimpleTopic: No topic with this id");
            String topicTitle = rs.getString("Topic.title");
            do {
                int postId = rs.getInt ("Post.id");
                String personName = rs.getString("Person.name");
                String postContent = rs.getString("Post.content");
                String postPostedAt = rs.getString("Post.postedAt");
                posts.add (new SimplePostView(postId, personName, postContent, postPostedAt) );

            } while (rs.next ());
            return Result.success(new SimpleTopicView(topicId, topicTitle, posts));
        }
        catch (SQLException e) {
            return Result.fatal(e.getMessage ());
        }
    }


    @Override
    public Result<PostView> getLatestPost(int topicId) {

        String sql = "SELECT COUNT(*) as count, Forum.id, Topic.id, Post.id, Person.name," +
                " Person.username, Post.content, Post.postedAt, Post.likes" +
                " FROM Forum JOIN Topic ON Topic.forumId = Forum.id" +
                " JOIN Post ON Post.topicId = Topic.id" +
                " JOIN Person ON Post.authorId = Person.id" +
                " WHERE Topic.id = ?" +
                " ORDER BY Post.postedAt";

        try (PreparedStatement ps = c.prepareStatement (sql)) {
            ps.setString(1, Integer.toString(topicId));
            ResultSet rs = ps.executeQuery ();

            if (!rs.next()) {
                return Result.failure("getLatestPost: No topic with this id");
            }

            int forumId = rs.getInt("Forum.id");
            int postNumber = rs.getInt("count");
            String postAuthorName = rs.getString("Person.name");
            String postAuthorUserName = rs.getString("Person.username");
            String postContent = rs.getString("Post.content");
            String postPostedAt = rs.getString("Post.postedAt");
            int postLikes = rs.getInt("Post.likes");

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
        if (text == null || text.isEmpty()) { Result.failure("createPost: Text cannot be empty"); }
        if (username == null || username.isEmpty()) { Result.failure("createPost: Username cannot be empty"); }

        // check user exists
        Result usernameResult = usernameExists(username);
        if (!usernameResult.isSuccess()) return Result.failure ("createPost: " + usernameResult.getMessage());
        if (usernameResult.isFatal()) return usernameResult;

        // check topic exists
        Result topicResult = topicExists(topicId);
        if (!topicResult.isSuccess()) return Result.failure ("createPost: " + topicResult.getMessage());
        if (topicResult.isFatal()) return topicResult;

        String sql = "INSERT INTO Post (authorId, content, topicId) VALUES (?, ?, ?)";
        try (PreparedStatement ps = c.prepareStatement (sql)) {
            int authorId = getUserId(username);
            ps.setInt (1, authorId);
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
        if (username == null || username.isEmpty()) { Result.failure("createTopic: Username cannot be empty"); }
        if (title == null || title.isEmpty()) { Result.failure("createTopic: Title cannot be empty"); }
        if (text == null || text.isEmpty()) { Result.failure("createTopic: Text cannot be empty"); }

        // check user exists
        Result usernameResult = usernameExists(username);
        if (!usernameResult.isSuccess()) return Result.failure ("createTopic: " + usernameResult.getMessage());
        if (usernameResult.isFatal()) return usernameResult;

        // check forum exists
        Result forumResult = forumExists(forumId);
        if (!forumResult.isSuccess()) return Result.failure("createTopic: " + forumExists().getMessage());
        if (forumResult.isFatal()) return  forumResult;


        try ()

            int userId = getUserId(username);

            String newTopicSQL = "INSERT INTO Topic (title, authorId, forumId) VALUES (?,?,?)";
            String newPostSQL = "INSERT INTO Post (authorId, topicId, content) VALUES (?,?,?)";

            PreparedStatement newTopicPs = c.prepareStatement (newTopicSQL, Statement.RETURN_GENERATED_KEYS);
            PreparedStatement newPostPs = c.prepareStatement(newPostSQL);

            newTopicPs.setString(1, title);
            newTopicPs.setInt(2, userId);
            newTopicPs.setInt(3, forumId);

            int rowsAffected = newTopicPs.executeUpdate();

            if(rowsAffected != 1) return Result.fatal("Failed to create topic");

            ResultSet generatedKeys = newTopicPs.getGeneratedKeys();

            if(!generatedKeys.next()) return Result.fatal("Failed to get new topic key");

            int newTopicId = generatedKeys.getInt(1);

            newPostPs.setInt(1, userId);
            newPostPs.setInt(2, newTopicId);
            newPostPs.setString(3, text);

            newPostPs.executeUpdate();
            if(rowsAffected != 1) return Result.fatal("Failed to create post");

            c.commit();

        } catch (SQLException e) {
            try {
                c.rollback();
                return Result.fatal (e.toString ());
            } catch (SQLException f) {
                return Result.fatal (f.toString ());
            }
        }

        return Result.success();

    }

    @Override
    public Result<Integer> countPostsInTopic(int topicId) {
        if (c == null) { return Result.fatal ("Error making connection"); }

        try {

            String sql = "SELECT count(1) AS c FROM Topic JOIN Post ON Post.topicId = Topic.id WHERE Topic.id = ?";
            PreparedStatement ps = c.prepareStatement (sql);
            ps.setInt(1, topicId);
            ResultSet rs = ps.executeQuery ();

            if(!rs.next()) return Result.fatal("Failed getting count");

            int count = rs.getInt ("c");
            return Result.success (count);

        } catch (SQLException e) {
            return Result.fatal(e.toString());
        }
    }


    /* B.1 */

    @Override
    public Result likeTopic(String username, int topicId, boolean like) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Result likePost(String username, int topicId, int post, boolean like) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Result<List<PersonView>> getLikers(int topicId) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Result<TopicView> getTopic(int topicId) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    /* B.2 */

    @Override
    public Result<List<AdvancedForumSummaryView>> getAdvancedForums() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Result<AdvancedPersonView> getAdvancedPersonView(String username) {
        throw new UnsupportedOperationException("Not supported yet.");
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


    // TODO Maybe delete this method and implement in one above ??
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



    private SimpleTopicSummaryView getSimpleTopicSummaryView (int topicId, int forumId, String topicTitle) {
        SimpleTopicSummaryView lastTopic;
        if (topicTitle == null) {
            lastTopic = null;
        }
        else {
            lastTopic = new SimpleTopicSummaryView (topicId, forumId, topicTitle);
        }
        return lastTopic;
    }


    private int getUserId (String username) throws SQLException {

        String sql = "SELECT id FROM Person WHERE username = ?";
        try(PreparedStatement ps = c.prepareStatement (sql)) {
            ps.setString(1, username);
            ResultSet rs = ps.executeQuery();

            rs.next();
            return rs.getInt(1);

        } catch (SQLException e) {
            throw e;
        }
    }

}
