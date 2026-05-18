package rinha.backend;

import java.io.IOException;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public final class ReferenceDataset {

  private static final int DIMENSIONS = 14;
  private static final int FLOAT_SIZE = 4;
  private static final int LABEL_SIZE = 1;
  private static final int JUMP_SIZE = 3;

  // 14 floats (56 bytes) + 1 byte de label = 57 bytes
  private static final int RECORD_SIZE = DIMENSIONS * FLOAT_SIZE + LABEL_SIZE + JUMP_SIZE;

  private static final String FILE_PATH = "references.bin";

  private final MappedByteBuffer buffer;
  private final int size;

  public ReferenceDataset() {
    try (FileChannel channel = FileChannel.open(Path.of(FILE_PATH), StandardOpenOption.READ)) {

      long fileSize = channel.size();

      this.size = (int) (fileSize / RECORD_SIZE);

      this.buffer = channel.map(
        FileChannel.MapMode.READ_ONLY,
        0,
        fileSize
      );

      this.buffer.order(ByteOrder.BIG_ENDIAN);
    } catch (IOException e) {
      System.exit(-1);
      throw new RuntimeException(e);
    }
  }

  public int size() {
    return size;
  }

  public int dimensions() {
    return DIMENSIONS;
  }

  public float get(int recordIndex, int dimension) {
    int offset = recordIndex * RECORD_SIZE + dimension * FLOAT_SIZE;
    return buffer.getFloat(offset);
  }

  // 0 = legit, 1 = fraud
  public byte label(int recordIndex) {
    int offset = recordIndex * RECORD_SIZE + DIMENSIONS * FLOAT_SIZE;
    return buffer.get(offset);
  }
}
