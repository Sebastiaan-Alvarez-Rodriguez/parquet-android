package org.sebastiaan.testutils;

import java.util.ArrayList;
import java.util.List;

import blue.strategic.parquet.Hydrator;

public class RowHydrator implements Hydrator<List<Object>, Row> {

    /**
     * Before reading an element, this function is called to create a storage container.
     * This container is provided as first argument at {@link #add(List, String, Object)}
     * and {@link #finish(List)} methods.
     */
    @Override
    public List<Object> start() {
        return new ArrayList<>(Row.names.length);
    }

    /**
     * Called when reading the value from 1 column for this particular data item.
     * @param target Target storage created at {@link #start()}
     * @param heading Column heading (name) from which we read a value belonging to this data item.
     * @param value Value belonging to this data item.
     * @return Updated target storage.
     */
    @Override
    public List<Object> add(List<Object> target, String heading, Object value) {
        target.add(value);
        return target;
    }

    /**
     * Finalizes the data item. Here we convert our storage to a Row class.
     * @param target Target container filled with data at {@link #add(List, String, Object)}
     * @return finalized Row item.
     */
    @Override
    public Row finish(List<Object> target) {
        return Row.fromValues(target);
    }
}
