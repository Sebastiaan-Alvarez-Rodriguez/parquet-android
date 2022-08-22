// https://developer.android.com/topic/performance/benchmarking/microbenchmark-overview
// From sample: https://github.com/android/performance-samples/tree/main/MicrobenchmarkSample
package org.sebastiaan.parquet_android;

import androidx.benchmark.BenchmarkState;
import androidx.benchmark.junit4.BenchmarkRule;

import com.opencsv.bean.CsvToBeanBuilder;
import com.opencsv.bean.StatefulBeanToCsv;
import com.opencsv.bean.StatefulBeanToCsvBuilder;
import com.opencsv.exceptions.CsvDataTypeMismatchException;
import com.opencsv.exceptions.CsvRequiredFieldEmptyException;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

/**
 * Example local unit test, which will execute on the development machine (host).
 */
@RunWith(Parameterized.class)
public class CSVBenchmark {
    @Parameterized.Parameter
    public int numRows;

    @Parameterized.Parameters
    public static Object[] data() {
        return new Object[] {1000, 5000, 10000, 15000, 20000};
    }

    public static List<CSVRow> generate(int numRows) {
        List<CSVRow> rows = new ArrayList<>(numRows);
        for (int i = 0; i < numRows; ++i) {
            rows.add(new CSVRow(i, "KingHenryThe"+i, 18+(i%10)));
        }
        return rows;
    }

    @Rule
    public BenchmarkRule benchmarkRule = new BenchmarkRule();

    @Rule
    public TemporaryFolder testFolder = new TemporaryFolder();

    @Test
    public void csvWrite() throws IOException, CsvRequiredFieldEmptyException, CsvDataTypeMismatchException {
        BenchmarkState state = benchmarkRule.getState();
        List<CSVRow> data = generate(numRows);

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
        List<CSVRow> data = generate(numRows);

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