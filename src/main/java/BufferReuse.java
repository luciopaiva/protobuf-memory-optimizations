import com.google.protobuf.ByteString;
import com.google.protobuf.CodedOutputStream;
import com.google.protobuf.InvalidProtocolBufferException;
import com.sun.management.ThreadMXBean;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.nio.ByteBuffer;

public class BufferReuse {

    private static final int BUFFER_SIZE = 2048;
    private static final int PAYLOAD_SIZE = 512;  // serializes to 1408 bytes, similar to a full MTU
    private static final int COUNT = 100000;
    private static final int RUNS = 4;

    private static final Protocol.Foo foo;
    private static final int serializedSize;
    private static final byte[] byteArray;
    private static final ByteBuffer byteBuffer;
    private static final ThreadMXBean mxBean;
    private static final long threadId;
    private static final com.google.protobuf.Parser<Protocol.Foo> parser;

    static {
        foo = buildSampleMessage();
        serializedSize = foo.getSerializedSize();
        byteArray = new byte[BUFFER_SIZE];
        byteBuffer = ByteBuffer.allocateDirect(BUFFER_SIZE);
        mxBean = (com.sun.management.ThreadMXBean) ManagementFactory.getThreadMXBean();
        threadId = Thread.currentThread().getId();
        parser = Protocol.Foo.getDefaultInstance().getParserForType();
    }

    private static Protocol.Foo buildSampleMessage() {
        Protocol.Foo.Builder builder = Protocol.Foo.newBuilder();
        for (int i = 0; i < PAYLOAD_SIZE; i++) {
            builder.addBar(i);
        }
        return builder.build();
    }

    /**
     * This is the usual way to serialize messages. Serialize to a ByteString and then copy it to a reusable byte
     * buffer. The problem here is that ByteString allocates its own byte array only to be copied to our own buffer
     * immediately after.
     */
    private static void serialize() {
        byteBuffer.clear();
        ByteString byteString = foo.toByteString();
        byteString.copyTo(byteBuffer);
        byteBuffer.flip();
    }

    /**
     * This is a proposed new way of serializing messages. We hand our own byte array to protobuf so it can serialize
     * the message directly to the final buffer. This avoids both the extra copy needed and the extra memory allocation.
     *
     * Would it be possible to reuse the CodedOutputStream instance?
     */
    private static void serializeReuse() {
        CodedOutputStream output = CodedOutputStream.newInstance(byteArray);
        try {
            foo.writeTo(output);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * This is the usual way of deserializing a message. Copy from our buffer to a ByteString instance and then pass it
     * to protobuf.
     */
    private static void deserialize() {
        ByteString byteString = ByteString.copyFrom(byteBuffer);
        byteBuffer.flip();
        try {
            Protocol.Foo foo = parser.parseFrom(byteString);

            // this is just to avoid the line above being optimized away by the hotspot compiler
            if (foo.getBarCount() != PAYLOAD_SIZE) {
                System.out.println("Output zero!");
            }
        } catch (InvalidProtocolBufferException e) {
            e.printStackTrace();
        }
    }

    /**
     * This is a proposed new way of deserializing messages. We tell the parser to deserialize it directly to out byte
     * array. This avoids both the extra copy needed and the extra memory allocation.
     */
    private static void deserializeReuse() {
        try {
            Protocol.Foo foo = parser.parseFrom(byteArray, 0, serializedSize);
            if (foo.getBarCount() != PAYLOAD_SIZE) {
                System.out.println("Output zero!");
            }
        } catch (InvalidProtocolBufferException e) {
            e.printStackTrace();
        }
    }

    private static void runCountAllocatedBytes(Runnable runnable, String message) {
        long bytesBefore = mxBean.getThreadAllocatedBytes(threadId);
        for (int i = 0; i < COUNT; i++) {
            runnable.run();
        }
        long bytesAfter = mxBean.getThreadAllocatedBytes(threadId);
        long mega = Math.round((bytesAfter - bytesBefore) / (1024d * 1024d));
        System.out.println(String.format("%s: %d MB", message, mega));
    }

    private static void runCountAllocatedBytesMultiple(Runnable runnable, String message) {
        for (int i = 0; i < RUNS; i++) {
            runCountAllocatedBytes(runnable, message);
        }
    }

    public static void main(String ...args) {
        runCountAllocatedBytesMultiple(BufferReuse::serialize, "Serialization (original)");
        runCountAllocatedBytesMultiple(BufferReuse::serializeReuse, "Serialization (optimized)");
        runCountAllocatedBytesMultiple(BufferReuse::deserialize, "Deserialization (original)");
        runCountAllocatedBytesMultiple(BufferReuse::deserializeReuse, "Deserialization (optimized)");
    }
}
