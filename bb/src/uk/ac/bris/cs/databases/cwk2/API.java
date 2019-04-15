package uk.ac.bris.cs.databases.cwk2;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import uk.ac.bris.cs.databases.api.APIProvider;
import uk.ac.bris.cs.databases.api.AdvancedForumSummaryView;
import uk.ac.bris.cs.databases.api.AdvancedForumView;
import uk.ac.bris.cs.databases.api.ForumSummaryView;
import uk.ac.bris.cs.databases.api.ForumView;
import uk.ac.bris.cs.databases.api.AdvancedPersonView;
import uk.ac.bris.cs.databases.api.PostView;
import uk.ac.bris.cs.databases.api.Result;
import uk.ac.bris.cs.databases.api.PersonView;
import uk.ac.bris.cs.databases.api.SimpleForumSummaryView;
import uk.ac.bris.cs.databases.api.SimpleTopicView;
import uk.ac.bris.cs.databases.api.TopicView;

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
    public Result<Map<String, String>> getUsers(){

        String query = "SELECT username, name FROM Person";

        try (PreparedStatement s = c.prepareStatement(query)) {
            Map<String, String> users = new HashMap<>();
            ResultSet r = s.executeQuery();
            while(r.next()){
                String name = r.getString("name");
                String username = r.getString("username");
                users.put(username, name);
            }
            s.close();

            return Result.success(users);

        } catch (SQLException e) {
            return Result.fatal("SQL error...");
        } catch (Exception e) {
            return Result.fatal("Could not get users");
        }
    }

    @Override
    public Result<PersonView> getPersonView(String username) {

        String query = ("SELECT * FROM Person WHERE username = ?");

        try (PreparedStatement s = c.prepareStatement(query)) {
            s.setString(1, username);
            ResultSet r = s.executeQuery();
            r.next(); // SHOULD WE CHECK IF HAS NEXT BEFORE?
            PersonView person = new PersonView(
                    r.getString("name"), r.getString("username"), r.getString("stuId")
            );
            s.close();
            return Result.success(person);
        } catch (SQLException e){

            return Result.fatal("Could not get user");
        }

        // TODO should we handle a fatal error for other exceptions?
    }

    @Override
    public Result addNewPerson(String name, String username, String studentId) {
        String query = ("INSERT INTO Person (name, username, stuId) VALUES (?,?,?)");

        if(name == null || name.isEmpty()) return Result.failure("Name cannot be null or empty");
        if(username == null || username.isEmpty()) return Result.failure("username cannot be null or empty");
        if(studentId == null || studentId.isEmpty()) return Result.failure("StudentId cannot be null or empty");

        // TODO HOW IS UNIQUENESS HANDLED???

        try (PreparedStatement s = c.prepareStatement(query)) {
            s.setString(1, name);
            s.setString(2, username);
            s.setString(3, studentId);

            ResultSet r = s.executeQuery();
            s.close();
            return Result.success();

        } catch (SQLException e) {
            return Result.fatal("Could not create user");
        }

    }

    /* A.2 */

    @Override
    public Result<List<SimpleForumSummaryView>> getSimpleForums() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Result createForum(String title) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    /* A.3 */

    @Override
    public Result<List<ForumSummaryView>> getForums() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Result<ForumView> getForum(int id) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Result<SimpleTopicView> getSimpleTopic(int topicId) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Result<PostView> getLatestPost(int topicId) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Result createPost(int topicId, String username, String text) {
        throw new UnsupportedOperationException("Not supported yet.");
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
