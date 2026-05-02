import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

public class UnitsConverter implements ActionListener {

    JFrame frame;
    JPanel panel;
    JTextField inputField, outputField;
    JLabel inputLabel, outputLabel;
    JComboBox<String> comboBox;
    JButton convertButton;

    public UnitsConverter() {
        frame = new JFrame("Units Converter");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        panel = new JPanel(new GridLayout(5, 2, 10, 10));

        init();

        frame.add(panel);
        frame.setSize(1200, 500);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    private void init() {
        inputLabel = new JLabel("Enter Value:");
        inputField = new JTextField(10);

        outputLabel = new JLabel("Converted Value:");
        outputField = new JTextField(10);
        outputField.setEditable(false);

        String[] options = {
            "Celsius to Fahrenheit",
            "Meters to Feet",
            "Kilograms to Pounds",
            "Radians to Degrees"
        };

        comboBox = new JComboBox<>(options);

        convertButton = new JButton("Convert");
        convertButton.addActionListener(this);

        panel.add(inputLabel);
        panel.add(inputField);

        panel.add(new JLabel("Select Conversion:"));
        panel.add(comboBox);

        panel.add(outputLabel);
        panel.add(outputField);

        panel.add(new JLabel(""));
        panel.add(convertButton);
    }

    public void actionPerformed(ActionEvent e) {
        try {
            double input = Double.parseDouble(inputField.getText());
            double result = 0;

            String selected = (String) comboBox.getSelectedItem();

            if (selected.equals("Celsius to Fahrenheit")) {
                result = (input * 9 / 5) + 32;
            }
            else if (selected.equals("Meters to Feet")) {
                result = input * 3.28084;
            }
            else if (selected.equals("Kilograms to Pounds")) {
                result = input * 2.20462;
            }
            else if (selected.equals("Radians to Degrees")) {
                result = input * 180 / Math.PI;
            }

            outputField.setText(String.format("%.2f", result));

        } catch (NumberFormatException ex) {
            outputField.setText("Invalid Input");
        }
    }

    public static void main(String[] args) {
        // the window wasn't showing in linux without this line of code on linux
        SwingUtilities.invokeLater(() -> new UnitsConverter());
    }
}