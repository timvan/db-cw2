package uk.ac.bris.cs.databases.cwk2;

import java.sql.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sun.jna.platform.win32.OaIdl;
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
        if (c == null) { return Result.fatal("Error making connection"); }

        HashMap<String, String> users = new HashMap<> ();

        String sql = "SELECT username, name FROM Person";
        try (PreparedStatement ps = c.prepareStatement (sql)) {
            ResultSet rs = ps.executeQuery ();

            while (rs.next ()) {
                String username = rs.getString ("username");
                String name = rs.getString ("name");
                users.put (username, name);
            }
            ps.close();

        } catch (SQLException e) {
            return Result.fatal (e.toString ());
        }
        return Result.success (users);
    }

    @Override
    public Result<PersonView> getPersonView(String username) {
        if (c == null) { return Result.fatal("Error making connection"); }
        if (username == null || username.isEmpty ()) { return Result.failure ("Username cannot be null"); }

        PersonView user;

        String sql = "SELECT * FROM Person WHERE username = ?";
        try (PreparedStatement ps = c.prepareStatement (sql)) {
            ps.setString (1, username);
            ResultSet rs = ps.executeQuery ();

            if (!rs.next ()) {
                return Result.failure ("Username doesn't match any existing user");
            }

            String name = rs.getString ("name");
            String studentId = rs.getString ("stuId");
            user = new PersonView (name, username, studentId);
            ps.close();

        } catch (SQLException e){
            return Result.fatal (e.toString ());
        }

        return Result.success (user);
    }

    @Override
    public Result addNewPerson(String name, String username, String studentId) {
        if (c == null) { return Result.fatal ("Error making connection"); }

        // TODO the handler is checking some the input - therefore some/all of the following checks may not be needed.
        //  check where the method gets called.
        if (name == null || name.isEmpty ()) { return Result.failure ("Missing or empty name"); }
        if (username == null || username.isEmpty ()) { return Result.failure ("Missing or empty username"); }
        if (studentId == null || studentId.isEmpty ()) { return Result.failure ("Missing or empty student id"); }

        String anotherSql = "INSERT INTO Person (name, username, stuId) " + "VALUES (?, ?, ?)";
        try (PreparedStatement ps = c.prepareStatement (anotherSql)) {

            if(usernameExists("username")) return Result.failure ("This username is already being used");

            ps.setString (1, name);
            ps.setString (2, username);
            ps.setString (3, studentId);
            ps.executeQuery ();
            c.commit ();

        } catch (SQLException e) {
            try {
                c.rollback ();
                return Result.fatal ("User couldn't be created");
            } catch (SQLException f) {
                return Result.fatal (f.toString ());
            }
        }

        return Result.success ();
    }

    /* A.2 */

    @Override
    public Result<List<SimpleForumSummaryView>> getSimpleForums() {
        if (c == null) { return Result.fatal ("Error making connection"); }

        ArrayList<SimpleForumSummaryView> forums = new ArrayList<> ();

        String sql = "SELECT * FROM Forum ORDER BY title ASC";
        try (PreparedStatement ps = c.prepareStatement (sql)) {
            ResultSet rs = ps.executeQuery ();
            while(rs.next ()) {
                int id = rs.getInt ("id");
                String title = rs.getString ("title");
                forums.add (new SimpleForumSummaryView (id, title));
            }
        }
        catch (SQLException e) {
            return Result.fatal (e.toString ());
        }
        return Result.success (forums);
    }

    @Override
    public Result createForum(String title) {
        if (c == null) { return Result.fatal ("Error making connection"); }
        if (title == null || title.isEmpty ()) return Result.failure ("Missing or empty title");

        String sql = "SELECT count(1) as c FROM Forum WHERE title = ?";

        // Check if forum does not already exists
        try (PreparedStatement ps = c.prepareStatement (sql)) {
            ps.setString (1, title);
            ResultSet rs = ps.executeQuery ();

            if (rs.next ()) {
                if (rs.getInt("c") > 0) {
                    return Result.failure ("This Forum name is already being used");
                }
            }
        } catch (SQLException e) {
            return Result.fatal (e.toString ());
        }

        // Forum name doesn't exist yet: Add num Forum to Database
        String writeSql = "INSERT INTO Forum (title) VALUES (?)";
        try (PreparedStatement ps = c.prepareStatement (writeSql)) {
            ps.setString (1, title);
            ps.executeQuery ();
            c.commit();
        } catch (SQLException e) {
            try {
                c.rollback();
                return Result.fatal ("Forum couldn't be created");
            } catch (SQLException f) {
                return Result.fatal (f.toString ());
            }
        }
        return Result.success ();
    }

    /* A.3 */

    @Override
    public Result<List<ForumSummaryView>> getForums() {
        if (c == null) { return Result.fatal ("Error making connection"); }

        ArrayList<ForumSummaryView> forums = new ArrayList<> ();
        String sql = "SELECT id, title FROM Forum;";

        try (PreparedStatement ps = c.prepareStatement (sql)) {
            ResultSet rs = ps.executeQuery ();

            while (rs.next ()) {

                int id = rs.getInt ("Forum.id");
                String title = rs.getString ("Forum.title");
                SimpleTopicSummaryView lastTopic = getLastTopic(id);

                forums.add (new ForumSummaryView (id, title, lastTopic));
            }
        }
        catch (SQLException e) {
            return Result.fatal (e.toString ());
        }
        return Result.success (forums);
    }

    @Override
    public Result<ForumView> getForum(int id) {
        if (c == null) { return Result.fatal ("Error making connection"); }

        String sql = "SELECT id, title FROM Forum WHERE id = ?";

        try (PreparedStatement ps = c.prepareStatement (sql)) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery ();

            if (!rs.next()) return Result.failure("Topic does not exist");

            String forumTitle = rs.getString ("Forum.title");
            ForumView forum = new ForumView(id, forumTitle, getForumTopics(id));

            return Result.success (forum);
        }
        catch (SQLException e) {
            return Result.fatal(e.toString());
        }
    }

    private ArrayList<SimpleTopicSummaryView> getForumTopics (int forumId) throws SQLException {
        // TODO what's going on here with assuming connections? can't return result but should be tested?
        // if (c == null) { return Result.fatal ("Error making connection"); }

        ArrayList<SimpleTopicSummaryView> topics = new ArrayList<>();

        String sql = "SELECT id, title FROM Topic WHERE forumId = ?";

        try(PreparedStatement ps = c.prepareStatement (sql)){
            ps.setInt(1, forumId);

            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                topics.add(new SimpleTopicSummaryView (
                        rs.getInt("id"),
                        forumId,
                        rs.getString("title")
                ));
            }

            return topics;

        } catch (SQLException e) { throw e; }
    }

    @Override
    public Result<SimpleTopicView> getSimpleTopic(int topicId) {
        if (c == null) { throw new IllegalStateException (); }

        String sql = "SELECT id, title FROM Topic WHERE id = ?";

        try (PreparedStatement ps = c.prepareStatement (sql)) {
            ps.setInt(1, topicId);
            ResultSet rs = ps.executeQuery ();

            if (!rs.next()) return Result.failure("No topic with this id");

            return Result.success(
                    new SimpleTopicView(
                            topicId,
                            rs.getString("title"),
                            getTopicsPosts(topicId)
                    )
            );
        }
        catch (SQLException e) {
            return Result.fatal(e.toString());
        }
    }

    private ArrayList<SimplePostView> getTopicsPosts(int topicId) throws SQLException {

        ArrayList<SimplePostView> posts = new ArrayList<>();
        int count = 1;
        String sql = "SELECT Post.content, Post.postedAt, Person.name " +
                "FROM Post " +
                "JOIN Person ON Person.id = Post.authorId " +
                "WHERE Post.topicId = ? " +
                "ORDER BY Post.postedAt ASC";

        try (PreparedStatement ps = c.prepareStatement (sql)) {
            ps.setInt(1, topicId);
            ResultSet rs = ps.executeQuery ();

            while (rs.next ()) {
                posts.add(new SimplePostView(
                        count++,
                        rs.getString("Person.name"),
                        rs.getString("Post.content"),
                        rs.getString("Post.postedAt")
                ));
            }
            return posts;

        } catch (SQLException e) { throw e; } // TODO is this the correct way? does it capture all the stack trace?
    }

    @Override
    public Result<PostView> getLatestPost(int topicId) {
        if (c == null) { throw new IllegalStateException (); }
        /*PostView takes int forumId, int topicId, int postNumber,
            String authorName, String authorUserName, String text,
            String postedAt, int likes*/
        String sql = "SELECT Forum.id, Topic.id, Post.id, Person.name, " +
                "Person.username, Post.content, Post.postedAt, Post.likes " +
                "FROM Forum JOIN Topic ON Topic.forumId = Forum.id " +
                "JOIN Post ON Post.topicId = Topic.id " +
                "JOIN Person ON Post.authorId = Person.id " +
                "WHERE Topic.id = ? " +
                "ORDER BY Post.postedAt";

        try (PreparedStatement ps = c.prepareStatement (sql)) {
            ps.setString(1, Integer.toString(topicId));
            ResultSet rs = ps.executeQuery ();

            if (!rs.next()) {
                return Result.failure("No topic with this id");
            }

            int forumId = rs.getInt("Forum.id");
            int postId = rs.getInt("Post.id");
            String authorName = rs.getString("Person.name");
            String authorUserName = rs.getString("Person.username");
            String content = rs.getString("Post.content");
            String postedAt = rs.getString("Post.postedAt");
            int likes = rs.getInt("Post.likes");

            PostView postView = new PostView(
                    forumId,
                    topicId,
                    postId,
                    authorName,
                    authorUserName,
                    content,
                    postedAt,
                    likes
            );
            return Result.success(postView);
        }
        catch (SQLException e) {
            return Result.fatal(e.toString());
        }
    }

    @Override
    public Result createPost(int topicId, String username, String text) {
        if (c == null) { throw new IllegalStateException (); }
        if (text.equals("") || text == null) { Result.failure("Post cannot be empty"); }
        if (username.equals("") || username == null) { Result.failure("Username cannot be empty"); }


        String sql = "INSERT INTO Post (authorId, content, topicId) VALUES (?, ?, ?)";

        try (PreparedStatement ps = c.prepareStatement (sql)) {
            if(!topicExists(topicId)) return Result.failure("Topic does not exist");
            if(!usernameExists(username)) return Result.failure("User does not exist");

            int authorId = getUserId(username);
            ps.setInt (1, authorId);
            ps.setString (2, text);
            ps.setInt (3, topicId);

            if(ps.executeUpdate () != 1) return Result.fatal("Post not created");

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
        if (c == null) { return Result.fatal ("Error making connection"); }
        if (username.isEmpty() || username == null) { Result.failure("Missing or empty username"); }
        if (title.isEmpty() || title == null) { Result.failure("Missing or empty title"); }
        if (text.isEmpty() || text == null) { Result.failure("Missing or empty text"); }

        try {
            if(!usernameExists(username)) return Result.failure ("Username does not exist");
            if(!forumExists (forumId)) return Result.failure("Forum does not exist");

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
            if(!topicExists(topicId)) return Result.failure("Topic does not exist");

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


    // TODO these exist functions can be wrapped into one - HMMM this doesn't look like it works
    private boolean existsInTable(String table, String field, String value) throws SQLException {
        String sql = "SELECT count(1) AS c FROM ? WHERE ? = ?";

        try(PreparedStatement ps = c.prepareStatement (sql)){
            ps.setString(1, table);
            ps.setString(2, field);
            ps.setString(3, value);
            ResultSet rs = ps.executeQuery ();

            if (rs.next () && rs.getInt("c") > 0) {
                return true;
            }

        } catch (SQLException e){
            throw e;
        }

        return false;
    }

    private boolean usernameExists(String username) throws SQLException {
        String sql = "SELECT count(1) AS c FROM Person WHERE username = ?";

        try(PreparedStatement ps = c.prepareStatement (sql)){
            ps.setString(1, username);
            ResultSet rs = ps.executeQuery ();

            if (rs.next () && rs.getInt("c") > 0) {
                return true;
            }

        } catch (SQLException e){
            throw e;
        }

        return false;
    }

    private boolean forumExists(int forumId) throws SQLException {
        String sql = "SELECT count(1) AS c FROM Forum WHERE id = ?";

        try(PreparedStatement ps = c.prepareStatement (sql)) {
            ps.setInt(1, forumId);
            ResultSet rs = ps.executeQuery ();

            if (rs.next () && rs.getInt("c") > 0) {
                return true;
            }

        } catch (SQLException e){
            throw e;
        }

        return false;
    }

    private boolean topicExists(int topicId) throws SQLException {
        String sql = "SELECT count(1) AS c FROM Topic WHERE id = ?";

        try(PreparedStatement ps = c.prepareStatement (sql)) {
            ps.setInt(1, topicId);
            ResultSet rs = ps.executeQuery ();

            if (rs.next () && rs.getInt("c") > 0) {
                return true;
            }

        } catch (SQLException e){
            throw e;
        }

        return false;
    }

    private SimpleTopicSummaryView getLastTopic(int forumId) throws SQLException {

        String getLastTopicSQL = "SELECT id, title FROM Topic WHERE forumId = ? ORDER BY id DESC LIMIT 1";
        try(PreparedStatement ps = c.prepareStatement(getLastTopicSQL);) {
            ps.setInt(1, forumId);

            ResultSet rs = ps.executeQuery();

            if(!rs.next()) return null;

            int id = rs.getInt("id");
            String title = rs.getString("title");

            SimpleTopicSummaryView lastTopic =  new SimpleTopicSummaryView (
                    id,
                    forumId,
                    title
            );

            return lastTopic;

        } catch (SQLException e){
            throw e;
        }
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
