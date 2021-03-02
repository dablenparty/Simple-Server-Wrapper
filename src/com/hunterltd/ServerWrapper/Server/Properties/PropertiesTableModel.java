package com.hunterltd.ServerWrapper.Server.Properties;

import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableModel;

public class PropertiesTableModel extends AbstractTableModel {
    private final ServerProperties properties;
    private final Object[][] data;

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
}
