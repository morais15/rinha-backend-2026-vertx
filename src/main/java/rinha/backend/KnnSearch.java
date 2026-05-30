package rinha.backend;

import java.util.Arrays;

public final class KnnSearch {

  private static final int K = 5;
  private static final int DIMENSIONS = 14;
  private static final float INFINITY = Float.POSITIVE_INFINITY;
  private static final float MAX_VALUE = Float.MAX_VALUE;

  private final ReferenceDataset dataset;
  private final VpTree tree = new VpTree();

  private final ThreadLocal<float[]> bestDistancesLocal =
    ThreadLocal.withInitial(() -> new float[K]);

  private final ThreadLocal<byte[]> bestLabelsLocal =
    ThreadLocal.withInitial(() -> new byte[K]);

  public KnnSearch(ReferenceDataset dataset) {
    this.dataset = dataset;
  }

  public int search(float[] query) {
    final float[] bestDistances = bestDistancesLocal.get();
    final byte[] bestLabels = bestLabelsLocal.get();

    Arrays.fill(bestDistances, MAX_VALUE);
    Arrays.fill(bestLabels, (byte) 0);

    searchNode(0, query, bestDistances, bestLabels);

    return bestLabels[0]
      + bestLabels[1]
      + bestLabels[2]
      + bestLabels[3]
      + bestLabels[4];
  }

  private void searchNode(
    int node,
    float[] query,
    float[] bestDistances,
    byte[] bestLabels
  ) {
    if (node == -1) {
      return;
    }

    final int pointIndex = tree.pointIndex(node);
    final float distance = distance(query, pointIndex);

    updateBest(
      distance,
      dataset.label(pointIndex),
      bestDistances,
      bestLabels
    );

    final float radius = tree.radius(node);
    final int left = tree.left(node);
    final int right = tree.right(node);

    if (distance < radius) {
      searchNode(left, query, bestDistances, bestLabels);

      final float tau =
        bestDistances[K - 1] == MAX_VALUE
          ? INFINITY
          : bestDistances[K - 1];

      if (distance + tau >= radius) {
        searchNode(right, query, bestDistances, bestLabels);
      }
    } else {
      searchNode(right, query, bestDistances, bestLabels);

      final float tau =
        bestDistances[K - 1] == MAX_VALUE
          ? INFINITY
          : bestDistances[K - 1];

      if (distance - tau < radius) {
        searchNode(left, query, bestDistances, bestLabels);
      }
    }
  }

  private float distance(float[] query, int recordIndex) {
    float sum = 0f;

    for (int d = 0; d < DIMENSIONS; d++) {
      final float diff = query[d] - dataset.get(recordIndex, d);
      sum += diff * diff;
    }

    return (float) Math.sqrt(sum);
  }

  private void updateBest(
    float distance,
    byte label,
    float[] bestDistances,
    byte[] bestLabels
  ) {
    if (distance >= bestDistances[K - 1]) {
      return;
    }

    int pos = K - 1;

    while (pos > 0 && distance < bestDistances[pos - 1]) {
      bestDistances[pos] = bestDistances[pos - 1];
      bestLabels[pos] = bestLabels[pos - 1];
      pos--;
    }

    bestDistances[pos] = distance;
    bestLabels[pos] = label;
  }
}
