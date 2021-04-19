package com.hunterltd.ssw.server.properties;

import javax.swing.table.AbstractTableModel;

public class PropertiesTableModel extends AbstractTableModel {
    private final ServerProperties properties;
    private final Object[][] data;
    private final String[] columnNames = {"Property", "Value"};

    public PropertiesTableModel(ServerProperties props) {
        properties = props;
        data = new Object[properties.size()][2];
        int idx = 0;
        for (Object key :
                properties.keySet()) {
            data[idx][0] = key;
            data[idx][1] = properties.get(key);
            idx++;
        }
    }

    @Override
    public int getRowCount() {
        return properties.size();
    }

    @Override
    public int getColumnCount() {
        return 2;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        return data[rowIndex][columnIndex];
    }

    @Override
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        data[rowIndex][columnIndex] = aValue;
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return columnIndex == 1; // Only the second ("value") column is editable
    }

    @Override
    public String getColumnName(int column) {
        return columnNames[column];
    }
}
