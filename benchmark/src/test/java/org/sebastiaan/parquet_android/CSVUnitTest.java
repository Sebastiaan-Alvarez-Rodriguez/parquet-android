package org.sebastiaan.parquet_android;

import com.opencsv.bean.CsvToBeanBuilder;
import com.opencsv.bean.StatefulBeanToCsv;
import com.opencsv.bean.StatefulBeanToCsvBuilder;
import com.opencsv.exceptions.CsvDataTypeMismatchException;
import com.opencsv.exceptions.CsvRequiredFieldEmptyException;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sebastiaan.testutils.Junit4AssertHelper;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

/** Very short unit test to show our CSV implementation works */
public class CSVUnitTest {
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
    public TemporaryFolder testFolder = new TemporaryFolder();

    @Test
    public void csvWrite() throws IOException, CsvRequiredFieldEmptyException, CsvDataTypeMismatchException {
        File file = testFolder.newFile();
        // write
        try (Writer writer = new FileWriter(file)) {
            StatefulBeanToCsv<CSVRow> beanToCsv = new StatefulBeanToCsvBuilder<CSVRow>(writer).build();
            beanToCsv.write(data);
        }

        Junit4AssertHelper.assertFileEquals(data.stream().map(Object::toString), file);
    }


    @Test
    public void csvRead() throws IOException, CsvRequiredFieldEmptyException, CsvDataTypeMismatchException {
        File file = testFolder.newFile();
        // write
        try (Writer writer = new FileWriter(file)) {
            StatefulBeanToCsv<CSVRow> beanToCsv = new StatefulBeanToCsvBuilder<CSVRow>(writer).build();
            beanToCsv.write(data);
        }

        Junit4AssertHelper.assertFileEquals(data.stream().map(Object::toString), file);

        // read
        List<CSVRow> beans = new CsvToBeanBuilder<CSVRow>(new FileReader(file))
                .withType(CSVRow.class)
                .build()
                .parse();
        Assert.assertEquals(data, beans);
    }
}