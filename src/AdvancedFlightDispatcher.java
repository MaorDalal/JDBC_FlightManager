import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

public class AdvancedFlightDispatcher {

    private static final String URL =
            "jdbc:mysql://localhost:3306/FlightDB?allowLoadLocalInfile=true&allowPublicKeyRetrieval=true&serverTimezone=Asia/Jerusalem&useSSL=false";
    private static final String USER = "root";

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        System.out.print("Enter MySQL root password: ");
        String password = scanner.nextLine();

        try (Connection conn = DriverManager.getConnection(URL, USER, password)) {
            updateMultipleDelays(conn, Arrays.asList(1, 3, 5), "Landed - Final");
        } catch (SQLException e) {
            System.err.println("Database error: " + e.getMessage());
        }
    }

    public static void updateMultipleDelays(Connection conn, List<Integer> arrivalIds, String newStatus) throws SQLException {
        String sql = "UPDATE Arrivals SET delay_status = ? WHERE id = ?";

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            // Parameter 1 (status) is the same for every row: set it ONCE, outside the loop
            pstmt.setString(1, newStatus);

            for (Integer id : arrivalIds) {
                pstmt.setInt(2, id);   // parameter 2 changes per row
                pstmt.addBatch();
            }

            int[] results = pstmt.executeBatch();

            int totalAffected = 0;
            for (int r : results) {
                totalAffected += r;
            }

            System.out.println("Batch update complete: " + results.length + " statements executed, "
                    + totalAffected + " rows updated to '" + newStatus + "' (IDs " + arrivalIds + ").");
        }
    }
}
