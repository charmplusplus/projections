package projections.gui;

import projections.analysis.RangeHistory;

import javax.swing.DefaultListModel;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JButton;
import javax.swing.JTextField;
import javax.swing.JPanel;
import javax.swing.ListSelectionModel;
import javax.swing.JOptionPane;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.io.File;
import java.io.IOException;

public class RangeListWindow
        extends ProjectionsWindow
        implements ActionListener, ListSelectionListener{

    private static int myRun = 0;

    private RangeHistory history;
    private DefaultListModel listModel;

    private JList rangeList;
    private JButton addButton, removeButton, editButton, saveButton;
    private TimeTextField startTimeField, endTimeField;
    private JTextField nameField;
    private JSelectField processorField;

    RangeListWindow(MainWindow mainWindow) {
        super("Projections Range List Window - " + MainWindow.runObject[myRun].getFilename() + ".sts", mainWindow);
        history = new RangeHistory(MainWindow.runObject[myRun].getLogDirectory() + File.separator);

        createLayout();
        pack();
        setLocationRelativeTo(mainWindow);
        setVisible(true);
    }

    @SuppressWarnings("unchecked")
    private void createLayout() {
        GridBagConstraints gbc = new GridBagConstraints();
        GridBagLayout gbl = new GridBagLayout();

        gbc.fill = GridBagConstraints.BOTH;
        gbc.insets = new Insets(2, 2, 2, 2);

        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(gbl);

        listModel = new DefaultListModel();
        for(String o : history.getHistoryStrings())
            listModel.addElement(o);
        rangeList = new JList(listModel);
        rangeList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        rangeList.addListSelectionListener(this);
        rangeList.setBorder(new TitledBorder(new LineBorder(Color.black), "Range List"));

        JLabel startTimeLabel = new JLabel("Start Time:", JLabel.LEFT);
        startTimeField = new TimeTextField(" ", 12);
        JLabel endTimeLabel = new JLabel("End Time:", JLabel.LEFT);
        endTimeField = new TimeTextField(" ", 12);
        JLabel nameLabel = new JLabel("Name of Entry:", JLabel.LEFT);
        nameField = new JTextField("", 12);
        JLabel processorLabel = new JLabel("Processor Range:", JLabel.LEFT);
        if(MainWindow.runObject[myRun].hasLogData() || MainWindow.runObject[myRun].hasSumDetailData())
            processorField = new JSelectField(MainWindow.runObject[myRun].getValidProcessorString(), 12);

        JPanel infoPanel = new JPanel();
        infoPanel.setLayout(gbl);
        Util.gblAdd(infoPanel, startTimeLabel,
                gbc, 0, 0, 1, 1, 1, 1);
        Util.gblAdd(infoPanel, startTimeField,
                gbc, 1, 0, 1, 1, 1, 1);
        Util.gblAdd(infoPanel, endTimeLabel,
                gbc, 2, 0, 1, 1, 1, 1);
        Util.gblAdd(infoPanel, endTimeField,
                gbc, 3, 0, 1, 1, 1, 1);
        Util.gblAdd(infoPanel, nameLabel,
                gbc, 0, 1, 1, 1, 1, 1);
        Util.gblAdd(infoPanel, nameField,
                gbc, 1, 1, 1, 1, 1, 1);
        Util.gblAdd(infoPanel, processorLabel,
                gbc, 2, 1, 1, 1, 1, 1);
        Util.gblAdd(infoPanel, processorField,
                gbc, 3, 1, 1, 1, 1, 1);
        addButton = new JButton("Add new Range Entry");
        removeButton = new JButton("Remove selected Range Entry");
        editButton = new JButton("Edit selected Range Entry");
        saveButton = new JButton("Save changes to disk");
        addButton.addActionListener(this);
        removeButton.addActionListener(this);
        editButton.addActionListener(this);
        saveButton.addActionListener(this);

        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(gbl);
        Util.gblAdd(buttonPanel, addButton,
                gbc, 0, 0, 1, 1, 1, 1);
        Util.gblAdd(buttonPanel, removeButton,
                gbc, 1, 0, 1, 1, 1, 1);
        Util.gblAdd(buttonPanel, editButton,
                gbc, 2, 0, 1, 1, 1, 1);
        Util.gblAdd(buttonPanel, saveButton,
                gbc, 3, 0, 1, 1, 1, 1);

        Util.gblAdd(mainPanel, rangeList,
                gbc, 0, 0, 1, 1, 1, 1);
        Util.gblAdd(mainPanel, infoPanel,
                gbc, 0, 1, 1, 1, 1, 1);
        Util.gblAdd(mainPanel, buttonPanel,
                gbc, 0, 2, 1, 1, 0, 0);

        getContentPane().add(mainPanel);
    }

    @Override
    public void valueChanged(ListSelectionEvent listSelectionEvent) {
        int selection = rangeList.getSelectedIndex();
        if(selection == -1)
            return;
        startTimeField.setValue(history.getStartValue(selection));
        endTimeField.setValue(history.getEndValue(selection));
        nameField.setText(history.getName(selection));
        processorField.setText(history.getProcRange(selection));
        pack();
    }

    @SuppressWarnings("unchecked")
    @Override
    public void actionPerformed(ActionEvent ae) {
        if(ae.getSource() == addButton) {
            long startTime = startTimeField.getValue(), endTime = endTimeField.getValue();
            if(startTime >= endTime)
                JOptionPane.showMessageDialog(this, "Start Time must be less than End Time.");
            else if(processorField.getText().equals(""))
                JOptionPane.showMessageDialog(this, "Must specify a processor range.");
            else if(nameField.getText().equals(""))
                JOptionPane.showMessageDialog(this, "Must specify a name for the entry.");
            else {
                history.add(startTime, endTime, nameField.getText(), processorField.getText());
                String currString = getRangeString(startTime, endTime, nameField.getText(), processorField.getText());
                listModel.addElement(currString);
                pack();
            }
        } else if(ae.getSource() == removeButton) {
            if(rangeList.getModel().getSize() == 0)
                JOptionPane.showMessageDialog(this,
                        "Need at least one range entry to be removed.",
                        "Empty Range Entry List",
                        JOptionPane.ERROR_MESSAGE);
            else if(rangeList.isSelectionEmpty())
                JOptionPane.showMessageDialog(this, "Select a Range Entry from the list to be removed.");
            else {
                history.remove(rangeList.getSelectedIndex());
                listModel.removeElementAt(rangeList.getSelectedIndex());
                pack();
            }
        } else if(ae.getSource() == editButton) {
            long startTime = startTimeField.getValue(), endTime = endTimeField.getValue();
            if(rangeList.getModel().getSize() == 0)
                JOptionPane.showMessageDialog(this,
                        "Need at least one range entry to be edited.",
                        "Empty Phase List",
                        JOptionPane.ERROR_MESSAGE);
            else if(rangeList.isSelectionEmpty())
                JOptionPane.showMessageDialog(this, "Select a Range Entry from the list to be edited.");
            else if(startTime >= endTime)
                JOptionPane.showMessageDialog(this, "Start Time must be less than End Time.");
            else {
                history.update(rangeList.getSelectedIndex(), startTime, endTime, nameField.getText(), processorField.getText());
                int selection = rangeList.getSelectedIndex();
                listModel.removeElementAt(selection);
                listModel.add(selection, getRangeString(startTime, endTime, nameField.getText(), processorField.getText()));
            }
        } else if(ae.getSource() == saveButton) {
            try {
                history.save();
            } catch (IOException e) {
                System.out.println("Error saving history to disk: " + e.toString());
            }
        }
    }

    private String getRangeString(long startTime, long endTime, String name, String procs) {
        StringBuilder currString = new StringBuilder();
        currString
                .append(U.humanReadableString(startTime))
                .append(" to ")
                .append(U.humanReadableString(endTime));
        if(procs.length() > 10)
            currString
                    .append(" Proc(s): ")
                    .append(procs.substring(0, 10))
                    .append("...");
        else
            currString.append(" Proc(s): ").append(procs);
        if(name.length() > 10)
            currString
                    .append(" (")
                    .append(name.substring(0, 10))
                    .append("...)");
        else
            currString.append(" (").append(name).append(")");

        return currString.toString();
    }

    protected void showDialog() {

    }
}
