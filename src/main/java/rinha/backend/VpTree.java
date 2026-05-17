package rinha.backend;

import java.io.IOException;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public final class VpTree {

  private static final int NODE_SIZE = 16;

  private final MappedByteBuffer buffer;
  private final int size;

  public VpTree(Path path) throws IOException {
    try (FileChannel channel = FileChannel.open(path, StandardOpenOption.READ)) {
      long fileSize = channel.size();

      this.size = (int) (fileSize / NODE_SIZE);
      this.buffer = channel.map(
        FileChannel.MapMode.READ_ONLY,
        0,
        fileSize
      );
      this.buffer.order(ByteOrder.BIG_ENDIAN);
    }
  }

  public int size() {
    return size;
  }

  public int pointIndex(int node) {
    return buffer.getInt(node * NODE_SIZE);
  }

  public float radius(int node) {
    return buffer.getFloat(node * NODE_SIZE + 4);
  }

  public int left(int node) {
    return buffer.getInt(node * NODE_SIZE + 8);
  }

  public int right(int node) {
    return buffer.getInt(node * NODE_SIZE + 12);
  }
}
