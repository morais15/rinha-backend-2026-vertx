package rinha.backend;

import java.io.IOException;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import static rinha.backend.Constants.SHORT_SCALE;

public final class ReferenceDataset {

  private static final int DIMENSIONS = 14;

  private static final int SHORT_SIZE = 2;
  private static final int LABEL_SIZE = 1;

  private static final int RECORD_SIZE =
    DIMENSIONS * SHORT_SIZE + LABEL_SIZE;

  private static final String FILE_PATH = "references.bin";

  private final MappedByteBuffer buffer;
  private final int size;

  public ReferenceDataset() {
    try (
      FileChannel channel = FileChannel.open(
        Path.of(FILE_PATH),
        StandardOpenOption.READ
      )
    ) {

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

  public float get(int recordIndex, int dimension) {
    final int offset =
      recordIndex * RECORD_SIZE +
        dimension * SHORT_SIZE;

    return buffer.getShort(offset) / SHORT_SCALE;
  }

  public short getQuantized(int recordIndex, int dimension) {
    final int offset =
      recordIndex * RECORD_SIZE +
        dimension * SHORT_SIZE;

    return buffer.getShort(offset);
  }

  // 0 = legit, 1 = fraud
  public byte label(int recordIndex) {
    final int offset =
      recordIndex * RECORD_SIZE +
        DIMENSIONS * SHORT_SIZE;

    return buffer.get(offset);
  }
}
