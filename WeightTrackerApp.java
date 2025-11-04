import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.table.DefaultTableModel;

public class WeightTrackerApp extends JFrame {

    // UI Components
   // private JTextField morningWeightField;
   // private JTextField nightWeightField;
   // private JButton saveButton;
    private JTextField weightField;       // One field
    private JButton saveMorningButton;    // First button
    private JButton saveNightButton;      // Second button
    private JTextArea analysisArea;
    private JButton dailyButton, weeklyButton, monthlyButton;
    private JButton viewAllButton; // New button for all entries
    private JButton resetButton;
    // --- REPLACEMENT CONSTRUCTOR ---
// --- REPLACEMENT CONSTRUCTOR ---
// --- REPLACEMENT CONSTRUCTOR ---
public WeightTrackerApp() {
    dbHelper = new DatabaseHelper();
    dbHelper.createNewTable(); // Create table on startup

    // --- Basic Window Setup ---
    setTitle("ScaleSync"); 
    setSize(500, 400);
    setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    setLocationRelativeTo(null); 
    setLayout(new BorderLayout(10, 10)); 

    // --- 1. Input Panel (Top) ---
    JPanel inputPanel = new JPanel(new GridLayout(2, 2, 5, 5));
    inputPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

    inputPanel.add(new JLabel("Current Weight (kg):"));
    weightField = new JTextField();
    inputPanel.add(weightField);

    saveMorningButton = new JButton("Save Morning Weight");
    inputPanel.add(saveMorningButton);

    saveNightButton = new JButton("Save Night Weight");
    inputPanel.add(saveNightButton);

    add(inputPanel, BorderLayout.NORTH);

    // --- 2. Analysis Panel (Center) ---
    analysisArea = new JTextArea("Analysis results will appear here.");
    analysisArea.setEditable(false);
    analysisArea.setMargin(new Insets(10, 10, 10, 10));
    JScrollPane scrollPane = new JScrollPane(analysisArea);
    add(scrollPane, BorderLayout.CENTER);

    // --- 3. Button Panel (Bottom) - (MODIFIED) ---
    JPanel buttonPanel = new JPanel(new FlowLayout());
    dailyButton = new JButton("Daily Analysis");
    weeklyButton = new JButton("Weekly Analysis");
    monthlyButton = new JButton("Monthly Analysis");
    viewAllButton = new JButton("View All Entries");
    resetButton = new JButton("Reset All"); // --- NEW ---

    buttonPanel.add(dailyButton);
    buttonPanel.add(weeklyButton);
    buttonPanel.add(monthlyButton);
    buttonPanel.add(viewAllButton);
    buttonPanel.add(resetButton); // --- NEW ---
    add(buttonPanel, BorderLayout.SOUTH);

    // --- 4. Add Action Listeners (The Logic) ---

    // Save Morning Button Logic
    saveMorningButton.addActionListener(new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
            handleSave("morning");
        }
    });

    // Save Night Button Logic
    saveNightButton.addActionListener(new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
            handleSave("night");
        }
    });

    // Daily Analysis Button
    dailyButton.addActionListener(e -> analysisArea.setText(dbHelper.getDailyAnalysis()));

    // Weekly Analysis Button
    weeklyButton.addActionListener(e -> analysisArea.setText(dbHelper.getWeeklyAnalysis()));

    // Monthly Analysis Button
    monthlyButton.addActionListener(e -> analysisArea.setText(dbHelper.getMonthlyAnalysis()));

    // View All Button
    viewAllButton.addActionListener(new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
            DefaultTableModel model = dbHelper.getAllEntries();
            JTable table = new JTable(model);
            table.setEnabled(false); 
            JScrollPane tableScrollPane = new JScrollPane(table);
            JDialog dialog = new JDialog(WeightTrackerApp.this, "All Entries", true);
            dialog.setSize(450, 300);
            dialog.setLocationRelativeTo(WeightTrackerApp.this); 
            dialog.add(tableScrollPane); 
            dialog.setVisible(true);
        }
    });

    // --- NEW ACTION LISTENER for RESET ---
    resetButton.addActionListener(new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
            // Show a confirmation dialog
            int response = JOptionPane.showConfirmDialog(
                WeightTrackerApp.this, 
                "Are you sure you want to delete ALL entries?\nThis action cannot be undone.", 
                "Confirm Reset", 
                JOptionPane.YES_NO_OPTION, 
                JOptionPane.WARNING_MESSAGE
            );

            if (response == JOptionPane.YES_OPTION) {
                // If user clicked Yes, call the delete method
                String result = dbHelper.deleteAllEntries();

                // Show the result in the analysis area
                analysisArea.setText(result);
            } else {
                // If user clicked No, do nothing
                analysisArea.setText("Reset canceled.");
            }
        }
    });
}
    // Database helper
    private DatabaseHelper dbHelper;

// --- NEW HELPER METHOD (add this inside the WeightTrackerApp class) ---
private void handleSave(String timeOfDay) {
    try {
        double weight = Double.parseDouble(weightField.getText());

        // Call our new database method
        String result = dbHelper.saveWeight(weight, timeOfDay);

        analysisArea.setText(result); // Show success message
        weightField.setText(""); // Clear field after saving
    } catch (NumberFormatException ex) {
        JOptionPane.showMessageDialog(null, 
            "Invalid input. Please enter numbers only.", 
            "Input Error", 
            JOptionPane.ERROR_MESSAGE);
    }
}

    // --- Main method to run the application ---
// --- Main method to run the application ---
    public static void main(String[] args) {

        // --- ADD THIS NEW CODE ---
        try {
            // This line makes your app look like a native Windows app
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }
        // --- END OF NEW CODE ---

        // Run the GUI creation on the Event Dispatch Thread for thread safety
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                new WeightTrackerApp().setVisible(true);
            }
        });
    }
}