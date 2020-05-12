
# Protobuf memory optimizations

This is a simple experiment to show how reusing objects can hugely decrease the number memory allocations needed to handle protobuf messages.

## Results

Allocation metrics show that the optimized method reduces memory allocation during serialization by a whopping 97%:

    Serialization (original): 143 MB
    Serialization (optimized): 4 MB

Deserialization points to an economy of 10%:

    Deserialization (original): 1381 MB
    Deserialization (optimized): 1245 MB

The rationale for the deserialization benchmark being not so good is that we can't avoid the resulting final objects created by protobuf, since protobuf does not allow reusing existing objects.

This is sad, because those objects could be easily reused if the code allowed doing so. Some interesting references about this:

- [Can I re-use object instances to avoid allocations with protobuf-java?](https://github.com/protocolbuffers/protobuf/issues/5825)
- [Improving performance of protocol buffers](https://stackoverflow.com/a/21870564/778272)
- [Deserialize Google Protobuf wire message on existing object](https://stackoverflow.com/q/55405495/778272)

## How to run

Just use gradle to build the project (I recommend using an IDE like IntelliJ).
