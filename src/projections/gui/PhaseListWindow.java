package projections.gui;

import projections.analysis.PhaseHistory;

import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.JTextArea;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.ListSelectionModel;
import javax.swing.JOptionPane;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.io.File;
import java.io.IOException;

public class PhaseListWindow
        extends ProjectionsWindow
        implements ListSelectionListener, ActionListener {

    private static int myRun = 0;
    private MainWindow mainWindow;

    private PhaseHistory history;
    private DefaultListModel listModel;

    private JList phaseHistoryList;
    private JTextArea phaseList;
    private JButton addButton, removeButton, editButton, saveButton;

    PhaseListWindow(MainWindow mainWindow) {
        super("Projections Phase List Window - " + MainWindow.runObject[myRun].getFilename() + ".sts", mainWindow);
        this.mainWindow = mainWindow;
        history = new PhaseHistory(MainWindow.runObject[myRun].getLogDirectory() + File.separator);

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
        phaseHistoryList = new JList(listModel);
        phaseHistoryList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        phaseHistoryList.addListSelectionListener(this);
        phaseHistoryList.setBorder(new TitledBorder(new LineBorder(Color.black), "Phase Config List"));

        phaseList = new JTextArea(5, 20);
        phaseList.setBorder(new TitledBorder(new LineBorder(Color.black), "Phase List"));
        phaseList.setEditable(false);

        removeButton = new JButton("Remove selected Phase Config");
        editButton = new JButton("Edit selected Phase Config");
        addButton = new JButton("Add new Phase Config");
        saveButton = new JButton("Save changes to disk");
        removeButton.addActionListener(this);
        editButton.addActionListener(this);
        addButton.addActionListener(this);
        saveButton.addActionListener(this);

        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(gbl);
        Util.gblAdd(buttonPanel, removeButton,
                gbc, 0, 0, 1, 1, 1, 1);
        Util.gblAdd(buttonPanel, editButton,
                gbc, 1, 0, 1, 1, 1, 1);
        Util.gblAdd(buttonPanel, addButton,
                gbc, 2, 0, 1, 1, 1, 1);
        Util.gblAdd(buttonPanel, saveButton,
                gbc, 3, 0, 1, 1, 1, 1);

        Util.gblAdd(mainPanel, phaseHistoryList,
                gbc, 0, 0, 1, 1, 1, 1);
        Util.gblAdd(mainPanel, phaseList,
                gbc, 1, 0, 1, 1, 1, 1);
        Util.gblAdd(mainPanel, buttonPanel,
                gbc, 0, 1, 2, 1, 0, 0);

        getContentPane().add(mainPanel);
    }

    @Override
    public void valueChanged(ListSelectionEvent listSelectionEvent) {
        int selection = phaseHistoryList.getSelectedIndex();
        if(selection == -1)
            return;
        StringBuilder areaText = new StringBuilder();
        for(int i = 0; i < history.getNumPhases(selection); i++)
            areaText
                    .append(history.getPhaseString(selection, i))
                    .append("\n");
        phaseList.setText(areaText.toString());
        pack();
    }

    @Override
    public void actionPerformed(ActionEvent ae) {
        if(ae.getSource() == addButton) {
            if(MainWindow.runObject[myRun].hasLogData() || MainWindow.runObject[myRun].hasSumDetailData()) {
                super.close();
                new PhaseWindow(mainWindow, -1);
            } else
                JOptionPane.showMessageDialog(this,
                        "Need open files open to add new phase.",
                        "No open files",
                        JOptionPane.ERROR_MESSAGE);
        } else if(ae.getSource() == removeButton) {
            if(phaseHistoryList.getModel().getSize() == 0) {
                JOptionPane.showMessageDialog(this,
                        "Need at least one phase config to be removed.",
                        "Empty Phase Config List",
                        JOptionPane.ERROR_MESSAGE);
            }
            else if(phaseHistoryList.isSelectionEmpty())
                JOptionPane.showMessageDialog(this, "Select a Phase Config from the list to be removed.");
            else {
                history.remove(phaseHistoryList.getSelectedIndex());
                listModel.removeElementAt(phaseHistoryList.getSelectedIndex());
            }
        } else if(ae.getSource() == editButton) {
            if(phaseHistoryList.getModel().getSize() == 0) {
                JOptionPane.showMessageDialog(this,
                        "Need at least one phase config to be edited.",
                        "Empty Phase List",
                        JOptionPane.ERROR_MESSAGE);
            } else if(phaseHistoryList.isSelectionEmpty())
                JOptionPane.showMessageDialog(this, "Select a Phase Config from the list to be edited.");
            else {
                super.close();
                new PhaseWindow(mainWindow, phaseHistoryList.getSelectedIndex());
            }
        } else if(ae.getSource() == saveButton) {
            try {
                history.save();
            } catch (IOException e) {
                System.out.println("Error saving history to disk: " + e.toString());
            }
        }
    }

    protected void showDialog() {

    }
}
