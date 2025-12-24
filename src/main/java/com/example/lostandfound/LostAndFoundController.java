package com.example.lostandfound;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.Region;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;

import java.util.Optional;
import java.util.Map;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class LostAndFoundController {

    @FXML
    private TextArea outputArea;

    @FXML
    private Label activeItemsCount;

    @FXML
    private Label todayItemsCount;

    @FXML
    private Label pendingItemsCount;

    private final LostAndFoundSystem system = new LostAndFoundSystem();

    // === Color Scheme (Matches Updated FXML) ===
    private static final String DARK_BG = "#0f172a";
    private static final String CARD_BG = "#1e293b";
    private static final String LIGHT_TEXT = "#f1f5f9";
    private static final String MUTED_TEXT = "#94a3b8";
    private static final String BORDER_COLOR = "#334155";
    private static final String PRIMARY_BLUE = "#3b82f6";
    private static final String SUCCESS_GREEN = "#10b981";
    private static final String WARNING_ORANGE = "#f59e0b";
    private static final String ERROR_RED = "#ef4444";
    private static final String GRAY = "#6b7280";

    // === Initialize ===
    @FXML
    public void initialize() {
        // Load saved data
        system.loadData();

        updateStatistics();
        outputArea.setText("""
                🎉 Welcome to the Lost and Found System!

                This system helps you manage found items with qualification checks to ensure fairness and efficiency.

                📋 Available Actions:
                • 📝 Submit Found Item – Add new qualified items
                • 🔍 Search Lost Item – Find items using keywords
                • 📋 List Items – View all items in storage
                • 🗑️ Remove Item – Delete existing records
                • 📖 View Criteria – Review item acceptance standards

                💡 Tip: Always check the criteria before submitting!
                🚀 System ready — use the tabs above to begin.
                """);
    }

    // === Statistics ===
    private void updateStatistics() {
        int activeCount = system.getActiveItemsCount();
        int todayCount = system.getTodayItemsCount();
        int pendingCount = system.getPendingItemsCount();

        activeItemsCount.setText(activeCount + " items");
        todayItemsCount.setText(todayCount + " today");
        pendingItemsCount.setText(pendingCount + " items");
        logActivity("Statistics updated");
    }

    private void logActivity(String message) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        outputArea.appendText("[" + timestamp + "] " + message + "\n");
    }

    // === Unified Dialog Styler ===
    private void styleDialog(Dialog<?> dialog, String accentColor) {
        DialogPane pane = dialog.getDialogPane();
        String accent = (accentColor != null) ? accentColor : PRIMARY_BLUE;

        String style = ""
                + "-fx-background-color: " + CARD_BG + "; "
                + "-fx-border-color: " + accent + "; "
                + "-fx-border-width: 2; "
                + "-fx-border-radius: 10; "
                + "-fx-background-radius: 10; "
                + "-fx-padding: 20;";

        pane.setStyle(style);

        // Text content style
        pane.lookupAll(".label").forEach(node -> {
            node.setStyle("-fx-text-fill: " + LIGHT_TEXT + "; -fx-font-size: 13px;");
        });

        if (pane.lookup(".header-panel") != null)
            pane.lookup(".header-panel").setStyle("-fx-background-color: transparent;");

        // Text input field styling
        TextField field = (TextField) pane.lookup(".text-field");
        if (field != null) {
            String style1 = "-fx-control-inner-background: " + DARK_BG + "; "
                    + "-fx-text-fill: " + LIGHT_TEXT + "; "
                    + "-fx-border-color: " + BORDER_COLOR + "; "
                    + "-fx-border-width: 1; "
                    + "-fx-border-radius: 8; "
                    + "-fx-background-radius: 8; "
                    + "-fx-padding: 6 10;";
            field.setStyle(style1);
        }

        // Button styles
        for (ButtonType type : pane.getButtonTypes()) {
            Button btn = (Button) pane.lookupButton(type);
            if (btn != null) {
                String btnColor = switch (type.getButtonData()) {
                    case CANCEL_CLOSE -> GRAY;
                    case OK_DONE -> accent;
                    default -> PRIMARY_BLUE;
                };
                String styles = "-fx-background-color: " + btnColor + "; "
                        + "-fx-text-fill: white; "
                        + "-fx-font-weight: bold; "
                        + "-fx-background-radius: 8; "
                        + "-fx-padding: 8 16; "
                        + "-fx-cursor: hand;";
                btn.setStyle(styles);
            }
        }

        // Dialog size & layout tweaks
        pane.setPrefWidth(480);
        pane.setPrefHeight(Region.USE_COMPUTED_SIZE);
    }

    private void showError(String msg) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("❌ Error");
        alert.setHeaderText("Something went wrong");
        alert.setContentText(msg);
        styleDialog(alert, ERROR_RED);
        alert.showAndWait();
    }

    private void showInformationDialog(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.getDialogPane().setPrefSize(600, 500);
        styleDialog(alert, PRIMARY_BLUE);
        alert.showAndWait();
    }

    private void showSuccessDialog(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText("✅ Success");
        alert.setContentText(content);
        styleDialog(alert, SUCCESS_GREEN);
        alert.showAndWait();
    }

    // === Core Button Handlers ===
    @FXML
    private void handleSubmitFoundItem() {
        try {
            // Create custom submission dialog
            Dialog<Map<String, Object>> dialog = new Dialog<>();
            dialog.setTitle("📝 Submit Found Item");
            dialog.setHeaderText("Complete the form below to submit a found item");

            // Create form layout
            GridPane grid = new GridPane();
            grid.setHgap(15);
            grid.setVgap(12);
            grid.setPadding(new javafx.geometry.Insets(20));

            // Form fields
            TextField nameField = new TextField();
            nameField.setPromptText("e.g., Black Wallet");

            TextField descField = new TextField();
            descField.setPromptText("Describe the item uniquely");

            ComboBox<String> categoryCombo = new ComboBox<>();
            categoryCombo.getItems().addAll(LostAndFoundSystem.CATEGORIES);
            categoryCombo.setValue("Others");

            TextField locField = new TextField();
            locField.setPromptText("e.g., Library 3rd Floor");

            DatePicker datePicker = new DatePicker();
            datePicker.setValue(java.time.LocalDate.now());

            TextField valueField = new TextField();
            valueField.setPromptText("e.g., 1500");

            CheckBox perishBox = new CheckBox();

            TextField contactField = new TextField();
            contactField.setPromptText("Phone or email");

            // Add to grid
            int row = 0;
            grid.add(new Label("Item Name:"), 0, row);
            grid.add(nameField, 1, row++);

            grid.add(new Label("Description:"), 0, row);
            grid.add(descField, 1, row++);

            grid.add(new Label("Category:"), 0, row);
            grid.add(categoryCombo, 1, row++);

            grid.add(new Label("Location Found:"), 0, row);
            grid.add(locField, 1, row++);

            grid.add(new Label("Date Found:"), 0, row);
            grid.add(datePicker, 1, row++);

            grid.add(new Label("Estimated Value (₱):"), 0, row);
            grid.add(valueField, 1, row++);

            grid.add(new Label("Perishable:"), 0, row);
            HBox perishBox_container = new HBox(10);
            perishBox_container.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
            perishBox_container.getChildren().add(perishBox);
            Label perishNote = new Label("(Item must NOT be perishable)");
            perishNote.setStyle("-fx-text-fill: #ef4444; -fx-font-size: 10px; -fx-font-style: italic;");
            perishBox_container.getChildren().add(perishNote);
            grid.add(perishBox_container, 1, row++);

            grid.add(new Label("Contact Info:"), 0, row);
            grid.add(contactField, 1, row++);

            // Add info label
            Label infoLabel = new Label("💡 Your item will be evaluated against qualification criteria");
            infoLabel.setStyle("-fx-text-fill: #3b82f6; -fx-font-size: 11px;");
            infoLabel.setWrapText(true);
            grid.add(infoLabel, 0, row++, 2, 1);

            dialog.getDialogPane().setContent(grid);
            dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

            // Style the dialog
            styleDialog(dialog, PRIMARY_BLUE);

            // Result converter
            dialog.setResultConverter(buttonType -> {
                if (buttonType == ButtonType.OK) {
                    Map<String, Object> result = new java.util.HashMap<>();
                    result.put("name", nameField.getText());
                    result.put("desc", descField.getText());
                    result.put("category", categoryCombo.getValue());
                    result.put("location", locField.getText());
                    result.put("date", datePicker.getValue());
                    result.put("value", valueField.getText());
                    result.put("perishable", perishBox.isSelected());
                    result.put("contact", contactField.getText());
                    return result;
                }
                return null;
            });

            // Show dialog and process result
            Optional<Map<String, Object>> result = dialog.showAndWait();

            if (result.isEmpty()) {
                logActivity("Item submission cancelled");
                return;
            }

            Map<String, Object> data = result.get();

            // Validation
            String name = (String) data.get("name");
            String desc = (String) data.get("desc");
            String location = (String) data.get("location");
            String contact = (String) data.get("contact");

            if (name.trim().isEmpty() || desc.trim().isEmpty() || location.trim().isEmpty()
                    || contact.trim().isEmpty()) {
                showError("All fields except perishable are required.");
                return;
            }

            double value;
            try {
                value = Double.parseDouble((String) data.get("value"));
            } catch (NumberFormatException e) {
                showError("Invalid value. Please enter a valid number.");
                return;
            }

            String category = (String) data.get("category");
            java.time.LocalDate dateFound = (java.time.LocalDate) data.get("date");
            boolean perish = (Boolean) data.get("perishable");

            // Submit to system
            String message = system.guiSubmitFoundItem(name, desc, category, location, dateFound, value, perish,
                    contact);

            if (message.toLowerCase().contains("not accepted") || message.toLowerCase().contains("rejected")) {
                showError("Item submission failed:\n\n" + message);
                logActivity("Item submission rejected");
            } else {
                showSuccessDialog("Item Accepted", "✅ " + message);
                logActivity("New item successfully submitted");
                updateStatistics();
            }
        } catch (Exception e) {
            showError("An unexpected error occurred: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void handleSearchLostItem() {
        try {
            // Create custom search dialog
            Dialog<Map<String, String>> dialog = new Dialog<>();
            dialog.setTitle("🔍 Search Lost Item");
            dialog.setHeaderText("Enter details to search for your lost item");

            // Create form layout
            GridPane grid = new GridPane();
            grid.setHgap(15);
            grid.setVgap(12);
            grid.setPadding(new javafx.geometry.Insets(20));

            // Form fields
            TextField nameField = new TextField();
            nameField.setPromptText("Item Name (optional)");

            TextField descField = new TextField();
            descField.setPromptText("Enter key description (required)");

            TextField locField = new TextField();
            locField.setPromptText("Location found (optional)");

            // Add to grid
            grid.add(new Label("Item Name"), 0, 0);
            grid.add(nameField, 1, 0);

            grid.add(new Label("Description"), 0, 1);
            grid.add(descField, 1, 1);

            grid.add(new Label("Location"), 0, 2);
            grid.add(locField, 1, 2);

            dialog.getDialogPane().setContent(grid);
            dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

            // Style the dialog
            styleDialog(dialog, SUCCESS_GREEN);

            // Result converter
            dialog.setResultConverter(buttonType -> {
                if (buttonType == ButtonType.OK) {
                    Map<String, String> results = new java.util.HashMap<>();
                    results.put("name", nameField.getText());
                    results.put("desc", descField.getText());
                    results.put("loc", locField.getText());
                    return results;
                }
                return null;
            });

            // Show dialog and process result
            Optional<Map<String, String>> result = dialog.showAndWait();

            if (result.isPresent()) {
                Map<String, String> data = result.get();
                String name = data.get("name");
                String desc = data.get("desc");
                String loc = data.get("loc");

                if (desc.trim().isEmpty()) {
                    showError("Description is required for search.");
                    return;
                }

                String message = system.guiSearchLostItem(name, desc, loc);
                showInformationDialog("Search Results", message);

                if (message.contains("No matches found")) {
                    logActivity("Search completed – no matches for: " + desc);
                } else {
                    logActivity("Search completed – matches found for: " + desc);
                }
            }
        } catch (Exception e) {
            showError("An unexpected search error occurred: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void handleListFoundItems() {
        String items = system.guiListItems(false); // Show only ACTIVE items by default
        showInformationDialog("📋 Current Found Items", items);
        logActivity("Listed all found items");

        updateStatistics();
    }

    @FXML
    private void handleRemoveItem() {
        try {
            // Get all active items
            // Get active items directly
            java.util.List<String[]> activeItems = system.getActiveItemsForDialog();

            if (activeItems.isEmpty()) {
                showInformationDialog("No Items", "There are no active items to remove.");
                return;
            }

            // Create custom dialog with checkboxes
            Dialog<java.util.List<String>> dialog = new Dialog<>();
            dialog.setTitle("🗑️ Remove Items");
            dialog.setHeaderText("Select items to remove (you can select multiple)");

            // Create scroll pane with checkboxes
            javafx.scene.layout.VBox vbox = new javafx.scene.layout.VBox(10);
            vbox.setPadding(new javafx.geometry.Insets(15));
            vbox.setStyle("-fx-background-color: " + DARK_BG + ";");

            java.util.Map<CheckBox, String> checkBoxMap = new java.util.HashMap<>();

            for (String[] itemData : activeItems) {
                String id = itemData[0];
                String display = itemData[1];

                CheckBox cb = new CheckBox(display);
                cb.setStyle("-fx-text-fill: " + LIGHT_TEXT + "; -fx-font-size: 12px;");
                checkBoxMap.put(cb, id);
                vbox.getChildren().add(cb);
            }

            ScrollPane scrollPane = new ScrollPane(vbox);
            scrollPane.setFitToWidth(true);
            scrollPane.setPrefHeight(300);
            scrollPane.setStyle("-fx-background: " + DARK_BG + "; -fx-border-color: " + BORDER_COLOR + ";");

            dialog.getDialogPane().setContent(scrollPane);
            dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

            styleDialog(dialog, ERROR_RED);

            // Result converter
            dialog.setResultConverter(buttonType -> {
                if (buttonType == ButtonType.OK) {
                    java.util.List<String> selectedIds = new java.util.ArrayList<>();
                    for (java.util.Map.Entry<CheckBox, String> entry : checkBoxMap.entrySet()) {
                        if (entry.getKey().isSelected()) {
                            selectedIds.add(entry.getValue());
                        }
                    }
                    return selectedIds;
                }
                return null;
            });

            // Show dialog and process
            Optional<java.util.List<String>> result = dialog.showAndWait();

            if (result.isPresent() && !result.get().isEmpty()) {
                java.util.List<String> idsToRemove = result.get();

                // Confirm removal
                Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
                confirm.setTitle("Confirm Removal");
                confirm.setHeaderText("Remove " + idsToRemove.size() + " item(s)?");
                confirm.setContentText("This action will mark the selected items as REMOVED. Are you sure?");
                styleDialog(confirm, WARNING_ORANGE);

                Optional<ButtonType> confirmResult = confirm.showAndWait();
                if (confirmResult.isPresent() && confirmResult.get() == ButtonType.OK) {
                    int removed = 0;
                    for (String id : idsToRemove) {
                        String message = system.guiRemoveItem(id);
                        if (!message.toLowerCase().contains("not found")) {
                            removed++;
                        }
                    }

                    showSuccessDialog("Items Removed",
                            String.format("Successfully removed %d item(s)!", removed));
                    logActivity(removed + " item(s) removed");
                    updateStatistics();
                }
            } else if (result.isPresent()) {
                showInformationDialog("No Selection", "No items were selected for removal.");
            }

        } catch (Exception e) {
            showError("Error removing items: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void handleViewCriteria() {
        String criteria = getQualificationCriteria();
        showInformationDialog("📖 Qualification Criteria", criteria);
        logActivity("Viewed qualification criteria");
    }

    @FXML
    private void handleClearLog() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("🗑️ Clear Activity Log");
        alert.setHeaderText("Confirm Log Clearance");
        alert.setContentText("Are you sure you want to clear the activity log? This cannot be undone.");
        styleDialog(alert, WARNING_ORANGE);

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            outputArea.clear();
            logActivity("Activity log cleared");
        }
    }

    // === Qualification Criteria Text ===
    private String getQualificationCriteria() {
        return """
                LOST AND FOUND SYSTEM - QUALIFICATION CRITERIA

                ACCEPTABLE ITEMS:
                • Handbags, wallets, purses, backpacks, briefcases
                • Identification cards, passports, licenses, credit cards
                • Mobile phones, tablets, laptops, computers
                • Books, textbooks, notebooks, documents
                • Accessories: watches, jewelry, glasses, keys
                • Clothing items: shirts, pants, jackets, shoes
                • Electronic accessories: chargers, headphones, cameras

                EXCLUDED ITEMS:
                • Food and beverages (all types)
                • Low-cost disposable products (tissues, wrappers, packaging)
                • Digital or virtual items (software, apps, online content)

                LOCATION REQUIREMENTS:
                • Must be found within campus or institution premises

                VALUE AND IMPORTANCE:
                • Minimum value threshold: ₱50.00
                • Perishables: Minimum ₱100.00
                • Must have notable importance (phones, IDs, laptops, etc.)

                CONTACT REQUIREMENTS:
                • Must include valid phone or email contact

                All criteria must be met for item acceptance.
                """;
    }
}
