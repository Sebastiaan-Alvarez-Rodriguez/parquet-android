package org.sebastiaan.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;

import sun.misc.Unsafe;

public class UnsafeProvider {
    public static final Unsafe UNSAFE;

    private static Method CLEAN_METHOD = null;
    private static Method PRECLEAN_METHOD = null;
    private static Field PRECLEAN_FIELD = null;

    /**
     * We have either:
     *
     *   0. Unsafe.invokeCleaner(bytebuffer) (java 9 and up)
     *   1. DirectByteBuffer.cleaner().clean()
     *   2. DirectByteBuffer.free()
     *   3. DirectByteBuffer.memoryRef.free()
     */
    private static int cleanBufferMethod = -1;
    private static final int failed = -1;
    private static final int invokeCleaner = 0;
    private static final int byteBufferCleanerClean = 1;
    private static final int byteBufferFree = 2;
    private static final int byteBufferMemoryRefFree = 3;
    private static final int majorVersion = Integer.parseInt(System.getProperty("java.version").split("\\D+")[0]);

    private UnsafeProvider(){}

    static {
        Logger logger = LoggerFactory.getLogger("org.sebastiaan.parquet.android");

        Unsafe localUnsafeSoStupidJavaUnderstands;
        boolean gotUnsafe = false;
        try {
            Field theUnsafe = Unsafe.class.getDeclaredField("theUnsafe");
            theUnsafe.setAccessible(true);
            localUnsafeSoStupidJavaUnderstands = (Unsafe) theUnsafe.get(null);
            gotUnsafe = true;
        } catch (Exception e) {
            localUnsafeSoStupidJavaUnderstands = null;
            logger.warn("parquet-android - UnsafeProvider - static initializer: Could not find Unsafe access. Leaks might occur.", e);
        }

        UNSAFE = localUnsafeSoStupidJavaUnderstands;
        if (!gotUnsafe || (!getUnsafeInvokeCleaner() && !getDirectByteBufferCleanerClean() && !getDirectByteBufferFree() && !getDirectByteBufferMemoryRefFree())) {
            logger.warn("parquet-android - UnsafeProvider - static initializer: Could not find a suitable bytebuffer cleaner. Leaks might occur.");
            cleanBufferMethod = failed;
        }
    }

    /**
     * Cleans a directly allocated bytebuffers memory.
     * This is desirable when waiting for the GC to deal with cleanup is not performant enough.
     * This function *might* free the buffer, if it could get access to a known direct cleaner function.
     * @param buffer Buffer to free.
     */
    public static void cleanDirectBuffer(ByteBuffer buffer) {
        if (cleanBufferMethod == failed)
            return;
        try {
            switch (cleanBufferMethod) {
                case invokeCleaner: CLEAN_METHOD.invoke(UNSAFE, buffer); return;
                case byteBufferCleanerClean: CLEAN_METHOD.invoke(PRECLEAN_METHOD.invoke(buffer)); return;
                case byteBufferFree: CLEAN_METHOD.invoke(buffer); return;
                case byteBufferMemoryRefFree: CLEAN_METHOD.invoke(PRECLEAN_FIELD.get(buffer)); return;
            }
        } catch (Exception e) {
            throw new RuntimeException("parquet-android - could not call direct buffer clean code (using method "+cleanBufferMethod+")", e);
        }
    }

    private static boolean getUnsafeInvokeCleaner() {
        try {
            if (majorVersion >= 9) {
                // We have UNSAFE.invokeCleaner(ByteBuffer)!
                CLEAN_METHOD = Unsafe.class.getMethod("invokeCleaner", ByteBuffer.class);
                PRECLEAN_METHOD = null;
                PRECLEAN_FIELD = null;
            }
            cleanBufferMethod = invokeCleaner;
            return CLEAN_METHOD != null;
        } catch (Exception e) {
            return false;
        }
    }

    private static boolean getDirectByteBufferCleanerClean() {
        final ByteBuffer tmpBuffer = ByteBuffer.allocateDirect(0);
        try {
            Method localCleaner = tmpBuffer.getClass().getDeclaredMethod("cleaner");
            localCleaner.setAccessible(true);
            final Object cleanerObject = localCleaner.invoke(tmpBuffer);
            if (cleanerObject != null) { // DirectByteBuffer.cleaner().clean()
                CLEAN_METHOD = cleanerObject.getClass().getMethod("clean");
                PRECLEAN_METHOD = localCleaner;
                CLEAN_METHOD.invoke(cleanerObject);
            }
            cleanBufferMethod = byteBufferCleanerClean;
            return PRECLEAN_METHOD != null && CLEAN_METHOD != null;
        } catch (Exception e) {
            return false;
        }
    }

    private static boolean getDirectByteBufferFree() {
        final ByteBuffer tmpBuffer = ByteBuffer.allocateDirect(0);
        try {
            CLEAN_METHOD = tmpBuffer.getClass().getDeclaredMethod("free");
            CLEAN_METHOD.setAccessible(true);
            CLEAN_METHOD.invoke(tmpBuffer);
            cleanBufferMethod = byteBufferFree;
            return CLEAN_METHOD != null;
        } catch (Exception e) {
            return false;
        }
    }

    private static boolean getDirectByteBufferMemoryRefFree() {
        final ByteBuffer tmpBuffer = ByteBuffer.allocateDirect(0);
        try {
            // Android JDK DirectByteBuffer may have a memoryRef field with a free() method
            PRECLEAN_FIELD = tmpBuffer.getClass().getDeclaredField("memoryRef");
            PRECLEAN_FIELD.setAccessible(true);
            CLEAN_METHOD = PRECLEAN_FIELD.getClass().getDeclaredMethod("free");
            PRECLEAN_METHOD = null;
            cleanBufferMethod = byteBufferMemoryRefFree;
            return PRECLEAN_FIELD != null && CLEAN_METHOD != null;
        } catch (Exception e) {
            return false;
        }
    }
}

