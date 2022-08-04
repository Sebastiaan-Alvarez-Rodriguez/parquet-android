package org.sebastiaan.parquet.android;

import blue.strategic.parquet.CompressionCodecName;
import blue.strategic.parquet.Dehydrator;
import blue.strategic.parquet.Hydrator;
import blue.strategic.parquet.HydratorSupplier;
import blue.strategic.parquet.ParquetWriter;
import org.apache.parquet.schema.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Example local unit test, which will execute on the development machine (host).
 */
class ParquetTest {
    static final int numRows = 1000;
    static final List<Row> data;

    static {
        List<Row> rows = new ArrayList<>(numRows);
        for (int i = 0; i < numRows; ++i) {
            rows.add(new Row(i, "KingHenryThe"+i, 18+(i%10)));
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

    @TempDir
    Path tempDir;

    public static final String[] COMPRESSION_CODECS = {
            CompressionCodecName.UNCOMPRESSED.name(),
            CompressionCodecName.SNAPPY.name()
    };

    @ParameterizedTest
//    @ValueSource(strings = COMPRESSION_CODECS)
    @EnumSource(CompressionCodecName.class)
    void writeParquet(CompressionCodecName compressionCodecName) throws IOException {
        final Path tempFile = Files.createFile(tempDir.resolve("test.parquet"));
        Dehydrator<Row> dehydrator = getRowDehydrator();

        int writtenRows = 0;

        try(ParquetWriter<Row> parquetWriter = ParquetWriter.writeFile(Row.schema, tempFile.toFile(), dehydrator, compressionCodecName)) {
            // Here we write all Row data to parquet
            for (Row datum : data) {
                parquetWriter.write(datum);
                writtenRows += 1;
            }
        }

        Assertions.assertEquals(numRows, writtenRows);
        AssertHelper.assertWritten(data, tempFile, HydratorSupplier.constantly(getRowHydrator()));
    }

    /** @return Dehydrator, which tells how to write (store) a single Row record at a time. */
    static Dehydrator<Row> getRowDehydrator() {
        return (record, valueWriter) -> {
            final Object[] values = record.values();
            for (int i = 0; i < Row.names.length; ++i) {
                valueWriter.write(Row.names[i], values[i]);
            }
        };
    }

    /** @return Hydrator, which tells how to read a single Row record at a time. */
    static Hydrator<List<Object>, Row> getRowHydrator() {
        return new Hydrator<>() {
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
        };
    }
}