package rinha.backend;

import java.io.IOException;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public final class VpTree {

  private static final String FILE_PATH = "vptree.bin";

  private static final int POINT_OFFSET = 0;
  private static final int RADIUS_OFFSET = 4;
  private static final int LEFT_OFFSET = 8;
  private static final int RIGHT_OFFSET = 12;

  private final ThreadLocal<MappedByteBuffer> localBuffer;

  public VpTree() {
    try (
      FileChannel channel = FileChannel.open(
        Path.of(FILE_PATH),
        StandardOpenOption.READ
      )
    ) {
      final MappedByteBuffer masterBuffer = channel.map(
        FileChannel.MapMode.READ_ONLY,
        0,
        channel.size()
      );

      masterBuffer.order(ByteOrder.BIG_ENDIAN);

      localBuffer = ThreadLocal.withInitial(() -> {
        final MappedByteBuffer duplicate = masterBuffer.duplicate();
        duplicate.order(ByteOrder.BIG_ENDIAN);
        return duplicate;
      });

    } catch (IOException e) {
      System.exit(-2);
      throw new RuntimeException(e);
    }
  }

  public int pointIndex(int node) {
    final int offset = node << 4;
    return localBuffer.get().getInt(offset + POINT_OFFSET);
  }

  public float radius(int node) {
    final int offset = node << 4;
    return localBuffer.get().getFloat(offset + RADIUS_OFFSET);
  }

  public int left(int node) {
    final int offset = node << 4;
    return localBuffer.get().getInt(offset + LEFT_OFFSET);
  }

  public int right(int node) {
    final int offset = node << 4;
    return localBuffer.get().getInt(offset + RIGHT_OFFSET);
  }
}
