// https://developer.android.com/topic/performance/benchmarking/microbenchmark-overview
// From sample: https://github.com/android/performance-samples/tree/main/MicrobenchmarkSample
package org.sebastiaan.parquet_android;

import androidx.benchmark.BenchmarkState;
import androidx.benchmark.junit4.BenchmarkRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.opencsv.bean.CsvToBeanBuilder;
import com.opencsv.bean.StatefulBeanToCsv;
import com.opencsv.bean.StatefulBeanToCsvBuilder;
import com.opencsv.exceptions.CsvDataTypeMismatchException;
import com.opencsv.exceptions.CsvRequiredFieldEmptyException;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.sebastiaan.testutils.Row;
import org.sebastiaan.testutils.RowDehydrator;
import org.sebastiaan.testutils.RowHydrator;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
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
@RunWith(AndroidJUnit4.class)
public class CSVBenchmark {
    static final int numRows = 1000;
    static List<CSVRow> data; // Do not make this field local or final, as the JVM may constant-fold it then.

    static {
        List<CSVRow> rows = new ArrayList<>(numRows);
        for (int i = 0; i < numRows; ++i) {
            rows.add(new CSVRow(i, "KingHenryThe"+i, 18+(i%10)));
        }
        data = rows;
    }

    @Rule
    public BenchmarkRule benchmarkRule = new BenchmarkRule();

    @Rule
    public TemporaryFolder testFolder = new TemporaryFolder();

    @Test
    public void csvWrite() throws IOException, CsvRequiredFieldEmptyException, CsvDataTypeMismatchException {
        BenchmarkState state = benchmarkRule.getState();

        File file = testFolder.newFile();
        while (state.keepRunning()) {
            // write
            try (Writer writer = new FileWriter(file)) {
                StatefulBeanToCsv<CSVRow> beanToCsv = new StatefulBeanToCsvBuilder<CSVRow>(writer).build();
                beanToCsv.write(data);
            }
        }
    }

    @Test
    public void csvRead() throws IOException, CsvRequiredFieldEmptyException, CsvDataTypeMismatchException {
        BenchmarkState state = benchmarkRule.getState();
        File file = testFolder.newFile();
        // write once
        try (Writer writer = new FileWriter(file)) {
            StatefulBeanToCsv<CSVRow> beanToCsv = new StatefulBeanToCsvBuilder<CSVRow>(writer).build();
            beanToCsv.write(data);
        }

        while (state.keepRunning()) {
            // read
            List<CSVRow> beans = new CsvToBeanBuilder<CSVRow>(new FileReader(file))
                    .withType(CSVRow.class)
                    .build()
                    .parse();
        }
    }
}