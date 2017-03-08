/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package data;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedList;
import java.util.logging.Level;
import java.util.logging.Logger;
import objects.Message;
import objects.User;

/**
 *
 * @author wlloyd
 */
public class Model {
    static final Logger logger = Logger.getLogger(Model.class.getName());
    private static Model instance;
    private static Model instancemock;
    private Connection conn;
    private static String dbConnUrl = System.getenv("JDBC_DATABASE_URL");
    
    public static Model singleton() throws Exception {
        if (instance == null) {
            instance = new Model();
        }
        return instance;
    }
    
    public static Model mockSingleton(Connection conn) throws Exception {
        if (instancemock == null) {
            instancemock = new Model(conn);
        }
        return instancemock;
    }
    
    
    Model() throws Exception
    {  
        Class.forName("org.postgresql.Driver");
        logger.log(Level.INFO, "dbUrl=" + getDBConnURL());  
        logger.log(Level.INFO, "attempting db connection");
        try
        {
            conn = DriverManager.getConnection(getDBConnURL());
            logger.log(Level.INFO, "db connection ok.");
        }
        catch (Exception e)
        {
            logger.log(Level.SEVERE, "Unable to open db connection:" + e.toString());
        }
    }
    
    Model(Connection mockconn) throws Exception
    {  
        Class.forName("org.postgresql.Driver");
        logger.log(Level.INFO, "dbUrl=" + getDBConnURL());  
        logger.log(Level.INFO, "attempting mock db connection");
        conn = mockconn;
        logger.log(Level.INFO, "mock db connection ok.");
    }
    
    private Connection getConnection()
    {
        return conn;
    }
    
    private Statement createStatement() throws SQLException
    {
        Connection conn = getConnection();
        if ((conn != null) && (!conn.isClosed()))
        {
            logger.log(Level.INFO, "attempting statement create");
            Statement st = conn.createStatement();
            logger.log(Level.INFO, "statement created");
            return st;
        }
        else
        {
            // Handle connection failure
        }
        return null;
    }
    
    private PreparedStatement createPreparedStatement(String sql) throws SQLException
    {
        Connection conn = getConnection();
        if ((conn != null) && (!conn.isClosed()))
        {
            logger.log(Level.INFO, "attempting statement create");
            PreparedStatement pst = conn.prepareStatement(sql);
            logger.log(Level.INFO, "prepared statement created");
            return pst;
        }
        else
        {
            // Handle connection failure
        }
        return null;
    }
    
    public static String getDBConnURL() {
        if ((dbConnUrl == null) || (dbConnUrl.length() < 1))
            dbConnUrl = System.getProperties().getProperty("DBCONN");        
        logger.log(Level.INFO, "dbConnUrl VALUE=" + dbConnUrl);
        logger.log(Level.INFO, "sys-prop-getprop DBCONN VALUE=" + System.getProperties().getProperty("DBCONN"));
        return dbConnUrl;
    }
    
    public static void setDBConnURL(String connUrl) {
        dbConnUrl = connUrl;
    }
            
    public User newUser(User usr) throws SQLException
    {
        String sqlInsert="insert into users (name, age) values ('" + usr.getName() + "'" + "," + usr.getAge() + ");";
        Statement s = createStatement();
        logger.log(Level.INFO, "attempting statement execute");
        s.execute(sqlInsert,Statement.RETURN_GENERATED_KEYS);
        logger.log(Level.INFO, "statement executed.  atempting get generated keys");
        ResultSet rs = s.getGeneratedKeys();
        logger.log(Level.INFO, "retrieved keys from statement");
        int userid = -1;  
        while (rs.next())
            userid = rs.getInt(3);   // assuming 3rd column is userid
        logger.log(Level.INFO, "The new user id=" + userid);
        usr.setUserId(userid);
        return usr;
    }
    
    public void deleteUser(int userid) throws SQLException
    {
        String sqlDelete="delete from users where userid=?";
        PreparedStatement pst = createPreparedStatement(sqlDelete);
        pst.setInt(1, userid);
        pst.execute();
    }
    
    public User[] getUsers(int userId) throws SQLException
    {
        LinkedList<User> ll = new LinkedList<User>();
        String sqlQuery ="select * from users";
        sqlQuery += (userId > 0) ? " where userid=" + userId + " order by userid;" : " order by userid;";
        Statement st = createStatement();
        ResultSet rows = st.executeQuery(sqlQuery);
        while (rows.next())
        {
            logger.log(Level.INFO, "Reading row...");
            User usr = new User();
            usr.setName(rows.getString("name"));
            usr.setUserId(rows.getInt("userid"));
            usr.setAge(rows.getInt("age"));
            logger.log(Level.INFO, "Adding user to list with id=" + usr.getUserid());
            ll.add(usr);
        }
        return ll.toArray(new User[ll.size()]);
    }
    
    public boolean updateUser(User usr) throws SQLException
    {
        StringBuilder sqlQuery = new StringBuilder();
        sqlQuery.append("update users ");
        sqlQuery.append("set name='" + usr.getName() + "', ");
        sqlQuery.append("age=" + usr.getAge() + " ");
        sqlQuery.append("where userid=" + usr.getUserid() + ";");
        Statement st = createStatement();
        logger.log(Level.INFO, "UPDATE SQL=" + sqlQuery.toString());
        return st.execute(sqlQuery.toString());
    }
    
     public int newMessage(Message msg) throws SQLException {
        //String sqlInsert="insert into messages ("
        String sqlInsert = "insert into messages (userId, message, dateadded) values (" + msg.getUserId() + ",'" + msg.getMessage()+"', now());";
        logger.log(Level.INFO, "SQL STATMENT= " + sqlInsert);
        Statement s = createStatement();
        logger.log(Level.INFO, "attempting statement execute");
        s.execute(sqlInsert, Statement.RETURN_GENERATED_KEYS);
        logger.log(Level.INFO, "statement executed.  atempting get generated keys");
        ResultSet rs = s.getGeneratedKeys();
        logger.log(Level.INFO, "retrieved keys from statement");
        int messageId = -1;
        while (rs.next()) {
            messageId = rs.getInt(1);   // assuming 1st column is messageid
        }
        logger.log(Level.INFO, "The new Message id=" + messageId);
        return messageId;

    }

    public Message[] getMessages() throws SQLException {
        LinkedList<Message> ll = new LinkedList<Message>();
        String sqlQuery = "select * from messages;";
        Statement st = createStatement();
        ResultSet rows = st.executeQuery(sqlQuery);
        while (rows.next()) {
            logger.log(Level.INFO, "Reading row...");
            Message msg = new Message();
            msg.setMessage(rows.getString("message"));
            msg.setMessageId(rows.getInt("messageid"));
            msg.setDateadded(rows.getDate("dateadded"));
            msg.setUserId(rows.getInt("userid"));
            logger.log(Level.INFO, "Adding message to list with id=" + msg.getMessageId());
            ll.add(msg);
        }
        return ll.toArray(new Message[ll.size()]);
    }

    public void deleteMessage(int messageId) throws SQLException {
        String sqlDelete = "delete from messages where messageid=?";
        PreparedStatement pst = createPreparedStatement(sqlDelete);
        pst.setInt(1, messageId);
        pst.execute();//To change body of generated methods, choose Tools | Templates.
    }

    public boolean updateMessage(Message message) throws SQLException {
         //To change body of generated methods, choose Tools | Templates.
        StringBuilder sqlQuery = new StringBuilder();
        sqlQuery.append("update messages ");
        sqlQuery.append("set userId=" + message.getUserId()+ ",");
        sqlQuery.append("message='" + message.getMessage()+ "', ");
        sqlQuery.append("dateadded='" + message.getDateadded()+ "' ");
        sqlQuery.append("where messageid=" + message.getMessageId() + ";");
        Statement st = createStatement();
        logger.log(Level.INFO, "UPDATE SQL=" + sqlQuery.toString());
        return st.execute(sqlQuery.toString());
         
         
    }
}

