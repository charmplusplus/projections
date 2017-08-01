package projections.gui;

import javax.swing.JFrame;
import javax.swing.JTable;
import javax.swing.JScrollPane;
import javax.swing.JPanel;
import javax.swing.JButton;
import javax.swing.table.TableColumn;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ChooseUserEntriesWindow extends JFrame {

    private int myRun = 0;

    private ColorUpdateNotifier gw;
    private EntryMethodVisibility data;
    private List<List> tabledata;
    private Map<Integer, String> entryNames;
    private GenericGraphColorer colorer;
    private String[] eventNames;
    private String option;

    public ChooseUserEntriesWindow(EntryMethodVisibility data, ColorUpdateNotifier gw, GenericGraphColorer colorer, Map<Integer, String> entryNames, String option) {
        this.data = data;
        this.gw = gw;
        this.colorer = colorer;
        this.entryNames = entryNames;
        this.eventNames = null;
        this.option = option;
        createLayout();
    }

    ChooseUserEntriesWindow(EntryMethodVisibility data, ColorUpdateNotifier gw, GenericGraphColorer colorer, Map<Integer, String> entryNames, String option, String[] eventNames) {
        this.data = data;
        this.gw = gw;
        this.colorer = colorer;
        this.entryNames = entryNames;
        this.eventNames = eventNames;
        this.option = option;
        createLayout();
    }

    private void createLayout() {
        setTitle("Choose which User " + option + "s are displayed");
        List<String> columnNames = new ArrayList<String>(4);
        columnNames.add("Visible");
        columnNames.add("User " + option + " Name");
        columnNames.add("ID");
        columnNames.add("Color");

        tabledata = new ArrayList<List>();

        makeTableData();

        final MyTableModel tableModel = new MyTableModel(tabledata, columnNames, data, true);

        JTable table = new JTable(tableModel);
        initColumnSizes(table);

        table.setDefaultRenderer(ClickableColorBox.class, new ColorRenderer());
        table.setDefaultEditor(ClickableColorBox.class, new ColorEditor());

        JScrollPane scroller = new JScrollPane(table);
        scroller.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);

        JPanel p = new JPanel();
        p.setLayout(new BorderLayout());

        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new FlowLayout());
        JButton checkAll = new JButton("Make All Visible");
        checkAll.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                changeVisibility(true, tableModel);
                tableModel.fireTableDataChanged();
                data.displayMustBeRedrawn();
            }
        });
        JButton uncheckAll = new JButton("Hide All");
        uncheckAll.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                changeVisibility(false, tableModel);
                tableModel.fireTableDataChanged();
                data.displayMustBeRedrawn();
            }
        });
        buttonPanel.add(checkAll);
        buttonPanel.add(uncheckAll);
        p.add(buttonPanel, BorderLayout.NORTH);
        p.add(scroller, BorderLayout.CENTER);
        this.setContentPane(p);

        pack();
        setSize(800, 400);
        setVisible(true);
    }

    private void makeTableData() {
        tabledata.clear();
        for(Integer id : entryNames.keySet()) {
            int currIndex = id;
            if(eventNames != null) {
                String currName = entryNames.get(id);
                for (int i = 0; i < eventNames.length; i++) {
                    if (currName.equals(eventNames[i])) {
                        currIndex = i;
                        break;
                    }
                }
            }
            List tableRow = new ArrayList(4);
            tableRow.add(data.entryIsVisibleID(currIndex));
            if(eventNames != null)
                tableRow.add(eventNames[currIndex]);
            else
                tableRow.add(entryNames.get(currIndex));
            tableRow.add(currIndex);
            tableRow.add(new ClickableColorBox(currIndex, (Color)colorer.getColorMap()[currIndex], myRun, gw, colorer));
            tabledata.add(tableRow);
        }
    }

    private void initColumnSizes(JTable table) {
        TableColumn column;

        column = table.getColumnModel().getColumn(0);
        column.setPreferredWidth(70);

        column = table.getColumnModel().getColumn(1);
        column.setPreferredWidth(680);

        column = table.getColumnModel().getColumn(2);
        column.setPreferredWidth(50);
    }

    private void changeVisibility(boolean visible, MyTableModel tableModel) {
        for(List v : tabledata) {
            Integer id = (Integer) v.get(2);
            if (visible)
                data.makeEntryVisibleID(id);
            else
                data.makeEntryInvisibleID(id);
        }
        for (List v : tabledata) {
            v.set(0,visible);
        }
        tableModel.fireTableDataChanged();
        data.displayMustBeRedrawn();
    }
}
