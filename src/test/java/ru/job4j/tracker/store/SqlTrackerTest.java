package ru.job4j.tracker.store;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import ru.job4j.tracker.model.Item;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.Properties;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.*;

public class SqlTrackerTest {

    private static Connection connection;

    @BeforeClass
    public static void initConnection() {
        try (InputStream in = SqlTrackerTest.class.getClassLoader().getResourceAsStream("test.properties")) {
            Properties config = new Properties();
            config.load(in);
            Class.forName(config.getProperty("driver-class-name"));
            connection = DriverManager.getConnection(
                    config.getProperty("url"),
                    config.getProperty("username"),
                    config.getProperty("password")

            );
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    @AfterClass
    public static void closeConnection() throws SQLException {
        connection.close();
    }

    @After
    public void wipeTable() throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("delete from items")) {
            statement.execute();
        }
    }
    @Test
    public void createItem() {
        SqlTracker tracker = new SqlTracker(connection);
        tracker.add(new Item("desc"));
        assertThat(tracker.findByName("desc").size(), is(1));
    }

    @Test
    public void replaceItem() {
        SqlTracker tracker = new SqlTracker(connection);
        Item item = new Item("desc");
        tracker.add(item);
        tracker.replace(item.getId(), new Item("replaced"));
        assertThat(tracker.findById(item.getId()).getName(), is("replaced"));
    }

    @Test
    public void deleteItem() {
        SqlTracker tracker = new SqlTracker(connection);
        Item item = new Item("desc");
        tracker.add(item);
        tracker.delete(item.getId());
        assertThat(tracker.findById(item.getId()), is(nullValue()));
    }

    @Test
    public void findByIdItem() {
        SqlTracker tracker = new SqlTracker(connection);
        Item item = new Item("desc");
        tracker.add(item);
        assertThat(tracker.findById(item.getId()), is(item));
    }

    @Test
    public void findNameItems() {
        SqlTracker tracker = new SqlTracker(connection);
        Item item = new Item("test1");
        Item item2 = new Item("test1");
        tracker.add(item);
        tracker.add(item2);
        List<Item> result = tracker.findByName("test1");
        assertThat(result, is(List.of(item, item2)));
    }

}