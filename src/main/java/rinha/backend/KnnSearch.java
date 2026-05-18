package rinha.backend;

public final class KnnSearch {

  private static final int K = 5;
  private static final int DIMENSIONS = 14;

  private final ReferenceDataset dataset;
  private final VpTree tree = new VpTree();

  // Ative/desative logs de depuração
  private static final boolean DEBUG = true;

  // Cada thread terá seus próprios buffers reutilizáveis.
  private final ThreadLocal<float[]> bestDistancesLocal =
    ThreadLocal.withInitial(() -> new float[K]);

  private final ThreadLocal<byte[]> bestLabelsLocal =
    ThreadLocal.withInitial(() -> new byte[K]);

  public KnnSearch(ReferenceDataset dataset) {
    this.dataset = dataset;

    if (DEBUG) {
      System.out.println("VP-Tree nodes: " + tree.size());
      System.out.println("Dataset size: " + dataset.size());
    }
  }

  public double search(float[] query) {

    // Recupera os buffers da thread atual
    float[] bestDistances = bestDistancesLocal.get();
    byte[] bestLabels = bestLabelsLocal.get();

    // Reinicializa o top K
    for (int i = 0; i < K; i++) {
      bestDistances[i] = Float.MAX_VALUE;
      bestLabels[i] = 0;
    }

    // Contador de nós visitados
    int[] visited = {0};

    // A raiz da VP-Tree está sempre no nó 0
    searchNode(0, query, bestDistances, bestLabels, visited);

    if (DEBUG) {
      System.out.println("Nós visitados: " + visited[0]);

      System.out.println("=== RESULTADO VP-TREE ===");
      for (int i = 0; i < K; i++) {
        System.out.printf(
          "k=%d distance=%.6f label=%d%n",
          i,
          bestDistances[i],
          bestLabels[i]
        );
      }
    }

    // fraud = 1, legit = 0
    int fraudCount = 0;

    for (int i = 0; i < K; i++) {
      fraudCount += bestLabels[i];
    }

    return fraudCount / 5.0;
  }

  private void searchNode(
    int node,
    float[] query,
    float[] bestDistances,
    byte[] bestLabels,
    int[] visited
  ) {
    if (node == -1) {
      return;
    }

    visited[0]++;

    int pointIndex = tree.pointIndex(node);

    // Distância da query até o ponto armazenado neste nó
    float distance = squaredDistance(query, pointIndex);

    // Atualiza o top K
    updateBest(distance, dataset.label(pointIndex), bestDistances, bestLabels);

    float radius = tree.radius(node);

    int left = tree.left(node);
    int right = tree.right(node);

    // Log do nó raiz
    if (DEBUG && visited[0] == 1) {
      System.out.printf(
        "Root: point=%d radius=%.6f left=%d right=%d distance=%.6f%n",
        pointIndex,
        radius,
        left,
        right,
        distance
      );
    }

    if (distance < radius) {
      // Visita primeiro a subárvore interna
      searchNode(left, query, bestDistances, bestLabels, visited);

      // Recalcula tau após a busca
      float tau = bestDistances[K - 1];

      // Decide se precisa visitar a externa
      if (distance + tau >= radius) {
        searchNode(right, query, bestDistances, bestLabels, visited);
      }
    } else {
      // Visita primeiro a subárvore externa
      searchNode(right, query, bestDistances, bestLabels, visited);

      // Recalcula tau após a busca
      float tau = bestDistances[K - 1];

      // Decide se precisa visitar a interna
      if (distance - tau <= radius) {
        searchNode(left, query, bestDistances, bestLabels, visited);
      }
    }
  }

  private float squaredDistance(float[] query, int recordIndex) {
    float distance = 0f;

    for (int d = 0; d < DIMENSIONS; d++) {
      float diff = query[d] - dataset.get(recordIndex, d);
      distance += diff * diff;
    }

    return distance;
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
