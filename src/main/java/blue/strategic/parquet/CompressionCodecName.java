package blue.strategic.parquet;

/**
 * Compression codec name exposer for Hadoop's CompressionCodecName.
 * We expose it rather than adding a dependency on hadoop-common, because that library is titanic.
 * Depending on it would be a waste of space.
 * @see org.apache.parquet.hadoop.metadata.CompressionCodecName
 */
public enum CompressionCodecName {
    UNCOMPRESSED,
    SNAPPY,
    GZIP,
    LZO,
    BROTLI,
    LZ4,
    ZSTD
}
