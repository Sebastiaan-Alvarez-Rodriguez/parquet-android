package org.sebastiaan.testutils;

import org.junit.jupiter.api.Assertions;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.List;
import java.util.stream.BaseStream;
import java.util.stream.Stream;

import blue.strategic.parquet.HydratorSupplier;
import blue.strategic.parquet.ParquetReader;

public class AssertHelper {

    public static <T> void assertWritten(List<T> data, Path file, HydratorSupplier<List<Object>, T> supplier) throws IOException {
        assertWritten(data, file.toFile(), supplier);
    }
    /** Asserts that given data is written to given file **/
    public static <T> void assertWritten(List<T> data, File file, HydratorSupplier<List<Object>, T> supplier) throws IOException {
        Stream<T> readStream = ParquetReader.streamContent(file, supplier);
        assertStreamEquals(data.stream(), readStream);
    }

    /** Asserts that 2 sequential, ordered streams are equivalent. Closes the streams. */
    public static void assertStreamEquals(BaseStream<?, ?> expected, BaseStream<?, ?> actual) {
        try (expected; actual) {
            Iterator<?> e = expected.iterator(), a = actual.iterator();
            while (e.hasNext() && a.hasNext()) {
                Assertions.assertEquals(e.next(), a.next());
            }
            Assertions.assertFalse(e.hasNext());
            Assertions.assertFalse(a.hasNext());
        }
    }
}
