// https://developer.android.com/topic/performance/benchmarking/microbenchmark-overview
// From sample: https://github.com/android/performance-samples/tree/main/MicrobenchmarkSample
package org.sebastiaan.parquet_android;

import androidx.benchmark.BenchmarkState;
import androidx.benchmark.junit4.BenchmarkRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.sebastiaan.testutils.Row;
import org.sebastiaan.testutils.RowDehydrator;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import blue.strategic.parquet.CompressionCodecName;
import blue.strategic.parquet.Dehydrator;
import blue.strategic.parquet.ParquetWriter;

/**
 * Example local unit test, which will execute on the development machine (host).
 */
@RunWith(AndroidJUnit4.class)
public class ParquetBenchmark {
    static final int numRows = 1000;
    static List<Row> data; // Do not make this field local or final, as the JVM may constant-fold it then.

    static {
        List<Row> rows = new ArrayList<>(numRows);
        for (int i = 0; i < numRows; ++i) {
            rows.add(new Row(i, "KingHenryThe"+i, 18+(i%10)));
        }
        data = rows;
    }

    @Rule
    public BenchmarkRule benchmarkRule = new BenchmarkRule();

    @Rule
    public TemporaryFolder testFolder = new TemporaryFolder();

    @Test
    public void parquetWrite() throws IOException {
        BenchmarkState state = benchmarkRule.getState();

        File file = testFolder.newFile();
        while (state.keepRunning()) {
            try(ParquetWriter<Row> parquetWriter = ParquetWriter.writeFile(Row.schema, file, new RowDehydrator(), CompressionCodecName.UNCOMPRESSED)) {
                // Here we write all Row data to parquet
                for (Row datum : data) {
                    parquetWriter.write(datum);
                }
            }
        }
    }

//    @Benchmark
//    @Warmup(iterations = 5, time = 5)
//    @Measurement(iterations = 5, time = 5)
//    @BenchmarkMode(Mode.AverageTime)
//    public void parquetReadAverage(Blackhole bh) throws IOException {
//        final Path tempFile = Files.createFile(tempDir.resolve("test.parquet"));
//        Dehydrator<Row> dehydrator = getRowDehydrator();
//
//        // write
//        try(ParquetWriter<Row> parquetWriter = ParquetWriter.writeFile(Row.schema, tempFile.toFile(), dehydrator, CompressionCodecName.UNCOMPRESSED)) {
//            // Here we write all Row data to parquet
//            for (Row datum : data) {
//                parquetWriter.write(datum);
//            }
//        }
//
//        AssertHelper.assertWritten(data, tempFile, HydratorSupplier.constantly(getRowHydrator()));
//
//        // read
//        try(Stream<Row> readStream = ParquetReader.streamContent(tempFile.toFile(), HydratorSupplier.constantly(getRowHydrator()))) {
//            List<Row> readData = readStream.collect(Collectors.toList());
//            Assertions.assertEquals(data, readData);
//        }
//    }
}