package rinha.backend.tools;

import rinha.backend.ReferenceDataset;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

public final class VpTreeBuilder {

  private final ReferenceDataset dataset;

  private int[] pointIndex;

  private int[] nodePointIndex;
  private float[] nodeRadius;
  private int[] nodeLeft;
  private int[] nodeRight;

  private int nodeCount;

  private float[] vectors;
  private float[] tempDistances;

  private static final int DIMENSIONS = 14;

  static void main() throws Exception {
    ReferenceDataset dataset = new ReferenceDataset();
    VpTreeBuilder builder = new VpTreeBuilder(dataset);
    builder.buildAndSave(Path.of("vptree.bin"));
  }

  public VpTreeBuilder(ReferenceDataset dataset) {
    this.dataset = dataset;
  }

  public void buildAndSave(Path output) throws Exception {
    final int n = dataset.size();
    final int totalFloats = n * DIMENSIONS;

    vectors = new float[totalFloats];

    for (int i = 0, base = 0; i < n; i++, base += DIMENSIONS) {
      for (int d = 0; d < DIMENSIONS; d++) {
        vectors[base + d] = dataset.get(i, d);
      }
    }

    pointIndex = new int[n];
    tempDistances = new float[n];

    for (int i = 0; i < n; i++) {
      pointIndex[i] = i;
    }

    nodePointIndex = new int[n];
    nodeRadius = new float[n];
    nodeLeft = new int[n];
    nodeRight = new int[n];

    Arrays.fill(nodeLeft, -1);
    Arrays.fill(nodeRight, -1);

    build(0, n);

    write(output);
  }

  private int build(int from, int to) {
    final int count = to - from;

    if (count <= 0) return -1;

    final int node = nodeCount++;

    int randomPos =
      from + java.util.concurrent.ThreadLocalRandom.current().nextInt(count);

    swap(from, randomPos);

    final int vp = pointIndex[from];
    nodePointIndex[node] = vp;

    if (count == 1) {
      nodeRadius[node] = 0f;
      return node;
    }

    final int start = from + 1;

    for (int i = start; i < to; i++) {
      tempDistances[i] = distance(vp, pointIndex[i]);
    }

    int medianIndex = start + ((count - 1) >>> 1);

    quickSelect(start, to - 1, medianIndex);

    float radius = tempDistances[medianIndex];
    nodeRadius[node] = radius;

    int split = start;

    for (int i = start; i < to; i++) {
      if (tempDistances[i] < radius) {
        swap(i, split++);
      }
    }

    for (int i = start; i < split; i++) {

      float d = distance(vp, pointIndex[i]);

      if (d >= radius) {
        throw new RuntimeException(
          "ERRO LEFT d=" + d +
            " radius=" + radius
        );
      }
    }

    for (int i = split; i < to; i++) {

      float d = distance(vp, pointIndex[i]);

      if (d < radius) {
        throw new RuntimeException(
          "ERRO RIGHT d=" + d +
            " radius=" + radius
        );
      }
    }

    nodeLeft[node] = build(start, split);
    nodeRight[node] = build(split, to);

    return node;
  }

  private float distance(int a, int b) {
    final int baseA = a * DIMENSIONS;
    final int baseB = b * DIMENSIONS;

    float sum = 0f;

    for (int d = 0; d < DIMENSIONS; d++) {
      float diff = vectors[baseA + d] - vectors[baseB + d];
      sum += diff * diff;
    }

    return sum;
  }

  private void quickSelect(int left, int right, int k) {
    while (left < right) {
      int pivot = partition(left, right);

      if (pivot == k) return;

      if (k < pivot) {
        right = pivot - 1;
      } else {
        left = pivot + 1;
      }
    }
  }

  private int partition(int left, int right) {
    float pivotValue = tempDistances[right];
    int store = left;

    for (int i = left; i < right; i++) {
      if (tempDistances[i] < pivotValue) {
        swap(i, store++);
      }
    }

    swap(store, right);
    return store;
  }

  private void swap(int i, int j) {
    if (i == j) return;

    float dTmp = tempDistances[i];
    tempDistances[i] = tempDistances[j];
    tempDistances[j] = dTmp;

    int pTmp = pointIndex[i];
    pointIndex[i] = pointIndex[j];
    pointIndex[j] = pTmp;
  }

  private void write(Path output) throws IOException {
    try (DataOutputStream out = new DataOutputStream(
      new BufferedOutputStream(Files.newOutputStream(output), 1 << 20))) {

      for (int i = 0; i < nodeCount; i++) {
        out.writeInt(nodePointIndex[i]);
        out.writeFloat(nodeRadius[i]);
        out.writeInt(nodeLeft[i]);
        out.writeInt(nodeRight[i]);
      }
    }
  }
}
