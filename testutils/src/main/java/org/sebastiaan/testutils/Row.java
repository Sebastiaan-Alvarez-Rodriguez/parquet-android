package org.sebastiaan.testutils;

import org.apache.parquet.schema.LogicalTypeAnnotation;
import org.apache.parquet.schema.MessageType;
import org.apache.parquet.schema.PrimitiveType;
import org.apache.parquet.schema.Type;
import org.apache.parquet.schema.Types;

import java.util.List;
import java.util.Objects;

public class Row {
    public long id;
    public String name;
    public int age;

    public Row(long id, String name, int age) {
        this.id = id;
        this.name = name;
        this.age = age;
    }

    /** Helper function to create a row */
    public static Row fromValues(List<Object> values) {
        return new Row(
                (long) values.get(0), // id
                (String) values.get(1), // name
                (int) values.get(2) // age
        );
    }

    public static String[] names = new String[] {"id", "name", "age"};
    public static Type[] types = new Type[] {
            Types.required(PrimitiveType.PrimitiveTypeName.INT64).named(names[0]),
            Types.required(PrimitiveType.PrimitiveTypeName.BINARY).as(LogicalTypeAnnotation.stringType()).named(names[1]),
            Types.required(PrimitiveType.PrimitiveTypeName.INT32).named(names[2])
    };
    public Object[] values() { return new Object[] {id, name, age}; }

    public static final MessageType schema = new MessageType("testTable", types);

    @Override
    public int hashCode() {
        return Long.hashCode(id); // id's should be unique and thus be perfect for hashing.
    }

    @Override
    public boolean equals(Object obj) {
        // For testing purposes, we do a full comparison between Rows
        // to show all fields match after reading/writing
        return obj instanceof Row &&
                this.id == ((Row) obj).id &&
                Objects.equals(this.name, ((Row) obj).name) &&
                this.age == ((Row) obj).age;
    }
}
