package org.sebastiaan.testutils;

import blue.strategic.parquet.Dehydrator;
import blue.strategic.parquet.ValueWriter;

public class RowDehydrator implements Dehydrator<Row> {
    /** tells how to write (store) a single Row record at a time. */
    @Override
    public void dehydrate(Row row, ValueWriter valueWriter) {
        final Object[] values = row.values();
        for (int i = 0; i < Row.names.length; ++i) {
            valueWriter.write(Row.names[i], values[i]);
        }
    }
}
