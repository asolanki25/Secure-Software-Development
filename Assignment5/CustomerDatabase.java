import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Assignment 5, Question 1 (Java JDBC version).
 * MSCS-535 Secure Software Development, Fall 2026. Ananya Solanki.
 *
 * Creates the customer_db database and a customer table with name and address
 * fields through JDBC, following the Oracle JDBC Basics tutorial pattern:
 * connect, create statements, execute, process results, close resources.
 *
 * Connection settings come from environment variables (PGHOST, PGPORT,
 * PGUSER, PGPASSWORD), so no credentials are written in the source code.
 */
public class CustomerDatabase {

    private static final String DB_NAME = "customer_db";

    public static void main(String[] args) throws SQLException {
        String host = env("PGHOST", "localhost");
        String port = env("PGPORT", "5432");
        String user = env("PGUSER", "postgres");
        String password = env("PGPASSWORD", "");
        String serverUrl = "jdbc:postgresql://" + host + ":" + port + "/postgres";
        String dbUrl = "jdbc:postgresql://" + host + ":" + port + "/" + DB_NAME;

        // Step 1 (DDL): create the database. CREATE DATABASE cannot run inside a
        // transaction, so it uses the default auto-commit connection.
        try (Connection server = DriverManager.getConnection(serverUrl, user, password);
             Statement st = server.createStatement()) {
            st.executeUpdate("DROP DATABASE IF EXISTS " + DB_NAME);
            st.executeUpdate("CREATE DATABASE " + DB_NAME);
            System.out.println("Database " + DB_NAME + " created.");
        }

        try (Connection con = DriverManager.getConnection(dbUrl, user, password)) {

            // Step 2 (DDL): create the table with name and address fields.
            try (Statement st = con.createStatement()) {
                st.executeUpdate(
                    "CREATE TABLE customer ("
                  + " cust_num   INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,"
                  + " first_name VARCHAR(30) NOT NULL,"
                  + " last_name  VARCHAR(30) NOT NULL,"
                  + " street     VARCHAR(60) NOT NULL,"
                  + " city       VARCHAR(30) NOT NULL,"
                  + " state      CHAR(2)     NOT NULL CHECK (state ~ '^[A-Z]{2}$'),"
                  + " zip        CHAR(5)     NOT NULL CHECK (zip ~ '^[0-9]{5}$'))");
                System.out.println("Table customer created.");
            }

            // Step 3 (DML): insert rows with a PreparedStatement. The values are
            // bound as parameters, so user input is never treated as SQL code.
            String insert = "INSERT INTO customer (first_name, last_name, street, city, state, zip)"
                          + " VALUES (?, ?, ?, ?, ?, ?)";
            try (PreparedStatement ps = con.prepareStatement(insert)) {
                addCustomer(ps, "John", "Smith", "112 Anywhere St", "Miami", "FL", "33333");
                addCustomer(ps, "Christy", "Jones", "2 Singleton Ave", "Orlando", "FL", "32801");
                // An injection attempt in the last name is stored as plain text.
                addCustomer(ps, "Robert", "Lee'); DROP TABLE customer;--",
                            "9 Harbor Rd", "Tampa", "FL", "33602");
                System.out.println("3 rows inserted.");
            }

            // Step 4 (DML): query the table and print each row.
            printCustomers(con);

            // Step 5: the CHECK constraint rejects a malformed ZIP code.
            try (PreparedStatement ps = con.prepareStatement(insert)) {
                addCustomer(ps, "Test", "User", "1 Main St", "Tampa", "FL", "3360A");
                System.out.println("ERROR: the invalid ZIP code was accepted.");
                System.exit(1);
            } catch (SQLException e) {
                System.out.println("Invalid ZIP code rejected (SQLState " + e.getSQLState() + "): "
                                   + e.getMessage().split("\n")[0]);
            }
        }
    }

    private static void addCustomer(PreparedStatement ps, String first, String last, String street,
                                    String city, String state, String zip) throws SQLException {
        ps.setString(1, first);
        ps.setString(2, last);
        ps.setString(3, street);
        ps.setString(4, city);
        ps.setString(5, state);
        ps.setString(6, zip);
        ps.executeUpdate();
    }

    private static void printCustomers(Connection con) throws SQLException {
        String query = "SELECT cust_num, first_name, last_name, street, city, state, zip"
                     + " FROM customer ORDER BY cust_num";
        try (Statement st = con.createStatement(); ResultSet rs = st.executeQuery(query)) {
            System.out.printf("%-4s %-8s %-36s %-16s %-8s %-3s %s%n",
                              "NUM", "FIRST", "LAST", "STREET", "CITY", "ST", "ZIP");
            while (rs.next()) {
                System.out.printf("%-4d %-8s %-36s %-16s %-8s %-3s %s%n",
                                  rs.getInt("cust_num"), rs.getString("first_name"),
                                  rs.getString("last_name"), rs.getString("street"),
                                  rs.getString("city"), rs.getString("state"), rs.getString("zip"));
            }
        }
    }

    private static String env(String name, String fallback) {
        String value = System.getenv(name);
        return (value == null || value.isEmpty()) ? fallback : value;
    }
}
