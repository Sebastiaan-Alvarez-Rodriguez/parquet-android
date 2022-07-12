package org.sebastiaan.parquet.android;

import blue.strategic.parquet.CompressionCodecName;
import blue.strategic.parquet.Dehydrator;
import blue.strategic.parquet.ParquetWriter;
import org.apache.parquet.schema.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Example local unit test, which will execute on the development machine (host).
 */
class ParquetTest {
    static final int numRows = 1000;
    static final List<Row> data;

    static {
        List<Row> rows = new ArrayList<>(numRows);
        for (int i = 0; i < numRows; ++i) {
            rows.add(new Row(i, "ArmanoThe"+i, 18+(i%10)));
        }
        data = rows;
    }

    static class Row {
        public long id;
        public String name;
        public int age;

        public Row(long id, String name, int age) {
            this.id = id;
            this.name = name;
            this.age = age;
        }

        public static String[] names = new String[] {"id", "name", "age"};
        public static Type[] types = new Type[] {
                Types.required(PrimitiveType.PrimitiveTypeName.INT64).named(names[0]),
                Types.required(PrimitiveType.PrimitiveTypeName.BINARY).as(LogicalTypeAnnotation.stringType()).named(names[1]),
                Types.required(PrimitiveType.PrimitiveTypeName.INT32).named(names[2])
        };
        public Object[] values() { return new Object[] {id, name, age}; }

        public static final MessageType schema = new MessageType("testTable", types);
    }

    @TempDir
    Path tempDir;

    @Test
    void writeParquet() throws IOException {
        final Path tempFile = Files.createFile(tempDir.resolve("test.parquet"));
        Dehydrator<Row> dehydrator = (record, valueWriter) -> {
            final Object[] values = record.values();
            for (int i = 0; i < Row.names.length; ++i) {
                valueWriter.write(Row.names[i], values[i]);
            }
        };

        int writtenRows = 0;
        try(ParquetWriter<Row> parquetWriter = ParquetWriter.writeFile(Row.schema, tempFile.toFile(), dehydrator, CompressionCodecName.UNCOMPRESSED)) {
            for (Row datum : data) {
                parquetWriter.write(datum);
                writtenRows += 1;
            }
        }
        Assertions.assertEquals(numRows, writtenRows);
    }
}