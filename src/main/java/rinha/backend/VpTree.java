package rinha.backend;

import java.io.IOException;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public final class VpTree {

  private static final int NODE_SIZE = 16;
  private static final String FILE_PATH = "vptree.bin";

  // Buffer mestre, compartilhado apenas para criar duplicates
  private final MappedByteBuffer masterBuffer;

  // Cada thread recebe seu próprio buffer
  private final ThreadLocal<MappedByteBuffer> localBuffer;

  private final int size;

  public VpTree() {
    try (
      FileChannel channel = FileChannel.open(
        Path.of(FILE_PATH),
        StandardOpenOption.READ
      )
    ) {
      long fileSize = channel.size();

      this.size = (int) (fileSize / NODE_SIZE);

      this.masterBuffer = channel.map(
        FileChannel.MapMode.READ_ONLY,
        0,
        fileSize
      );

      this.masterBuffer.order(ByteOrder.BIG_ENDIAN);

      this.localBuffer = ThreadLocal.withInitial(() -> {
        MappedByteBuffer duplicate =
          (MappedByteBuffer) masterBuffer.duplicate();
        duplicate.order(ByteOrder.BIG_ENDIAN);
        return duplicate;
      });

    } catch (IOException e) {
      throw new RuntimeException("Erro ao carregar VP-Tree", e);
    }
  }

  private MappedByteBuffer buffer() {
    return localBuffer.get();
  }

  public int size() {
    return size;
  }

  public int pointIndex(int node) {
    return buffer().getInt(node * NODE_SIZE);
  }

  public float radius(int node) {
    return buffer().getFloat(node * NODE_SIZE + 4);
  }

  public int left(int node) {
    return buffer().getInt(node * NODE_SIZE + 8);
  }

  public int right(int node) {
    return buffer().getInt(node * NODE_SIZE + 12);
  }
}
