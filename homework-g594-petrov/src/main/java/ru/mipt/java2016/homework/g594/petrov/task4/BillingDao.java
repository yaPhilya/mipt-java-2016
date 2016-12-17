package ru.mipt.java2016.homework.g594.petrov.task4;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import ru.mipt.java2016.homework.base.task1.ParsingException;

import javax.annotation.PostConstruct;
import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.DoubleSummaryStatistics;
import java.util.HashMap;


/**
 * Created by philipp on 15.12.16.
 */


@Repository
public class BillingDao {
    private static final Logger LOG = LoggerFactory.getLogger(BillingDao.class);

    @Autowired
    private DataSource dataSource;

    private JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void postConstruct() {
        jdbcTemplate = new JdbcTemplate(dataSource, false);
        initSchema();
    }

    public void initSchema() {
        LOG.trace("Initializing schema");
        jdbcTemplate.execute("CREATE SCHEMA IF NOT EXISTS billing");
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS billing.users " +
                "(username VARCHAR PRIMARY KEY, password VARCHAR, enabled BOOLEAN)");
        //jdbcTemplate.update("UPSERT INTO billing.users VALUES ('username', 'password', TRUE)");
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS billing.functions" +
                "(username VARCHAR, functionName VARCHAR, arguments INT, PRIMARY KEY(username, functionName, arguments))");
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS billing.variables" +
                "(username VARCHAR, varName VARCHAR, varValue DOUBLE NOT NULL, PRIMARY KEY(username, varName))");
    }


    public BillingUser loadUser(String username) throws EmptyResultDataAccessException {
        LOG.trace("Querying for user " + username);
        return jdbcTemplate.queryForObject(
            "SELECT username, password, enabled FROM billing.users WHERE username = ?",
            new Object[]{username},
            new RowMapper<BillingUser>() {
                @Override
                public BillingUser mapRow(ResultSet rs, int rowNum) throws SQLException {
                    return new BillingUser(
                            rs.getString("username"),
                            rs.getString("password"),
                            rs.getBoolean("enabled")
                    );
                }
            }
        );
    }
    public void addUser(String username, String password) {
        LOG.trace("Adding user");
        jdbcTemplate.update("UPSERT INTO billing.users VALUES ('" + username + "','" + password + "', TRUE)");
    }
    public void addVariable(String username, String variable, Double value) {
        LOG.trace("Adding variable");
        jdbcTemplate.update("INSERT INTO billing.variables VALUES ('" + username + "','" + variable + "'," + value.toString() + ")");
    }
    public void deleteVariable(String username, String variable) {
        LOG.trace("Deleting variable");
        jdbcTemplate.update("DELETE FROM billing.variables WHERE username = '" + username + "' AND varName = '" + variable + "'");
    }
    public HashMap<String, String> getVariables(String username) {
        HashMap<String, String> tmp = new HashMap<>();
        jdbcTemplate.query("SELECT username, varName, varValue FROM billing.variables WHERE username = ?", new Object[]{username}, new RowCallbackHandler() {
            @Override
            public void processRow(ResultSet rs) throws SQLException {
                tmp.put(rs.getString("varName"), Double.toString(rs.getDouble("varValue")));
            }
        });
        return tmp;
    }
    public String getValue(String username, String varName) throws ParsingException {
        try {
            return jdbcTemplate.queryForObject(
                    "SELECT username, varName, varValue FROM billing.variables WHERE username = ? AND varName = ?",
                    new Object[]{username, varName},
                    new RowMapper<String>() {
                        @Override
                        public String mapRow(ResultSet rs, int rowNum) throws SQLException {
                            return Double.toString(rs.getDouble("varValue"));
                        }
                    }
            );
        } catch (EmptyResultDataAccessException e) {
            throw new ParsingException("No such variable");
        }
    }
}


