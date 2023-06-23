package ru.job4j.grabber;

import ru.job4j.grabber.utils.HabrCareerDateTimeParser;

import java.io.FileReader;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class PsqlStore implements Store {

    private Connection cnn;

    public PsqlStore(Properties cfg) {
        try {
            Class.forName(cfg.getProperty("driver-class-name"));
            cnn = DriverManager.getConnection(cfg.getProperty("url"), cfg.getProperty("username"), cfg.getProperty("password"));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public void save(Post post) {
       try (PreparedStatement statement = cnn.prepareStatement(
                "INSERT INTO post(title, link, created, description) "
                        + "VALUES(?, ?, ?, ?)"
                        + "ON CONFLICT(link) do nothing;", Statement.RETURN_GENERATED_KEYS)) {
           statement.setString(1, post.getTitle());
           statement.setString(2, post.getLink());
           statement.setTimestamp(3, Timestamp.valueOf(post.getCreated()));
           statement.setString(4, post.getDescription());
           statement.execute();
           ResultSet keys = statement.getGeneratedKeys();
           if (keys.next()) {
               post.setId(keys.getInt(1));
           }
        } catch (Exception e) {
           e.printStackTrace();
       }
    }

    @Override
    public List<Post> getAll() {
        List<Post> list = new ArrayList<>();
        try (Statement statement = cnn.createStatement()) {
            ResultSet rslSet = statement.executeQuery("SELECT * FROM post;");
            while (rslSet.next()) {
                Post post = createPost(rslSet);
                list.add(post);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    @Override
    public Post findById(int id) {
        Post post = new Post();
        try (PreparedStatement statement = cnn.prepareStatement(
                "SELECT * FROM post WHERE id = ?;")) {
            statement.setInt(1, id);
            ResultSet rslSet = statement.executeQuery();
            while (rslSet.next()) {
                post = createPost(rslSet);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return post;
    }

    @Override
    public void close() throws Exception {
        if (cnn != null) {
            cnn.close();
        }
    }

    public Post createPost(ResultSet rslSet) throws SQLException {
        Post post = new Post();
            post.setId(rslSet.getInt(1));
            post.setTitle(rslSet.getString(2));
            post.setLink(rslSet.getString(3));
            post.setCreated(rslSet.getTimestamp(4).toLocalDateTime());
            post.setDescription(rslSet.getString(5));
        return post;
    }

    public void createTable() {
        try (Statement statement = cnn.createStatement()) {
            statement.executeQuery("CREATE TABLE IF NOT EXISTS post (\n"
                    + "id serial primary key,\n"
                    + "title text,\n"
                    + "link text UNIQUE,\n"
                    + "created timestamp,\n"
                    + "description text\n"
                    + ");");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

        public static void main(String[] args) {
        HabrCareerParse parse = new HabrCareerParse(new HabrCareerDateTimeParser());
        List<Post> postList = parse.list("https://career.habr.com/vacancies/java_developer");
        Properties properties = new Properties();
        try (FileReader reader = new FileReader("db/app.properties")) {
            properties.load(reader);
            PsqlStore store = new PsqlStore(properties);
            store.createTable();
            postList.forEach(store::save);
            List<Post> rslList = store.getAll();
            rslList.forEach(System.out::println);
            System.out.println(store.findById(1));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}