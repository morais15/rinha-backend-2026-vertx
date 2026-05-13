package rinha.backend.tools;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.GZIPInputStream;

public final class DatasetConverter {

  private static final int DIMENSIONS = 14;

  static void main() throws IOException {
    final String input = "references.json.gz";
    final String output = "references.bin";

    convert(Path.of(input), Path.of(output));
  }

  public static void convert(Path input, Path output) throws IOException {
    try (
      InputStream file = Files.newInputStream(input);
      GZIPInputStream gzip = new GZIPInputStream(file);
      DataOutputStream out =
        new DataOutputStream(
          new BufferedOutputStream(
            Files.newOutputStream(output)
          )
        )
    ) {
      String json = new String(
        gzip.readAllBytes(),
        StandardCharsets.UTF_8
      );

      JsonArray array = new JsonArray(json);

      for (int i = 0; i < array.size(); i++) {
        JsonObject obj = array.getJsonObject(i);

        JsonArray vector = obj.getJsonArray("vector");

//        System.out.println(vector);

        for (int d = 0; d < DIMENSIONS; d++) {
          out.writeFloat(vector.getFloat(d));
        }

        byte label = (byte) ("fraud".equals(obj.getString("label")) ? 1 : 0);

        System.out.println(label);

//        out.writeByte(label);
      }
    }
  }
}
