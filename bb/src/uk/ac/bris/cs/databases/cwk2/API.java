package uk.ac.bris.cs.databases.cwk2;

import java.sql.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
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

            if (rs.next ()) {
                String name = rs.getString ("name");
                String studentId = rs.getString ("stuId");
                user = new PersonView (name, username, studentId);
                return Result.success (user);
            }
            return Result.failure ("Username doesn't match any existing user");

        } catch (SQLException e){
            return Result.fatal (e.toString ());
        }
    }

    @Override
    public Result addNewPerson(String name, String username, String studentId) {
        if (c == null) { return Result.fatal("Error making connection"); }

        // TODO it appears the API is handling empty inputs for name and username but not sure how
        if (name == null || name.isEmpty ()) { return Result.failure ("Missing or empty name"); }
        if (username == null || username.isEmpty ()) { return Result.failure ("Missing or empty username"); }
        if (studentId == null || studentId.isEmpty ()) { return Result.failure ("Missing or empty student id"); }

        // Check if username does not already exists
        String sql = "SELECT count(1) as c FROM Person WHERE username = ?";
        try (PreparedStatement ps = c.prepareStatement (sql)) {
            ps.setString (1, username);
            ResultSet rs = ps.executeQuery ();

            if (rs.next () && rs.getInt("c") > 0) {
                return Result.failure ("This username is already being used");
            }

        } catch (SQLException e) {
            return Result.fatal (e.toString ());
        }

        // User doesn't exist yet: Add User to Database
        String anotherSql = "INSERT INTO Person (name, username, stuId) " + "VALUES (?, ?, ?)";
        try (PreparedStatement ps = c.prepareStatement (anotherSql)) {
            ps.setString (1, name);
            ps.setString (2, username);
            ps.setString (3, studentId);
            ps.executeQuery ();
            c.commit ();
        } catch (SQLException e) {
            try {
                c.rollback ();
                return Result.fatal ("Internal error: user couldn't be created");
            } catch (SQLException f) {
                return Result.fatal (e.toString ());
            }
        }

        return Result.success ();
    }

    /* A.2 */

    @Override
    public Result<List<SimpleForumSummaryView>> getSimpleForums() {
        if (c == null) { return Result.fatal("Error making connection"); }

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

        if (c == null) { throw new IllegalStateException (); }
        if (title == null || title.length () == 0) {
            return Result.failure ("Incorrect 'title' format: couldn't create Forum");
        }

        String sql = "SELECT count(1) as c FROM Forum WHERE title = ?";

        try (PreparedStatement ps = c.prepareStatement (sql)) {
            ps.setString (1, title);
            ResultSet rs = ps.executeQuery ();

            if (rs.next ()) {
                if (rs.getInt("c") > 0) {
                    return Result.failure ("This Forum name is already being used");
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException (e);
        }
        // Forum name doesn't exist yet: Add num Forum to Database
        String WriteSql = "INSERT INTO Forum (title) VALUES (?)";
        try (PreparedStatement ps = c.prepareStatement (WriteSql)) {
            ps.setString (1 ,title);
            ps.executeQuery ();

            c.commit();
        } catch (SQLException e) {
            try {
                c.rollback();
                return Result.fatal ("Internal error: forum couldn't be created");
            } catch (SQLException f) {
                throw new RuntimeException (f);
            }
        }
        return Result.success ();
    }

    /* A.3 */

    @Override
    public Result<List<ForumSummaryView>> getForums() {
        if (c == null) { throw new IllegalStateException (); }

        ArrayList<ForumSummaryView> forums = new ArrayList<> ();
        String sql = "SELECT * FROM Topic " +
                "INNER JOIN Forum " +
                "ON Forum.id = Topic.forumId ";

        try (PreparedStatement ps = c.prepareStatement (sql)) {
            ResultSet rs = ps.executeQuery ();

            while (rs.next ()) {
                int id = rs.getInt ("Forum.id");
                String title = rs.getString ("Forum.title");
                int topicId = rs.getInt ("Topic.id");
                String topicTitle = rs.getString ("Topic.title");

                SimpleTopicSummaryView lastTopic = new SimpleTopicSummaryView (topicId, id, topicTitle);
                forums.add (new ForumSummaryView (id, title, lastTopic));
            }
        }
        catch (SQLException e) {
            throw new RuntimeException (e);
        }
        return Result.success (forums);
    }


    @Override
    public Result<ForumView> getForum(int id) {
        if (c == null) { throw new IllegalStateException (); }

        /*A ForumView has id, title and list of topics*/
        String sql = "SELECT Forum.id, Forum.title, Topic.id, Topic.title " +
                "FROM Forum JOIN Topic ON Forum.id = Topic.forumId " +
                "WHERE Forum.id = ? " +
                "ORDER BY Forum.title";

        try (PreparedStatement ps = c.prepareStatement (sql)) {
            ps.setString(1, Integer.toString(id));
            ResultSet rs = ps.executeQuery ();

            if (!rs.next()) {
                return Result.failure("No topics or posts in this forum!");
            }

            int forumId = rs.getInt ("Forum.id");
            String forumTitle = rs.getString ("Forum.title");

            ArrayList<SimpleTopicSummaryView> topics = new ArrayList<>();
            while (rs.next ()) {
                int topicId = rs.getInt ("Topic.id");
                String topicTitle = rs.getString ("Topic.title");
                SimpleTopicSummaryView topic = new SimpleTopicSummaryView (topicId, id, topicTitle);
                topics.add(topic);
            }
            ForumView forum = new ForumView(forumId, forumTitle, topics);
            return Result.success(forum);
        }
        catch (SQLException e) {
            return Result.fatal(e.toString());
        }
    }

    @Override
    public Result<SimpleTopicView> getSimpleTopic(int topicId) {
        if (c == null) { throw new IllegalStateException (); }

        /*SimpleTopicView takes int topicId, String title, List<SimplePostView> posts*/
        /*SimplePostView takes int postNumber, String author, String text, String postedAt*/

        String sql = "SELECT Topic.id, Topic.title, Post.id, Person.name, Post.content, Post.postedAt " +
                "FROM Topic JOIN Post ON Topic.id = Post.topicId " +
                "JOIN Person ON Post.authorId = Person.id " +
                "WHERE Topic.id = ?";

        try (PreparedStatement ps = c.prepareStatement (sql)) {
            ps.setString(1, Integer.toString(topicId));
            ResultSet rs = ps.executeQuery ();

            if (!rs.next()) {
                return Result.failure("No topic with this id");
            }

            String topicTitle = rs.getString ("Topic.title");

            ArrayList<SimplePostView> posts = new ArrayList<>();
            while (rs.next ()) {
                int postId = rs.getInt ("Post.id");
                String author = rs.getString ("Person.name");
                String content = rs.getString("Post.content");
                String postedAt = rs.getString("Post.postedAt");
                SimplePostView post = new SimplePostView(postId, author, content, postedAt);
                posts.add(post);
            }
            SimpleTopicView simpleTopicView = new SimpleTopicView(topicId, topicTitle, posts);
            return Result.success(simpleTopicView);
        }
        catch (SQLException e) {
            return Result.fatal(e.toString());
        }
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
        if (text.isEmpty() || text == null) { Result.failure("createPost: Post cannot be empty"); }
        if (username.isEmpty() || username == null) { Result.failure("createPost: username cannot be empty"); }

        String sql = "INSERT INTO Post (authorId, content, topicId, postedAt, likes) VALUES (?, ?, ?, ?, ?)";
        String sqlCheck = "SELECT * FROM Topic WHERE id = ?";
        String getAuthor = "SELECT Person.id FROM Person WHERE username = ?";

        int authorId;

        try (PreparedStatement ps1 = c.prepareStatement(sqlCheck)) {
            ps1.setString(1, Integer.toString(topicId));
            ResultSet rs1 = ps1.executeQuery();
            if (!rs1.next()) {
                Result.failure("createPost: Topic does not exist!");
            }
        } catch (SQLException e) {
            Result.fatal("createPost: " + e.toString());
        }

        try (PreparedStatement ps2 = c.prepareStatement (getAuthor)) {
            ps2.setString (1, username);
            ResultSet rs2 = ps2.executeQuery ();
            if (!rs2.next()) {
                return Result.failure("createPost: username does not exist!");
            }

            authorId = Integer.parseInt(rs2.getString("id"));
        } catch (SQLException e) {
            return Result.fatal ("createPost: " + e.toString ());
        }

        try (PreparedStatement ps3 = c.prepareStatement (sql)) {
            Date date = new Date();
            String postedAt = date.toString();
            int likes = 0;

            ps3.setInt (1, authorId);
            ps3.setString(2, text);
            ps3.setInt(3, topicId);
            ps3.setString(4, postedAt);
            ps3.setInt(5, likes);

            ps3.executeQuery ();
            c.commit();

        } catch (SQLException e) {
            try {
                c.rollback();
                return Result.fatal("createPost: " + e.getMessage());
            } catch (SQLException f) {
                return Result.fatal("createPost: " + f.getMessage());
            }
        }
        return Result.success ();
    }

    @Override
    public Result createTopic(int forumId, String username, String title, String text) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Result<Integer> countPostsInTopic(int topicId) {
        throw new UnsupportedOperationException("Not supported yet.");
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
}
