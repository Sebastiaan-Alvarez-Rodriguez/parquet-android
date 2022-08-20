package org.sebastiaan.parquet_android;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.opencsv.bean.CsvBindByPosition;

import java.util.Objects;

public class CSVRow {
    @CsvBindByPosition(position = 0)
    public long id;

    @CsvBindByPosition(position = 1)
    public String name;

    @CsvBindByPosition(position = 2)
    public int age;

    // Required constructor for opencsv
    @SuppressWarnings("unused")
    public CSVRow() {}

    public CSVRow(long id, String name, int age) {
        this.id = id;
        this.name = name;
        this.age = age;
    }

    @Override
    public int hashCode() {
        return Long.hashCode(id); // id's should be unique and thus be perfect for hashing.
    }

    @Override
    public boolean equals(Object obj) {
        // For testing purposes, we do a full comparison between Rows to show all fields match after reading/writing
        return obj instanceof CSVRow &&
                this.id == ((CSVRow) obj).id &&
                Objects.equals(this.name, ((CSVRow) obj).name) &&
                this.age == ((CSVRow) obj).age;
    }

    @NonNull
    @Override
    public String toString() {
        return "\""+this.id+"\",\""+name+"\",\""+age+"\"";
    }
}
