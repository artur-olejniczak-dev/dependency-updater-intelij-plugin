import sys

with open('src/main/java/com/example/dependencyupdater/TransitiveDependencyPanel.java', 'r', encoding='utf-8') as f:
    content = f.read()

# Replace column names
content = content.replace(
    '''String[] columnNames = {"Library", "Current Version", "Available Versions", "Current Version Vulnerabilities"};''',
    '''String[] columnNames = {"Update", "Library", "Current Version", "Available Versions", "Current Version Vulnerabilities"};'''
)

# Replace getColumnClass
old_get_column_class = '''            public Class<?> getColumnClass(int columnIndex) {

                if (columnIndex == 2) return DropdownValue.class;

                return String.class;

            }'''

new_get_column_class = '''            public Class<?> getColumnClass(int columnIndex) {
                if (columnIndex == 0) return Boolean.class;
                if (columnIndex == 3) return DropdownValue.class;
                return String.class;
            }'''
content = content.replace(old_get_column_class, new_get_column_class)

# Replace isCellEditable
old_is_cell_editable = '''            public boolean isCellEditable(int row, int column) {

                if (column == 2) return true; 

                if (column == 0 || column == 1 || column == 3) return true; 

                return false;

            }'''

new_is_cell_editable = '''            public boolean isCellEditable(int row, int column) {
                if (column == 0 || column == 3) return true;
                if (column == 1 || column == 2 || column == 4) return true;
                return false;
            }'''
content = content.replace(old_is_cell_editable, new_is_cell_editable)


# Replace table setup
old_table_setup = '''        table.getTableHeader().setFont(table.getTableHeader().getFont().deriveFont(Font.BOLD));

        table.getColumnModel().getColumn(2).setCellEditor(new ComboBoxCellEditor());

        table.getColumnModel().getColumn(2).setCellRenderer(new DefaultTableCellRenderer() {

            @Override

            protected void setValue(Object value) {

                if (value instanceof DropdownValue) {

                    setText(((DropdownValue) value).selected);

                } else {

                    super.setValue(value);

                }

            }

        });

        JTextField readOnlyField = new JTextField();

        readOnlyField.setEditable(false);

        readOnlyField.setBorder(null);

        DefaultCellEditor readOnlyEditor = new DefaultCellEditor(readOnlyField);

        table.getColumnModel().getColumn(0).setCellEditor(readOnlyEditor);

        table.getColumnModel().getColumn(1).setCellEditor(readOnlyEditor);
        table.getColumnModel().getColumn(3).setCellEditor(readOnlyEditor);'''

new_table_setup = '''        table.getTableHeader().setFont(table.getTableHeader().getFont().deriveFont(Font.BOLD));

        // CheckBox renderer for Update column
        table.getColumnModel().getColumn(0).setMaxWidth(110);
        table.getColumnModel().getColumn(0).setMinWidth(90);
        table.getColumnModel().getColumn(0).setPreferredWidth(100);
        table.getColumnModel().getColumn(0).setCellRenderer(new DefaultTableCellRenderer() {
            private final JCheckBox checkBox = new JCheckBox();
            private final JLabel emptyLabel = new JLabel("");
            {
                checkBox.setHorizontalAlignment(SwingConstants.CENTER);
                checkBox.setOpaque(true);
                emptyLabel.setOpaque(true);
            }
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                if (value == null) return emptyLabel;
                checkBox.setSelected((Boolean) value);
                checkBox.setBackground(isSelected ? table.getSelectionBackground() : table.getBackground());
                return checkBox;
            }
        });

        table.getColumnModel().getColumn(3).setCellEditor(new ComboBoxCellEditor());
        table.getColumnModel().getColumn(3).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            protected void setValue(Object value) {
                if (value instanceof DropdownValue) {
                    setText(((DropdownValue) value).selected);
                } else {
                    super.setValue(value);
                }
            }
        });

        JTextField readOnlyField = new JTextField();
        readOnlyField.setEditable(false);
        readOnlyField.setBorder(null);
        DefaultCellEditor readOnlyEditor = new DefaultCellEditor(readOnlyField);

        table.getColumnModel().getColumn(1).setCellEditor(readOnlyEditor);
        table.getColumnModel().getColumn(2).setCellEditor(readOnlyEditor);
        table.getColumnModel().getColumn(4).setCellEditor(readOnlyEditor);'''

content = content.replace(old_table_setup, new_table_setup)

with open('src/main/java/com/example/dependencyupdater/TransitiveDependencyPanel.java', 'w', encoding='utf-8') as f:
    f.write(content)
