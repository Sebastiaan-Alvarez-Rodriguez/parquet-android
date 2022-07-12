package blue.strategic.parquet;

import org.apache.hadoop.conf.Configuration;
import org.apache.parquet.column.ParquetProperties;
import org.apache.parquet.hadoop.api.WriteSupport;
import org.apache.parquet.hadoop.metadata.CompressionCodecName;
import org.apache.parquet.io.DelegatingPositionOutputStream;
import org.apache.parquet.io.OutputFile;
import org.apache.parquet.io.PositionOutputStream;
import org.apache.parquet.io.api.Binary;
import org.apache.parquet.io.api.RecordConsumer;
import org.apache.parquet.schema.LogicalTypeAnnotation;
import org.apache.parquet.schema.MessageType;
import org.apache.parquet.schema.PrimitiveType;

import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collections;

/**
 * Override of original blue.strategic.parquet.ParquetWriter class: Made `CompressionCodecName` configurable.
 * Override necessary, because the original author made their implementation impossible to extend upon.
 * @param <T>
 */
public class ParquetWriter<T> implements Closeable {

    protected org.apache.parquet.hadoop.ParquetWriter<T> writer;

    public static <T> ParquetWriter<T> writeFile(MessageType schema, File out, Dehydrator<T> dehydrator) throws IOException {
        return writeFile(schema, out, dehydrator, CompressionCodecName.SNAPPY);
    }

    /** Writes file with given compression codec. Note that some compression codecs require library presence on the executing system. */
    public static <T> ParquetWriter<T> writeFile(MessageType schema, File out, Dehydrator<T> dehydrator, CompressionCodecName codec) throws IOException {
        OutputFile f = new OutputFile() {
            @Override
            public PositionOutputStream create(long blockSizeHint) throws IOException {
                return createOrOverwrite(blockSizeHint);
            }

            @Override
            public PositionOutputStream createOrOverwrite(long blockSizeHint) throws IOException {
                FileOutputStream fos = new FileOutputStream(out);
                return new DelegatingPositionOutputStream(fos) {
                    @Override
                    public long getPos() throws IOException {
                        return fos.getChannel().position();
                    }
                };
            }

            @Override
            public boolean supportsBlockSize() {
                return false;
            }

            @Override
            public long defaultBlockSize() {
                return 1024L;
            }
        };
        return writeOutputFile(schema, f, dehydrator, codec);
    }

    protected static <T> ParquetWriter<T> writeOutputFile(MessageType schema, OutputFile file, Dehydrator<T> dehydrator, CompressionCodecName codec) throws IOException {
        return new ParquetWriter<>(file, schema, dehydrator, codec);
    }

    protected ParquetWriter(OutputFile outputFile, MessageType schema, Dehydrator<T> dehydrator, CompressionCodecName codec) throws IOException {
        this.writer = new Builder<T>(outputFile)
                .withType(schema)
                .withDehydrator(dehydrator)
                .withCompressionCodec(codec)
                .withWriterVersion(ParquetProperties.WriterVersion.PARQUET_2_0)
                .build();
    }

    public void write(T record) throws IOException {
        writer.write(record);
    }

    @Override
    public void close() throws IOException {
        this.writer.close();
    }

    protected static class Builder<T> extends org.apache.parquet.hadoop.ParquetWriter.Builder<T, ParquetWriter.Builder<T>> {
        protected MessageType schema;
        protected Dehydrator<T> dehydrator;

        protected Builder(OutputFile file) {
            super(file);
        }

        public ParquetWriter.Builder<T> withType(MessageType schema) {
            this.schema = schema;
            return this;
        }

        public ParquetWriter.Builder<T> withDehydrator(Dehydrator<T> dehydrator) {
            this.dehydrator = dehydrator;
            return this;
        }

        @Override
        protected ParquetWriter.Builder<T> self() {
            return this;
        }

        @Override
        protected WriteSupport<T> getWriteSupport(Configuration conf) {
            return new SimpleWriteSupport<>(schema, dehydrator);
        }
    }

    protected static class SimpleWriteSupport<T> extends WriteSupport<T> {
        protected MessageType schema;
        protected Dehydrator<T> dehydrator;
        protected ValueWriter valueWriter = SimpleWriteSupport.this::writeField;

        protected RecordConsumer recordConsumer;

        SimpleWriteSupport(MessageType schema, Dehydrator<T> dehydrator) {
            this.schema = schema;
            this.dehydrator = dehydrator;
        }

        @Override
        public WriteContext init(Configuration configuration) {
            return new WriteContext(schema, Collections.emptyMap());
        }

        @Override
        public void prepareForWrite(RecordConsumer recordConsumer) {
            this.recordConsumer = recordConsumer;
        }

        @Override
        public void write(T record) {
            recordConsumer.startMessage();
            dehydrator.dehydrate(record, valueWriter);
            recordConsumer.endMessage();
        }

        @Override
        public String getName() {
            return "blue.strategic.parquet.ParquetWriter";
        }

        protected void writeField(String name, Object value) {
            int fieldIndex = schema.getFieldIndex(name);
            PrimitiveType type = schema.getType(fieldIndex).asPrimitiveType();
            recordConsumer.startField(name, fieldIndex);

            switch (type.getPrimitiveTypeName()) {
                case INT32: recordConsumer.addInteger((int)value); break;
                case INT64: recordConsumer.addLong((long)value); break;
                case DOUBLE: recordConsumer.addDouble((double)value); break;
                case BOOLEAN: recordConsumer.addBoolean((boolean)value); break;
                case FLOAT: recordConsumer.addFloat((float)value); break;
                case BINARY:
                    if (type.getLogicalTypeAnnotation() == LogicalTypeAnnotation.stringType()) {
                        recordConsumer.addBinary(Binary.fromString((String)value));
                    } else {
                        throw new UnsupportedOperationException("We don't support writing " + type.getLogicalTypeAnnotation());
                    }
                    break;
                default:
                    throw new UnsupportedOperationException("We don't support writing " + type.getPrimitiveTypeName());
            }
            recordConsumer.endField(name, fieldIndex);
        }
    }
}
