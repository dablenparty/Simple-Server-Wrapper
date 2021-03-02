package com.hunterltd.ServerWrapper.Server.Properties;

import javax.swing.table.DefaultTableModel;

public class PropertiesTableModel extends DefaultTableModel {
    private final ServerProperties properties;

    public PropertiesTableModel(ServerProperties props) {
        properties = props;
        properties.keySet().iterator().forEachRemaining(key -> this.addRow(new Object[]{key, properties.get(key)}));
    }
}
