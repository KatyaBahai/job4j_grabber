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
                Post post = new Post();
                post.setId(rslSet.getInt(1));
                post.setTitle(rslSet.getString(2));
                post.setLink(rslSet.getString(3));
                post.setCreated(rslSet.getTimestamp(4).toLocalDateTime());
                post.setDescription(rslSet.getString(5));
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
                post.setId(rslSet.getInt(1));
                post.setTitle(rslSet.getString(2));
                post.setLink(rslSet.getString(3));
                post.setCreated(rslSet.getTimestamp(4).toLocalDateTime());
                post.setDescription(rslSet.getString(5));
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

    public static void main(String[] args) {
        HabrCareerParse parse = new HabrCareerParse(new HabrCareerDateTimeParser());
        List<Post> postList = parse.list("https://career.habr.com/vacancies/java_developer");
        Properties properties = new Properties();
        postList.forEach(System.out::println);
        try (FileReader reader = new FileReader("db/liquibase.properties")) {
            properties.load(reader);
            PsqlStore store = new PsqlStore(properties);
            postList.forEach(store::save);
            List<Post> rslList = store.getAll();
            rslList.forEach(System.out::println);
            System.out.println(store.findById(1));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}