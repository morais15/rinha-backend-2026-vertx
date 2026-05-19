package rinha.backend.tools;

import rinha.backend.ReferenceDataset;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;

public final class VpTreeBuilder {

  private final ReferenceDataset dataset;

  private int[] pointIndex;
  private float[] distances;

  // Arrays que representam os nós
  private int[] nodePointIndex;
  private float[] nodeRadius;
  private int[] nodeLeft;
  private int[] nodeRight;

  private int nodeCount;

  private float[] vectors;
  private int dimensions;

  static void main() throws Exception {
    Path output = Path.of("vptree.bin");

    ReferenceDataset dataset = new ReferenceDataset();
    VpTreeBuilder builder = new VpTreeBuilder(dataset);

    builder.buildAndSave(output);
  }

  public VpTreeBuilder(ReferenceDataset dataset) {
    this.dataset = dataset;
  }

  public void buildAndSave(Path output) throws Exception {
    int n = dataset.size();

    System.out.println("Dataset size: " + n);

    this.dimensions = dataset.dimensions();
    this.vectors = new float[n * dimensions];

    System.out.println("Carregando dataset em memória...");

    for (int i = 0; i < n; i++) {
      int base = i * dimensions;
      for (int d = 0; d < dimensions; d++) {
        vectors[base + d] = dataset.get(i, d);
      }
    }

    System.out.println("Dataset carregado em RAM.");

    pointIndex = new int[n];
    distances = new float[n];

    for (int i = 0; i < n; i++) {
      pointIndex[i] = i;
    }

    // 1 nó por ponto
    nodePointIndex = new int[n];
    nodeRadius = new float[n];
    nodeLeft = new int[n];
    nodeRight = new int[n];

    Arrays.fill(nodeLeft, -1);
    Arrays.fill(nodeRight, -1);

    long t0 = System.nanoTime();
    int root = build(0, n);
    long t1 = System.nanoTime();

    System.out.printf(
      "VP-Tree construída. root=%d, nodes=%d, tempo=%.2f s%n",
      root,
      nodeCount,
      (t1 - t0) / 1_000_000_000.0
    );

    write(output);
  }

  private int build(int from, int to) {
    int count = to - from;

    if (count <= 0) {
      return -1;
    }

    int vp = pointIndex[from];
    int node = nodeCount++;

    if ((nodeCount % 10_000) == 0) {
      System.out.printf(
        "Nós criados: %,d (%.2f%%)%n",
        nodeCount,
        nodeCount * 100.0 / dataset.size()
      );
    }

    nodePointIndex[node] = vp;

    if (count == 1) {
      nodeRadius[node] = 0f;
      return node;
    }

    for (int i = from + 1; i < to; i++) {
      distances[i] = squaredDistance(vp, pointIndex[i]);
    }

    int median = (from + 1 + to) >>> 1;
    quickSelect(from + 1, to - 1, median);

    float radius = distances[median];
    nodeRadius[node] = radius;

    int split = from + 1;

    for (int i = from + 1; i < to; i++) {
      if (distances[i] < radius) {
        swap(i, split);
        split++;
      }
    }

    final boolean VALIDATE_PARTITION = true;

    if (VALIDATE_PARTITION) {
      for (int i = from + 1; i < split; i++) {
        float d = squaredDistance(vp, pointIndex[i]);

        if (!(d < radius)) {
          System.out.println();
          System.out.println("=== PARTIÇÃO INVÁLIDA (LEFT) ===");
          System.out.printf(
            "node=%d vp=%d radius=%.9f%n",
            node,
            vp,
            radius
          );
          System.out.printf(
            "index=%d point=%d distance=%.9f%n",
            i,
            pointIndex[i],
            d
          );
          System.out.println("Esperado: distance < radius");

          throw new RuntimeException("VP-Tree inválida (LEFT)");
        }
      }

      for (int i = split; i < to; i++) {
        float d = squaredDistance(vp, pointIndex[i]);

        if (!(d >= radius)) {
          System.out.println();
          System.out.println("=== PARTIÇÃO INVÁLIDA (RIGHT) ===");
          System.out.printf(
            "node=%d vp=%d radius=%.9f%n",
            node,
            vp,
            radius
          );
          System.out.printf(
            "index=%d point=%d distance=%.9f%n",
            i,
            pointIndex[i],
            d
          );
          System.out.println("Esperado: distance >= radius");

          throw new RuntimeException("VP-Tree inválida (RIGHT)");
        }
      }
    }

    nodeLeft[node] = build(from + 1, split);
    nodeRight[node] = build(split, to);

    return node;
  }

  private float squaredDistance(int a, int b) {
    int baseA = a * dimensions;
    int baseB = b * dimensions;

    float sum = 0f;

    for (int d = 0; d < dimensions; d++) {
      float diff = vectors[baseA + d] - vectors[baseB + d];
      sum += diff * diff;
    }

    return (float) Math.sqrt(sum);
  }

  /**
   * Quickselect para posicionar o k-ésimo menor elemento.
   */
  private void quickSelect(int left, int right, int k) {
    while (left < right) {
      int pivot = partition(left, right);

      if (k == pivot) {
        return;
      }

      if (k < pivot) {
        right = pivot - 1;
      } else {
        left = pivot + 1;
      }
    }
  }

  private int partition(int left, int right) {
    float pivotValue = distances[right];
    int store = left;

    for (int i = left; i < right; i++) {
      if (distances[i] < pivotValue) {
        swap(i, store);
        store++;
      }
    }

    swap(store, right);

    return store;
  }

  private void swap(int i, int j) {
    if (i == j) {
      return;
    }

    float td = distances[i];
    distances[i] = distances[j];
    distances[j] = td;

    int tp = pointIndex[i];
    pointIndex[i] = pointIndex[j];
    pointIndex[j] = tp;
  }

  private void write(Path output) throws IOException {
    try (
      DataOutputStream out = new DataOutputStream(
        new BufferedOutputStream(
          java.nio.file.Files.newOutputStream(output)
        )
      )
    ) {
      int totalNodes = nodeCount;

      for (int i = 0; i < totalNodes; i++) {
        out.writeInt(nodePointIndex[i]);
        out.writeFloat(nodeRadius[i]);
        out.writeInt(nodeLeft[i]);
        out.writeInt(nodeRight[i]);
      }

      long bytes = (long) totalNodes * 16;

      System.out.printf(
        "Arquivo gerado: %s (%d nós, %.2f MB)%n",
        output,
        totalNodes,
        bytes / 1024.0 / 1024.0
      );
    }
  }
}
