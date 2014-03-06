package org.eobjects.hadoopdatacleaner.datastores;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import org.apache.hadoop.io.Text;
import org.eobjects.analyzer.data.InputColumn;
import org.eobjects.analyzer.data.InputRow;
import org.eobjects.analyzer.data.MockInputRow;

public class CsvParser {

    public CsvParser(Collection<InputColumn<?>> jobColumns) {
        this.jobColumns = jobColumns;
    }

    private Collection<InputColumn<?>> jobColumns;

    private Collection<Boolean> usedColumns;

    private void parseHeaderRow(Text csvLine) {
        if (usedColumns == null) {
            usedColumns = new ArrayList<Boolean>();

            String[] values = csvLine.toString().split(";");

            for (String value : values) {
                Boolean found = false;
                for (Iterator<InputColumn<?>> jobColumnsIterator = jobColumns.iterator(); jobColumnsIterator.hasNext();) {
                    InputColumn<?> jobColumn = (InputColumn<?>) jobColumnsIterator.next();
                    String shortName = jobColumn.getName().substring(jobColumn.getName().lastIndexOf('.') + 1);
                    if (shortName.equals(value)) {
                        found = true;
                        break;
                    }
                }
                usedColumns.add(found);
            }
        }

    }

    public InputRow prepareRow(Text csvLine) {
        if (usedColumns == null)
            parseHeaderRow(csvLine);
        
        String[] values = csvLine.toString().split(";");

        Iterator<InputColumn<?>> jobColumnsIterator = jobColumns.iterator();
        Iterator<Boolean> usedColumnsIterator = usedColumns.iterator();

        MockInputRow row = new MockInputRow();
        for (String value : values) {
            Boolean used = usedColumnsIterator.next();
            if (used) {
                InputColumn<?> inputColumn = jobColumnsIterator.next();
                row.put(inputColumn, value);
            }
        }
        return row;
    }

}
