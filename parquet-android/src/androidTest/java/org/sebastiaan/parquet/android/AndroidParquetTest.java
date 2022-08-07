package org.sebastiaan.parquet.android;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.sebastiaan.testutils.AssertHelper;
import org.sebastiaan.testutils.Row;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import blue.strategic.parquet.CompressionCodecName;
import blue.strategic.parquet.Dehydrator;
import blue.strategic.parquet.Hydrator;
import blue.strategic.parquet.HydratorSupplier;
import blue.strategic.parquet.ParquetReader;
import blue.strategic.parquet.ParquetWriter;

/**
 * Example local unit test, which will execute on the development machine (host).
 */
class AndroidParquetTest {
    static final int numRows = 1000;
    static final List<Row> data;

    static {
        List<Row> rows = new ArrayList<>(numRows);
        for (int i = 0; i < numRows; ++i) {
            rows.add(new Row(i, "KingHenryThe" + i, 18 + (i % 10)));
        }
        data = rows;
    }

    @TempDir
    Path tempDir;

    /** Currently supported codecs (with the required dependency behind them) **/
    public static final CompressionCodecName[] IMPLEMENTED_COMPRESSION_CODECS = {
            CompressionCodecName.UNCOMPRESSED, // no dependency needed
            CompressionCodecName.SNAPPY        // implementation "com.github.Sebastiaan-Alvarez-Rodriguez:snappy-android:1.1.9"
    };

    @ParameterizedTest
    @EnumSource(CompressionCodecName.class)
    void writeParquet(CompressionCodecName compressionCodecName) throws IOException {
        if (Arrays.stream(IMPLEMENTED_COMPRESSION_CODECS).noneMatch(codec -> codec == compressionCodecName))
            return; // We do not support this codec yet
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

    @ParameterizedTest
    @EnumSource(CompressionCodecName.class)
    void readParquet(CompressionCodecName compressionCodecName) throws IOException {
        if (Arrays.stream(IMPLEMENTED_COMPRESSION_CODECS).noneMatch(codec -> codec == compressionCodecName))
            return; // We do not support this codec yet
        final Path tempFile = Files.createFile(tempDir.resolve("test.parquet"));
        Dehydrator<Row> dehydrator = getRowDehydrator();

        // write
        try(ParquetWriter<Row> parquetWriter = ParquetWriter.writeFile(Row.schema, tempFile.toFile(), dehydrator, compressionCodecName)) {
            // Here we write all Row data to parquet
            for (Row datum : data) {
                parquetWriter.write(datum);
            }
        }

        AssertHelper.assertWritten(data, tempFile, HydratorSupplier.constantly(getRowHydrator()));

        // read
        try(Stream<Row> readStream = ParquetReader.streamContent(tempFile.toFile(), HydratorSupplier.constantly(getRowHydrator()))) {
            List<Row> readData = readStream.collect(Collectors.toList());
            Assertions.assertEquals(data, readData);
        }
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