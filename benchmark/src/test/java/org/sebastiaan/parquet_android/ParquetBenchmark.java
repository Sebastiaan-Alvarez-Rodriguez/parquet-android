// https://developer.android.com/topic/performance/benchmarking/microbenchmark-overview
// From sample: https://github.com/android/performance-samples/tree/main/MicrobenchmarkSample
package org.sebastiaan.parquet_android;

import androidx.benchmark.BenchmarkState;
import androidx.benchmark.junit4.BenchmarkRule;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.sebastiaan.testutils.Row;
import org.sebastiaan.testutils.RowDehydrator;
import org.sebastiaan.testutils.RowHydrator;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import blue.strategic.parquet.CompressionCodecName;
import blue.strategic.parquet.HydratorSupplier;
import blue.strategic.parquet.ParquetReader;
import blue.strategic.parquet.ParquetWriter;

/**
 * Example local unit test, which will execute on the development machine (host).
 */
@RunWith(Parameterized.class)
public class ParquetBenchmark {
    @Parameterized.Parameter
    public int numRows;

    @Parameterized.Parameters
    public static Object[] data() {
        return new Object[] {1000, 5000, 10000, 15000, 20000};
    }

    public static List<Row> generate(int numRows) {
        List<Row> rows = new ArrayList<>(numRows);
        for (int i = 0; i < numRows; ++i) {
            rows.add(new Row(i, "KingHenryThe"+i, 18+(i%10)));
        }
        return rows;
    }

    @Rule
    public BenchmarkRule benchmarkRule = new BenchmarkRule();

    @Rule
    public TemporaryFolder testFolder = new TemporaryFolder();

    @Test
    public void parquetWriteUncompressed() throws IOException {
        BenchmarkState state = benchmarkRule.getState();
        List<Row> data = generate(numRows);

        File file = testFolder.newFile();
        while (state.keepRunning()) {
            // write
            try(ParquetWriter<Row> parquetWriter = ParquetWriter.writeFile(Row.schema, file, new RowDehydrator(), CompressionCodecName.UNCOMPRESSED)) {
                for (Row datum : data) {
                    parquetWriter.write(datum);
                }
            }
        }
    }

    @Test
    public void parquetWriteSnappy() throws IOException {
        BenchmarkState state = benchmarkRule.getState();
        List<Row> data = generate(numRows);

        File file = testFolder.newFile();
        while (state.keepRunning()) {
            // write
            try(ParquetWriter<Row> parquetWriter = ParquetWriter.writeFile(Row.schema, file, new RowDehydrator(), CompressionCodecName.SNAPPY)) {
                for (Row datum : data) {
                    parquetWriter.write(datum);
                }
            }
        }
    }

    @Test
    public void parquetReadUncompressed() throws IOException {
        BenchmarkState state = benchmarkRule.getState();
        List<Row> data = generate(numRows);

        File file = testFolder.newFile();
        // write once
        try(ParquetWriter<Row> parquetWriter = ParquetWriter.writeFile(Row.schema, file, new RowDehydrator(), CompressionCodecName.UNCOMPRESSED)) {
            for (Row datum : data) {
                parquetWriter.write(datum);
            }
        }

        while (state.keepRunning()) {
            // read
            try(Stream<Row> readStream = ParquetReader.streamContent(file, HydratorSupplier.constantly(new RowHydrator()))) {
                List<Row> readData = readStream.collect(Collectors.toList());
            }
        }
    }

    @Test
    public void parquetReadSnappy() throws IOException {
        BenchmarkState state = benchmarkRule.getState();
        List<Row> data = generate(numRows);

        File file = testFolder.newFile();
        // write once
        try(ParquetWriter<Row> parquetWriter = ParquetWriter.writeFile(Row.schema, file, new RowDehydrator(), CompressionCodecName.SNAPPY)) {
            for (Row datum : data) {
                parquetWriter.write(datum);
            }
        }

        while (state.keepRunning()) {
            // read
            try(Stream<Row> readStream = ParquetReader.streamContent(file, HydratorSupplier.constantly(new RowHydrator()))) {
                List<Row> readData = readStream.collect(Collectors.toList());
            }
        }
    }
}