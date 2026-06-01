package rinha.backend;

import java.util.Arrays;

public final class KnnSearch {

  private static final int K = 5;
  private static final int DIMENSIONS = 14;
  private static final float INF = Float.POSITIVE_INFINITY;

  private final ReferenceDataset dataset;
  private final VpTree tree = new VpTree();

  private final ThreadLocal<float[]> bestDistLocal =
    ThreadLocal.withInitial(() -> new float[K]);

  private final ThreadLocal<byte[]> bestLabelLocal =
    ThreadLocal.withInitial(() -> new byte[K]);

  public KnnSearch(ReferenceDataset dataset) {
    this.dataset = dataset;
  }

  public int bruteForce(float[] query) {

    float[] bestDist = new float[K];
    byte[] bestLabel = new byte[K];

    Arrays.fill(bestDist, INF);

    for (int i = 0; i < dataset.size(); i++) {
      float dist = distance(query, i);
      insert(dist, dataset.label(i), bestDist, bestLabel);
    }

    System.out.println("brute");

    for (int i = 0; i < K; i++) {
      System.out.println(
        i +
          " dist=" + bestDist[i] +
          " label=" + bestLabel[i]
      );
    }

    return bestLabel[0]
      + bestLabel[1]
      + bestLabel[2]
      + bestLabel[3]
      + bestLabel[4];
  }

  public int search(float[] query) {

    float[] bestDist = bestDistLocal.get();
    byte[] bestLabel = bestLabelLocal.get();

    Arrays.fill(bestDist, INF);
    Arrays.fill(bestLabel, (byte) 0);

    searchNode(0, query, bestDist, bestLabel);

    System.out.println("VP");

    for (int i = 0; i < K; i++) {
      System.out.println(
        i +
          " dist=" + bestDist[i] +
          " label=" + bestLabel[i]
      );
    }

    return bestLabel[0] + bestLabel[1] + bestLabel[2] + bestLabel[3] + bestLabel[4];
  }

  private void searchNode(
    int node,
    float[] query,
    float[] bestDist,
    byte[] bestLabel
  ) {

    if (node == -1) return;

    int idx = tree.pointIndex(node);

    float dist = distance(query, idx);

    insert(dist, dataset.label(idx), bestDist, bestLabel);

    float radius = tree.radius(node);
    int left = tree.left(node);
    int right = tree.right(node);

    float tau = bestDist[K - 1];

    if (dist < radius) {

      searchNode(left, query, bestDist, bestLabel);

      if (dist + tau >= radius) {
        searchNode(right, query, bestDist, bestLabel);
      }

    } else {

      searchNode(right, query, bestDist, bestLabel);

      if (dist - tau < radius) {
        searchNode(left, query, bestDist, bestLabel);
      }
    }
  }

  private float distance(float[] query, int idx) {
    float sum = 0f;

    for (int d = 0; d < DIMENSIONS; d++) {
      float diff = query[d] - dataset.get(idx, d);
      sum += diff * diff;
    }

    return sum;
  }

  private void insert(
    float dist,
    byte label,
    float[] bestDist,
    byte[] bestLabel
  ) {

    if (dist >= bestDist[K - 1]) return;

    int i = K - 1;

    while (i > 0 && dist < bestDist[i - 1]) {
      bestDist[i] = bestDist[i - 1];
      bestLabel[i] = bestLabel[i - 1];
      i--;
    }

    bestDist[i] = dist;
    bestLabel[i] = label;
  }
}
