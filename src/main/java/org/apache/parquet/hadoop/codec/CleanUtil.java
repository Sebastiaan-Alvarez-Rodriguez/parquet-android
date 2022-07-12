package org.apache.parquet.hadoop.codec;

import org.sebastiaan.parquet.android.UnsafeProvider;

import java.nio.ByteBuffer;

/**
 * Override of Apache's CleanUtil, which does not work on Android.
 */
public class CleanUtil {

    public static void cleanDirectBuffer(ByteBuffer buf) {
        UnsafeProvider.cleanDirectBuffer(buf);
    }
}
