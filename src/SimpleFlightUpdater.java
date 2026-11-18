import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Scanner;

/**
 * Updates the delay status of flight {@code LX354} in the {@code Arrivals} table of {@code FlightDB}.
 *
 * <h2>Security audit: why the previous version was vulnerable</h2>
 * <p>
 * The original implementation used a plain {@link java.sql.Statement} and built the SQL command by
 * string concatenation:
 * </p>
 * <pre>
 * "UPDATE Arrivals SET delay_status = '" + userInput + "' WHERE flight_number = 'LX354'"
 * </pre>
 * <p>
 * Because the user's input was pasted directly into the SQL text, the input could change the
 * <em>structure</em> of the command, not just its value. With the payload {@code Landed' #} the
 * database received:
 * </p>
 * <pre>
 * UPDATE Arrivals SET delay_status = 'Landed' #' WHERE flight_number = 'LX354'
 * </pre>
 * <p>
 * The single quote closed the string literal early, and {@code #} started a MySQL single-line comment,
 * so the whole {@code WHERE} clause was ignored and all 299 rows were overwritten (SQL Injection).
 * </p>
 *
 * <h2>How PreparedStatement prevents SQL Injection</h2>
 * <p>
 * This version uses a {@link PreparedStatement} with a {@code ?} placeholder. The SQL text is sent to
 * the server and compiled <em>before</em> any user data is attached, so the query structure is fixed.
 * The value is then bound with {@link PreparedStatement#setString(int, String)}, which transmits it
 * strictly as data. Quotes, {@code #}, {@code --} and other SQL syntax in the input are treated as
 * ordinary characters of the string and can never become part of the command. The same payload
 * {@code Landed' #} is now stored literally, and only the row for {@code LX354} is affected.
 * </p>
 * <p>
 * Connection and PreparedStatement are opened with try-with-resources so they are always closed,
 * even if an exception occurs.
 * </p>
 */
public class SimpleFlightUpdater {

    /** JDBC URL of the FlightDB schema. */
    private static final String URL =
            "jdbc:mysql://localhost:3306/FlightDB?allowLoadLocalInfile=true&allowPublicKeyRetrieval=true&serverTimezone=Asia/Jerusalem&useSSL=false";

    /** MySQL user name. */
    private static final String USER = "root";

    /** Parameterized update: the {@code ?} is filled only through setString, never by concatenation. */
    private static final String UPDATE_SQL =
            "UPDATE Arrivals SET delay_status = ? WHERE flight_number = ?";

    /**
     * Prompts for the MySQL root password and a new delay status, then safely updates flight LX354.
     *
     * @param args command-line arguments (not used)
     */
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        System.out.print("Enter MySQL root password: ");
        String password = scanner.nextLine();

        System.out.print("Enter new delay status for flight LX354: ");
        String userInput = scanner.nextLine();

        try (Connection conn = DriverManager.getConnection(URL, USER, password);
             PreparedStatement pstmt = conn.prepareStatement(UPDATE_SQL)) {

            pstmt.setString(1, userInput);   // bound as data, cannot alter the SQL
            pstmt.setString(2, "LX354");

            int rowsAffected = pstmt.executeUpdate();
            System.out.println("Rows affected: " + rowsAffected);

        } catch (SQLException e) {
            System.err.println("Database error: " + e.getMessage());
        }
    }
}
