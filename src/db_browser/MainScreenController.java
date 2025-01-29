/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package db_browser;

import javafx.geometry.Insets;
import java.net.URL;
import java.sql.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.stream.Collectors;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.layout.GridPane;

public class MainScreenController implements Initializable {

    @FXML private ListView<String> databaseList;
    @FXML private ListView<String> tableList;
    @FXML private TableView<ObservableList<String>> dataTable;

    private Connection connection;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        databaseList.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                loadTables(newValue);
            }
        });

        tableList.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                loadTableData(databaseList.getSelectionModel().getSelectedItem(), newValue);
            }
        });
    }

    public void setConnection(Connection connection) {
        this.connection = connection;
    }

    public void loadDatabases() {
        try {
            DatabaseMetaData metaData = connection.getMetaData();
            ResultSet resultSet = metaData.getCatalogs();
            ObservableList<String> databases = FXCollections.observableArrayList();

            while (resultSet.next()) {
                databases.add(resultSet.getString("TABLE_CAT"));
            }

            databaseList.setItems(databases);
        } catch (SQLException e) {
            showError("Database Error", "Failed to load databases", e.getMessage());
        }
    }

    private void loadTables(String database) {
        try {
            connection.setCatalog(database);
            DatabaseMetaData metaData = connection.getMetaData();
            ResultSet resultSet = metaData.getTables(database, null, null, new String[]{"TABLE"});

            ObservableList<String> tables = FXCollections.observableArrayList();
            while (resultSet.next()) {
                tables.add(resultSet.getString("TABLE_NAME"));
            }

            tableList.setItems(tables);
        } catch (SQLException e) {
            showError("Database Error", "Failed to load tables", e.getMessage());
        }
    }

    private void loadTableData(String database, String table) {
        try {
            Statement stmt = connection.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT * FROM " + table);
            dataTable.getColumns().clear();

            ResultSetMetaData rsmd = rs.getMetaData();
            int columnCount = rsmd.getColumnCount();

            for (int i = 1; i <= columnCount; i++) {
                final int index = i - 1;
                TableColumn<ObservableList<String>, String> column = new TableColumn<>(rsmd.getColumnName(i));
                column.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().get(index)));
                dataTable.getColumns().add(column);
            }

            ObservableList<ObservableList<String>> data = FXCollections.observableArrayList();
            while (rs.next()) {
                ObservableList<String> row = FXCollections.observableArrayList();
                for (int i = 1; i <= columnCount; i++) {
                    row.add(rs.getString(i));
                }
                data.add(row);
            }

            dataTable.setItems(data);
        } catch (SQLException e) {
            showError("Database Error", "Failed to load table data", e.getMessage());
        }
    }

    @FXML
    private void handleInsert(ActionEvent event) {
        String selectedTable = tableList.getSelectionModel().getSelectedItem();
        String selectedDatabase = databaseList.getSelectionModel().getSelectedItem();
    
        if (selectedTable == null || selectedDatabase == null) {
            showError("Selection Error", "No table or database selected", "Please select both a database and table.");
            return;
        }

  
        Dialog<List<String>> dialog = new Dialog<>();
        dialog.setTitle("Insert Data");
        dialog.setHeaderText("Insert New Row");

    
        ButtonType submitButton = new ButtonType("Submit", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(submitButton, ButtonType.CANCEL);

  
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

    
        List<TextField> textFields = new ArrayList<>();
        for (int i = 0; i < dataTable.getColumns().size(); i++) {
            String columnName = dataTable.getColumns().get(i).getText();
            TextField field = new TextField();
            field.setPromptText(columnName);
        
            if (columnName.equals("bDate")) {
                field.setPromptText("YYYY-MM-DD");
            } else {
                field.setPromptText(columnName);
            }
        
            grid.add(new Label(columnName + ":"), 0, i);
            grid.add(field, 1, i);
            textFields.add(field);
        }

        dialog.getDialogPane().setContent(grid);

    
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == submitButton) {
                return textFields.stream()
                        .map(TextField::getText)
                        .collect(Collectors.toList());
            }
            return null;
        });

        Optional<List<String>> result = dialog.showAndWait();
        result.ifPresent(values -> {
            try {
            
                connection.setCatalog(selectedDatabase);
            
                String[] columns = dataTable.getColumns().stream()
                        .map(TableColumn::getText)
                        .toArray(String[]::new);
                String columnNames = String.join(", ", columns);
                String placeholders = String.join(", ", Collections.nCopies(columns.length, "?"));

                String insertSQL = "INSERT INTO `" + selectedDatabase + "`.`" + selectedTable + "` (" + columnNames + ") VALUES (" + placeholders + ")";
                PreparedStatement pStatement = connection.prepareStatement(insertSQL);

                for (int i = 0; i < columns.length; i++) {
                    pStatement.setString(i + 1, values.get(i));
                }

                pStatement.executeUpdate();
                loadTableData(selectedDatabase, selectedTable);
                showSuccess("Success", "Data inserted successfully!");
            } catch (SQLException e) {
                showError("Insert Error", "Failed to insert data", e.getMessage());
            }
        });
    }

    @FXML
    private void handleUpdate(ActionEvent event) {
        ObservableList<String> selectedRow = dataTable.getSelectionModel().getSelectedItem();
        if (selectedRow == null) {
            showError("Selection Error", "No row selected", "Please select a row to update.");
            return;
        }

        String selectedTable = tableList.getSelectionModel().getSelectedItem();
        String selectedDatabase = databaseList.getSelectionModel().getSelectedItem();
    
        if (selectedTable == null || selectedDatabase == null) {
             showError("Selection Error", "No table or database selected", "Please select both a database and table.");
            return;
        }

   
        ObservableList<TableColumn<ObservableList<String>, ?>> columns = dataTable.getColumns();
    
    
        Dialog<List<String>> dialog = new Dialog<>();
        dialog.setTitle("Update Data");
        dialog.setHeaderText("Update Row");

   
        ButtonType updateButtonType = new ButtonType("Update", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(updateButtonType, ButtonType.CANCEL);

   
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

   
        List<TextField> textFields = new ArrayList<>();
        for (int i = 0; i < columns.size(); i++) {
        
            TextField field;
            if (selectedRow.get(i).equals("bDate")) {
                field = new TextField("YYYY-MM-DD");
            } else {
                field = new TextField(selectedRow.get(i));
            }
        

            textFields.add(field);
            grid.add(new Label(columns.get(i).getText() + ":"), 0, i);
            grid.add(field, 1, i);
        }

        dialog.getDialogPane().setContent(grid);

   
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == updateButtonType) {
                return textFields.stream()
                        .map(TextField::getText)
                        .collect(Collectors.toList());
            }
            return null;
        });

        dialog.showAndWait().ifPresent(newValues -> {
            try {
        
                connection.setCatalog(selectedDatabase);
            
          
                StringBuilder updateSQL = new StringBuilder("UPDATE `" + selectedDatabase + "`.`" + selectedTable + "` SET ");
                List<String> setClauses = new ArrayList<>();
            
                for (int i = 0; i < columns.size(); i++) {
                    setClauses.add(columns.get(i).getText() + " = ?");
                }
            
                updateSQL.append(String.join(", ", setClauses));
                updateSQL.append(" WHERE " + columns.get(0).getText() + " = ?"); 

           
                PreparedStatement pStatement = connection.prepareStatement(updateSQL.toString());
            
           
                for (int i = 0; i < newValues.size(); i++) {
                    pStatement.setString(i + 1, newValues.get(i));
                }
           
                pStatement.setString(newValues.size() + 1, selectedRow.get(0));

                pStatement.executeUpdate();
                loadTableData(selectedDatabase, selectedTable);
                showSuccess("Success", "Record updated successfully");
            } catch (SQLException e) {
                showError("Update Error", "Failed to update data", e.getMessage());
            }
        });
    }

    @FXML
    private void handleDelete(ActionEvent event) {
        ObservableList<String> selectedRow = dataTable.getSelectionModel().getSelectedItem();
        if (selectedRow == null) {
            showError("Selection Error", "No row selected", "Please select a row to delete.");
            return;
        }

        String selectedTable = tableList.getSelectionModel().getSelectedItem();
        String selectedDatabase = databaseList.getSelectionModel().getSelectedItem();
    
        if (selectedTable == null || selectedDatabase == null) {
            showError("Selection Error", "No table or database selected", "Please select both a database and table.");
            return;
        }

   
        Alert confirmDialog = new Alert(Alert.AlertType.CONFIRMATION);
        confirmDialog.setTitle("Confirm Delete");
        confirmDialog.setHeaderText("Delete Record");
        confirmDialog.setContentText("Are you sure you want to delete this record?");

        Optional<ButtonType> result = confirmDialog.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
           
                connection.setCatalog(selectedDatabase);
            
           
                DatabaseMetaData metaData = connection.getMetaData();
                ResultSet primaryKeys = metaData.getPrimaryKeys(selectedDatabase, null, selectedTable);
            
                if (!primaryKeys.next()) {
                    showError("Delete Error", "No primary key", "Table must have a primary key to delete records.");
                    return;
                }
            
                String pkColumnName = primaryKeys.getString("COLUMN_NAME");
                int pkColumnIndex = -1;
            
         
                for (int i = 0; i < dataTable.getColumns().size(); i++) {
                    if (dataTable.getColumns().get(i).getText().equals(pkColumnName)) {
                        pkColumnIndex = i;
                        break;
                    }
                }
            
                if (pkColumnIndex == -1) {
                    showError("Delete Error", "Column not found", "Primary key column not found in table view.");
                    return;
                }

                String primaryKeyValue = selectedRow.get(pkColumnIndex);
                String deleteSQL = "DELETE FROM `" + selectedDatabase + "`.`" + selectedTable + "` WHERE " + pkColumnName + " = ?";
                PreparedStatement pStatement = connection.prepareStatement(deleteSQL);
                pStatement.setString(1, primaryKeyValue);
                pStatement.executeUpdate();
            
                loadTableData(selectedDatabase, selectedTable);
                showSuccess("Success", "Record deleted successfully");
            } catch (SQLException e) {
                showError("Delete Error", "Failed to delete data", e.getMessage());
            }
        }
    }

    private void showSuccess(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showError(String title, String header, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }
}