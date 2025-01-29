package db_browser;

import java.net.URL;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ResourceBundle;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

/**
 *
 * @author birkanegee
 */

public class LoginScreenController implements Initializable {

    @FXML private TextField urlField;
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Button connectButton;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        urlField.setText("jdbc:mysql://localhost:3307");
        
    }

    @FXML
    private void handleConnect(ActionEvent event) {
        String url = urlField.getText();
        String username = usernameField.getText();
        String password = passwordField.getText();

        if (url.isEmpty() || username.isEmpty() || password.isEmpty()) {
            showError("Input Error", "Missing Information", "Please fill in all fields.");
            return;
        }

        try {
            
            Class.forName("com.mysql.cj.jdbc.Driver");
            
            Connection connection = DriverManager.getConnection(url, username, password);

            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Connection Successful");
            alert.setHeaderText(null);
            alert.setContentText("Successfully connected to the database.");
            alert.showAndWait();

            FXMLLoader loader = new FXMLLoader(getClass().getResource("MainScreen.fxml"));
            Parent mainScreen = loader.load();

            MainScreenController mainController = loader.getController();
            mainController.setConnection(connection);
            mainController.loadDatabases();

            Stage stage = (Stage) connectButton.getScene().getWindow();
            Scene scene = new Scene(mainScreen, 1000, 600);
            stage.setScene(scene);

        } catch (ClassNotFoundException e) {
            showError("Driver Error", "JDBC Driver Not Found", "Ensure that the MySQL JDBC driver is included in your project.");
        } catch (SQLException e) {
            showError("Database Connection Error", "Could not connect to the database", 
                "Ensure the URL, username, and password are correct.\nError: " + e.getMessage());
        } catch (Exception e) {
            showError("Unexpected Error", "An unexpected error occurred", e.getMessage());
        }
    }

    private void showError(String title, String header, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }
}

