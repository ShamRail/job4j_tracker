package ru.job4j.tracker.store;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.job4j.tracker.model.Item;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class SqlTracker implements Store {

    private static final Logger LOG = LoggerFactory.getLogger(SqlTracker.class.getName());
    private static final String INSERT_REQUEST = "insert into items(name) values(?) returning id;";
    private static final String UPDATE_REQUEST = "update items set name = ? where id = ?;";
    private static final String DELETE_REQUEST = "delete from items where id = ?;";
    private static final String FINDALL_REQUEST = "select * from items;";
    private static final String FINDBYNAME_REQUEST = "select * from items where name like ?;";
    private static final String FINDBYID_REQUEST = "select * from items where id = ?;";

    protected Connection cn;

    public SqlTracker() {

    }

    public SqlTracker(Connection cn) {
        this.cn = cn;
    }

    public void init() {
        initConnection();
    }

    private void initConnection() {
        try (InputStream in = SqlTracker.class.getClassLoader().getResourceAsStream("app.properties")) {
            Properties config = new Properties();
            config.load(in);
            Class.forName(config.getProperty("driver-class-name"));
            cn = DriverManager.getConnection(
                    config.getProperty("url"),
                    config.getProperty("username"),
                    config.getProperty("password")
            );
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public void close() throws Exception {
        if (cn != null) {
            cn.close();
        }
    }

    @Override
    public Item add(Item item) {
        LOG.debug("Insert data, name: {}", item.getName());
        try (PreparedStatement ps = cn.prepareStatement(INSERT_REQUEST)) {
            ps.setString(1, item.getName());
            try (ResultSet result = ps.executeQuery()) {
                if (result.next()) {
                    LOG.debug("Inserting complete");
                    item.setId(String.valueOf(result.getInt(1)));
                    LOG.debug("Generated id: {}", item.getId());
                } else {
                    LOG.debug("Inserting is fallen");
                }
            }
        } catch (Exception e) {
            LOG.debug("Something went wrong", e);
        }
        return item;
    }

    @Override
    public boolean replace(String id, Item item) {
        LOG.debug("Replace item. id: {}, name: {}", id, item.getName());
        boolean result = false;
        try (PreparedStatement ps = cn.prepareStatement(UPDATE_REQUEST)) {
            ps.setString(1, item.getName());
            ps.setInt(2, Integer.parseInt(id));
            result = ps.executeUpdate() > 0;
            LOG.debug("Replace complete. Result: {}", result);
        } catch (Exception e) {
            LOG.error("Something went wrong", e);
        }
        return result;
    }

    @Override
    public boolean delete(String id) {
        LOG.debug("Delete item. id: {}", id);
        boolean result = false;
        try (PreparedStatement ps = cn.prepareStatement(DELETE_REQUEST)) {
            ps.setInt(1, Integer.parseInt(id));
            result = ps.executeUpdate() > 0;
            LOG.debug("Deleting complete. Result: {}", result);
        } catch (Exception e) {
            LOG.error("Something went wrong", e);
        }
        return result;
    }

    @Override
    public List<Item> findAll() {
        LOG.debug("Find all");
        List<Item> result = new ArrayList<>();
        try (PreparedStatement ps = cn.prepareStatement(FINDALL_REQUEST)) {
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(new Item(
                            String.valueOf(rs.getInt("id")),
                            rs.getString("name")
                    ));
                }
            }
            LOG.debug("Selecting complete. Found items: {}", result.size());
        } catch (Exception e) {
            LOG.error("Something went wrong", e);
        }
        LOG.debug("Found {} items", result.size());
        return result;
    }

    @Override
    public List<Item> findByName(String key) {
        LOG.debug("Find by name {}", key);
        List<Item> result = new ArrayList<>();
        try (PreparedStatement ps = cn.prepareStatement(FINDBYNAME_REQUEST)) {
            ps.setString(1, "%" + key + "%");
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(new Item(
                            String.valueOf(rs.getInt("id")),
                            rs.getString("name")
                    ));
                }
            }
            LOG.debug("Selecting complete. Found items: {}", result.size());
        } catch (Exception e) {
            LOG.error("Something went wrong", e);
        }
        LOG.debug("Found {} items", result.size());
        return result;
    }

    @Override
    public Item findById(String id) {
        LOG.debug("Find by id {}", id);
        Item result = null;
        try (PreparedStatement ps = cn.prepareStatement(FINDBYID_REQUEST)) {
            ps.setInt(1, Integer.parseInt(id));
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    result = new Item(
                            String.valueOf(rs.getInt("id")),
                            rs.getString("name")
                    );
                }
            }
            LOG.debug("Selecting complete. Found {}", result);
        } catch (Exception e) {
            LOG.error("Something went wrong", e);
        }
        LOG.debug("Found {}", result);
        return result;
    }

}
