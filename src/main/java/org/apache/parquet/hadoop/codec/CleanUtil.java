package org.apache.parquet.hadoop.codec;

import org.sebastiaan.parquet.android.UnsafeProvider;

import java.nio.ByteBuffer;

public class CleanUtil {

    public static void cleanDirectBuffer(ByteBuffer buf) {
        UnsafeProvider.cleanDirectBuffer(buf);
    }
}
