package org.sebastiaan.parquet.android;

import org.xerial.snappy.SnappyError;
import org.xerial.snappy.SnappyErrorCode;
import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.Buffer;
import java.nio.ByteBuffer;

public class UnsafeProvider {
    public static final Unsafe UNSAFE;

    private static final Method CLEAN_METHOD;
    private static final Method PRECLEAN_METHOD;
    private static final Field PRECLEAN_FIELD;

    private static final int majorVersion = Integer.parseInt(System.getProperty("java.version").split("\\D+")[0]);

    private UnsafeProvider(){}

    static {
        try {
            Field theUnsafe = Unsafe.class.getDeclaredField("theUnsafe");
            theUnsafe.setAccessible(true);
            UNSAFE = (Unsafe) theUnsafe.get(null);
        } catch (Exception e) {
            throw new RuntimeException("Unsupported platform: parquet-android could not access sun.misc.Unsafe");
        }

        // Get bytebuffer cleaner method
        try {
            if (majorVersion >= 9) {
                // We have UNSAFE.invokeCleaner(ByteBuffer)!
                CLEAN_METHOD = Unsafe.class.getMethod("invokeCleaner", ByteBuffer.class);
                PRECLEAN_METHOD = null;
                PRECLEAN_FIELD = null;
            } else {
                // We have either:
                //    DirectByteBuffer.cleaner().clean()
                //    DirectByteBuffer.memoryRef.free()
                final ByteBuffer tempBuffer = ByteBuffer.allocateDirect(0);

                Method localCleaner = tempBuffer.getClass().getDeclaredMethod("cleaner");
                localCleaner.setAccessible(true);
                final Object cleanerObject = localCleaner.invoke(tempBuffer);
                if (cleanerObject != null) { // DirectByteBuffer.cleaner().clean()
                    CLEAN_METHOD = cleanerObject.getClass().getMethod("clean");
                    PRECLEAN_METHOD = localCleaner;
                    PRECLEAN_FIELD = null;
                } else { // DirectByteBuffer.memoryRef.free()
                    // Android JDK DirectByteBuffer has a memoryRef field with a free() method
                    PRECLEAN_FIELD = tempBuffer.getClass().getDeclaredField("memoryRef");
                    PRECLEAN_FIELD.setAccessible(true);
                    CLEAN_METHOD = PRECLEAN_FIELD.getClass().getDeclaredMethod("free");
                    PRECLEAN_METHOD = null;
                }
                CLEAN_METHOD.invoke(cleanerObject);
            }
        } catch (Exception e) {
            throw new RuntimeException("Unsupported platform: parquet-android could not access any known java.nio.DirectByteBuffer cleaning method");
        }
    }
    public static void cleanDirectBuffer(ByteBuffer buffer) {
        try {
            if (majorVersion >= 9) {
                CLEAN_METHOD.invoke(UNSAFE, buffer);
            } else if (PRECLEAN_METHOD != null) {
                CLEAN_METHOD.invoke(PRECLEAN_METHOD.invoke(buffer));
            } else if (PRECLEAN_FIELD != null) {
                CLEAN_METHOD.invoke(PRECLEAN_FIELD.get(buffer));
            }
        } catch (Exception ignored) {}
    }
}


final class UnsafeUtil
{
    public static final Unsafe UNSAFE;
    private static final Field ADDRESS_ACCESSOR;

    private UnsafeUtil()
    {
    }

    static {

        try {
            Field theUnsafe = Unsafe.class.getDeclaredField("theUnsafe");
            theUnsafe.setAccessible(true);
            UNSAFE = (Unsafe) theUnsafe.get(null);
        }
        catch (Exception e) {
            throw new SnappyError(SnappyErrorCode.UNSUPPORTED_PLATFORM, "pure-java snappy requires access to sun.misc.Unsafe");
        }

        try {
            Field field = Buffer.class.getDeclaredField("address");
            field.setAccessible(true);
            ADDRESS_ACCESSOR = field;
        }
        catch (Exception e) {
            throw new SnappyError(SnappyErrorCode.UNSUPPORTED_PLATFORM, "pure-java snappy requires access to java.nio.Buffer raw address field");
        }
    }

    public static long getAddress(Buffer buffer)
    {
        try {
            return (long) ADDRESS_ACCESSOR.get(buffer);
        }
        catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

}

