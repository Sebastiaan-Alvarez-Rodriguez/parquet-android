package org.sebastiaan.parquet.android;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.util.FileUtils;
import org.openjdk.jmh.util.TempFile;
import org.sebastiaan.testutils.Row;
import org.sebastiaan.testutils.RowDehydrator;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import blue.strategic.parquet.CompressionCodecName;
import blue.strategic.parquet.Dehydrator;
import blue.strategic.parquet.ParquetWriter;

/**
 * Example local unit test, which will execute on the development machine (host).
 */
@State(Scope.Benchmark)
class ParquetBenchmark {
    static final int numRows = 1000000;
    static List<Row> data; // Do not make this field local or final, as the JVM may constant-fold it then.

    static {
        List<Row> rows = new ArrayList<>(numRows);
        for (int i = 0; i < numRows; ++i) {
            rows.add(new Row(i, "KingHenryThe"+i, 18+(i%10)));
        }
        data = rows;
    }

    @Benchmark
    @Warmup(iterations = 5, time = 5)
    @Measurement(iterations = 5, time = 5)
    @BenchmarkMode({Mode.SingleShotTime, Mode.AverageTime, Mode.Throughput})
    public void parquetWriteAverage() throws IOException {
        final TempFile file = FileUtils.weakTempFile("parquetWriteAverage");
        Dehydrator<Row> dehydrator = new RowDehydrator();

        try(ParquetWriter<Row> parquetWriter = ParquetWriter.writeFile(Row.schema, file.file(), dehydrator, CompressionCodecName.UNCOMPRESSED)) {
            // Here we write all Row data to parquet
            for (Row datum : data) {
                parquetWriter.write(datum);
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