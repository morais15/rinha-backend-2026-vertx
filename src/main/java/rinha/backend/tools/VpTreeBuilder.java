package rinha.backend.tools;

import rinha.backend.ReferenceDataset;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

public final class VpTreeBuilder {

  private static final int DIMENSIONS = 14;

  private final ReferenceDataset dataset;

  private int[] pointIndex;
  private long[] distances;

  private int[] nodePointIndex;
  private long[] nodeRadius;
  private int[] nodeLeft;
  private int[] nodeRight;

  private int nodeCount;

  private short[] vectors;

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
    final int totalValues = n * DIMENSIONS;

    vectors = new short[totalValues];

    for (int i = 0, base = 0; i < n; i++, base += DIMENSIONS) {
      for (int d = 0; d < DIMENSIONS; d++) {
        vectors[base + d] =
          dataset.getQuantized(i, d);
      }
    }

    pointIndex = new int[n];
    distances = new long[n];

    for (int i = 0; i < n; i++) {
      pointIndex[i] = i;
    }

    nodePointIndex = new int[n];
    nodeRadius = new long[n];
    nodeLeft = new int[n];
    nodeRight = new int[n];

    Arrays.fill(nodeLeft, -1);
    Arrays.fill(nodeRight, -1);

    build(0, n);

    write(output);
  }

  private int build(int from, int to) {

    final int count = to - from;

    if (count <= 0) {
      return -1;
    }

    final int node = nodeCount++;
    final int vp = pointIndex[from];

    nodePointIndex[node] = vp;

    if (count == 1) {
      return node;
    }

    final int start = from + 1;

    for (int i = start; i < to; i++) {
      distances[i] =
        distanceSquared(vp, pointIndex[i]);
    }

    final int median =
      from + 1 + ((to - from - 1) >>> 1);

    quickSelect(start, to - 1, median);

    final long radius = distances[median];

    nodeRadius[node] = radius;

    int split = start;

    for (int i = start; i < to; i++) {
      if (distances[i] < radius) {
        swap(i, split++);
      }
    }

    nodeLeft[node] = build(start, split);
    nodeRight[node] = build(split, to);

    return node;
  }

  private long distanceSquared(int a, int b) {

    final int baseA = a * DIMENSIONS;
    final int baseB = b * DIMENSIONS;

    long sum = 0;

    for (int d = 0; d < DIMENSIONS; d++) {

      final long diff =
        vectors[baseA + d] -
          vectors[baseB + d];

      sum += diff * diff;
    }

    return sum;
  }

  private void quickSelect(int left, int right, int k) {

    while (left < right) {

      final int pivot =
        partition(left, right);

      if (pivot == k) {
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

    final long pivotValue =
      distances[right];

    int store = left;

    for (int i = left; i < right; i++) {
      if (distances[i] < pivotValue) {
        swap(i, store++);
      }
    }

    swap(store, right);

    return store;
  }

  private void swap(int i, int j) {

    if (i == j) {
      return;
    }

    final long distanceTmp = distances[i];
    distances[i] = distances[j];
    distances[j] = distanceTmp;

    final int pointTmp = pointIndex[i];
    pointIndex[i] = pointIndex[j];
    pointIndex[j] = pointTmp;
  }

  private void write(Path output)
    throws IOException {

    try (
      DataOutputStream out =
        new DataOutputStream(
          new BufferedOutputStream(
            Files.newOutputStream(output),
            1 << 20
          )
        )
    ) {

      for (int i = 0; i < nodeCount; i++) {

        out.writeInt(nodePointIndex[i]);
        out.writeLong(nodeRadius[i]);
        out.writeInt(nodeLeft[i]);
        out.writeInt(nodeRight[i]);
      }
    }
  }
}
