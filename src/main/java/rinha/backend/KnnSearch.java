package rinha.backend;

import java.util.Arrays;

public final class KnnSearch {

  private static final int K = 5;
  private static final int DIMENSIONS = 14;

  private static final long MAX_VALUE =
    Long.MAX_VALUE;

  private final ReferenceDataset dataset;
  private final VpTree tree = new VpTree();

  private final ThreadLocal<long[]> bestDistancesLocal =
    ThreadLocal.withInitial(() -> new long[K]);

  private final ThreadLocal<byte[]> bestLabelsLocal =
    ThreadLocal.withInitial(() -> new byte[K]);

  public KnnSearch(ReferenceDataset dataset) {
    this.dataset = dataset;
  }

  public int search(short[] query) {

    final long[] bestDistances =
      bestDistancesLocal.get();

    final byte[] bestLabels =
      bestLabelsLocal.get();

    Arrays.fill(bestDistances, MAX_VALUE);
    Arrays.fill(bestLabels, (byte) 0);

    searchNode(
      0,
      query,
      bestDistances,
      bestLabels
    );

    return bestLabels[0]
      + bestLabels[1]
      + bestLabels[2]
      + bestLabels[3]
      + bestLabels[4];
  }

  private void searchNode(
    int node,
    short[] query,
    long[] bestDistances,
    byte[] bestLabels
  ) {

    if (node == -1) {
      return;
    }

    final int pointIndex =
      tree.pointIndex(node);

    final long distance =
      distanceSquared(query, pointIndex);

    updateBest(
      distance,
      dataset.label(pointIndex),
      bestDistances,
      bestLabels
    );

    final long radius =
      tree.radius(node);

    final int left =
      tree.left(node);

    final int right =
      tree.right(node);

    final long tau =
      bestDistances[K - 1];

    if (distance < radius) {

      searchNode(
        left,
        query,
        bestDistances,
        bestLabels
      );

      if (distance + tau >= radius) {
        searchNode(
          right,
          query,
          bestDistances,
          bestLabels
        );
      }

    } else {

      searchNode(
        right,
        query,
        bestDistances,
        bestLabels
      );

      if (distance - tau < radius) {
        searchNode(
          left,
          query,
          bestDistances,
          bestLabels
        );
      }
    }
  }

  private long distanceSquared(
    short[] query,
    int recordIndex
  ) {

    long sum = 0;

    for (int d = 0; d < DIMENSIONS; d++) {

      final long diff =
        query[d] -
          dataset.getQuantized(
            recordIndex,
            d
          );

      sum += diff * diff;
    }

    return sum;
  }

  private void updateBest(
    long distance,
    byte label,
    long[] bestDistances,
    byte[] bestLabels
  ) {

    if (distance >= bestDistances[K - 1]) {
      return;
    }

    int pos = K - 1;

    while (
      pos > 0 &&
        distance < bestDistances[pos - 1]
    ) {

      bestDistances[pos] =
        bestDistances[pos - 1];

      bestLabels[pos] =
        bestLabels[pos - 1];

      pos--;
    }

    bestDistances[pos] = distance;
    bestLabels[pos] = label;
  }
}
