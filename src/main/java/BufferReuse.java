
import com.google.protobuf.ByteString;
import com.google.protobuf.CodedOutputStream;
import com.google.protobuf.InvalidProtocolBufferException;
import com.sun.management.ThreadMXBean;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.management.ManagementFactory;
import java.nio.ByteBuffer;

public class BufferReuse {

    private static final int BUFFER_SIZE = 2048;
    private static final int PAYLOAD_SIZE = 512;  // serializes to 1408 bytes, similar to a full MTU
    private static final int COUNT = 100000;

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
//        byteBuffer = ByteBuffer.wrap(byteArray);
        byteBuffer = ByteBuffer.allocateDirect(BUFFER_SIZE);  // This is already reused
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

    private static void serialize() {
        byteBuffer.clear();
        ByteString byteString = foo.toByteString();  // ToDo avoid this
        byteString.copyTo(byteBuffer);
        byteBuffer.flip();
    }

    private static void serializeReuse() {
//        byteBuffer.clear();
        CodedOutputStream output = CodedOutputStream.newInstance(byteArray);
        try {
            foo.writeTo(output);
//            byteBuffer.position(foo.getSerializedSize());
//            byteBuffer.flip();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void deserialize() {
        ByteString byteString = ByteString.copyFrom(byteBuffer);
        byteBuffer.flip();
        try {
            Protocol.Foo foo = parser.parseFrom(byteString);
            if (foo.getBarCount() != PAYLOAD_SIZE) {
                System.out.println("Output zero!");
            }
        } catch (InvalidProtocolBufferException e) {
            e.printStackTrace();
        }
    }

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
//            deserialize();
        }
        long bytesAfter = mxBean.getThreadAllocatedBytes(threadId);
        long mega = Math.round((bytesAfter - bytesBefore) / (1024d * 1024d));
        System.out.println(String.format("%s: %d MB", message, mega));
    }

    private static void runCountAllocatedBytesMultiple(Runnable runnable, String message, int times) {
        for (int i = 0; i < times; i++) {
            runCountAllocatedBytes(runnable, message);
        }
    }

    public static void main(String ...args) {

//        serialize();
//        deserialize();
        serializeReuse();
        deserializeReuse();

        runCountAllocatedBytesMultiple(BufferReuse::serialize, "Serialization (no reuse)", 2);
        runCountAllocatedBytesMultiple(BufferReuse::deserialize, "Deserialization (no reuse)", 2);
        runCountAllocatedBytesMultiple(BufferReuse::serializeReuse, "Serialization (reuse)", 2);
        runCountAllocatedBytesMultiple(BufferReuse::deserializeReuse, "Deserialization (reuse)", 2);
    }
}
