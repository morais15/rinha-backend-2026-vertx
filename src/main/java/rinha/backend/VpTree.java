package rinha.backend;

import java.io.IOException;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public final class VpTree {

  private static final String FILE_PATH =
    "vptree.bin";

  private static final int NODE_SIZE = 20;

  private static final int POINT_OFFSET = 0;
  private static final int RADIUS_OFFSET = 4;
  private static final int LEFT_OFFSET = 12;
  private static final int RIGHT_OFFSET = 16;

  private final int[] pointIndex;
  private final long[] radius;
  private final int[] left;
  private final int[] right;

  public VpTree() {

    try (
      FileChannel channel =
        FileChannel.open(
          Path.of(FILE_PATH),
          StandardOpenOption.READ
        )
    ) {

      final long fileSize = channel.size();

      final int nodeCount =
        (int) (fileSize / NODE_SIZE);

      final MappedByteBuffer buffer =
        channel.map(
          FileChannel.MapMode.READ_ONLY,
          0,
          fileSize
        );

      buffer.order(ByteOrder.BIG_ENDIAN);

      pointIndex = new int[nodeCount];
      radius = new long[nodeCount];
      left = new int[nodeCount];
      right = new int[nodeCount];

      for (int node = 0; node < nodeCount; node++) {

        final int offset =
          node * NODE_SIZE;

        pointIndex[node] =
          buffer.getInt(offset + POINT_OFFSET);

        radius[node] =
          buffer.getLong(offset + RADIUS_OFFSET);

        left[node] =
          buffer.getInt(offset + LEFT_OFFSET);

        right[node] =
          buffer.getInt(offset + RIGHT_OFFSET);
      }

    } catch (IOException e) {
      System.exit(-2);
      throw new RuntimeException(e);
    }
  }

  public int pointIndex(int node) {
    return pointIndex[node];
  }

  public long radius(int node) {
    return radius[node];
  }

  public int left(int node) {
    return left[node];
  }

  public int right(int node) {
    return right[node];
  }
}
