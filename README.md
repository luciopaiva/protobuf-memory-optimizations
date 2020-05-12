
# Protobuf buffer memory optimizations

This is a simple experiment to show how reusing objects can hugely improve memory allocations needed to serialize protobuf messages.

## How to run

Just use gradle to build the project (I recommend using an IDE like IntelliJ).

*The instructions below are no longer needed*

`protoc` is needed to compile the `.proto` file. Get it from the releases page here: https://github.com/protocolbuffers/protobuf/releases/tag/v3.11.4 (look for `protoc-X.YY.Z-PLATFORM.zip` in the assets list).

Once `protoc` is installed and added to `PATH`, simply run `proto.sh` to generate sources.
