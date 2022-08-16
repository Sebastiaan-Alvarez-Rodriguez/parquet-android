# Important keep rules to ensure R2 does not rename dynamically loaded classes/members.
-keeppackagenames
-keep class org.apache.parquet.hadoop.metadata.CompressionCodecName { *; }
-keep class org.apache.parquet.hadoop.metadata.CompressionCodecName.** { *; }

-keep class org.apache.parquet.hadoop.codec.SnappyCodec { *; }

-keep class org.apache.parquet.column.values.** { *; }