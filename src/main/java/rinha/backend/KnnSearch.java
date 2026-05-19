package rinha.backend;

public final class KnnSearch {

  private static final int K = 5;
  private static final int DIMENSIONS = 14;

  private final ReferenceDataset dataset;
  private final VpTree tree = new VpTree();

  // Ative/desative logs de depuração
  private static final boolean DEBUG = false;

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
      debugCompareWithBruteForce(query);
    }

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
    float distance = squaredDistance(query, pointIndex);

    updateBest(
      distance,
      dataset.label(pointIndex),
      bestDistances,
      bestLabels
    );

    float radius = tree.radius(node);
    int left = tree.left(node);
    int right = tree.right(node);

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

    float tau = bestDistances[K - 1];

    if (tau == Float.MAX_VALUE) {
      tau = Float.POSITIVE_INFINITY;
    }

    if (distance < radius) {
      searchNode(left, query, bestDistances, bestLabels, visited);

      tau = bestDistances[K - 1];

      if (tau == Float.MAX_VALUE) {
        tau = Float.POSITIVE_INFINITY;
      }

      if (DEBUG && right != -1) {
        StringBuilder path = new StringBuilder();

        if (
          distance + tau < radius &&
            findPointInTree(right, 766951, path)
        ) {
          System.out.println();
          System.out.println("=== PODA SUSPEITA DETECTADA ===");
          System.out.printf(
            "node=%d point=%d radius=%.9f%n",
            node,
            pointIndex,
            radius
          );
          System.out.printf(
            "distance=%.9f tau=%.9f distance+tau=%.9f%n",
            distance,
            tau,
            distance + tau
          );
          System.out.println("RIGHT=" + right + " contém o ponto 766951");
          System.out.println(path);
        }
      }

      if (distance + tau >= radius) {
        searchNode(right, query, bestDistances, bestLabels, visited);
      }

    } else {
      searchNode(right, query, bestDistances, bestLabels, visited);

      tau = bestDistances[K - 1];

      if (tau == Float.MAX_VALUE) {
        tau = Float.POSITIVE_INFINITY;
      }

      if (DEBUG && left != -1) {
        StringBuilder path = new StringBuilder();

        if (
          distance - tau >= radius &&
            findPointInTree(left, 766951, path)
        ) {
          System.out.println();
          System.out.println("=== PODA SUSPEITA DETECTADA ===");
          System.out.printf(
            "node=%d point=%d radius=%.9f%n",
            node,
            pointIndex,
            radius
          );
          System.out.printf(
            "distance=%.9f tau=%.9f distance-tau=%.9f%n",
            distance,
            tau,
            distance - tau
          );
          System.out.println("LEFT=" + left + " contém o ponto 766951");
          System.out.println(path);
        }
      }

      if (distance - tau < radius) {
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

    return (float) Math.sqrt(distance);
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

  private void debugCompareWithBruteForce(float[] query) {
    float[] vpDistances = new float[K];
    byte[] vpLabels = new byte[K];

    float[] bruteDistances = new float[K];
    byte[] bruteLabels = new byte[K];

    // Inicializa buffers
    for (int i = 0; i < K; i++) {
      vpDistances[i] = Float.MAX_VALUE;
      bruteDistances[i] = Float.MAX_VALUE;
      vpLabels[i] = 0;
      bruteLabels[i] = 0;
    }

    // Busca na VP-Tree
    searchNode(0, query, vpDistances, vpLabels, new int[]{0});

    // Busca força bruta
    for (int i = 0; i < dataset.size(); i++) {
      float distance = squaredDistance(query, i);
      updateBest(distance, dataset.label(i), bruteDistances, bruteLabels);
    }

    // Compara resultados
    boolean equal = true;
    for (int i = 0; i < K; i++) {
      if (Math.abs(vpDistances[i] - bruteDistances[i]) > 1e-7 ||
        vpLabels[i] != bruteLabels[i]) {
        equal = false;
        break;
      }
    }

    if (!equal) {
      System.out.println("=== DIVERGÊNCIA ENCONTRADA ===");

      System.out.println("VP-TREE:");
      for (int i = 0; i < K; i++) {
        System.out.printf(
          "k=%d distance=%.9f label=%d%n",
          i,
          vpDistances[i],
          vpLabels[i]
        );
      }

      System.out.println("FORÇA BRUTA:");
      for (int i = 0; i < K; i++) {
        System.out.printf(
          "k=%d distance=%.9f label=%d%n",
          i,
          bruteDistances[i],
          bruteLabels[i]
        );
      }

      // Descobre qual ponto foi perdido pela VP-Tree.
// O ponto mais útil para investigar é o melhor vizinho da força bruta.
      int missingPoint = -1;
      float missingDistance = bruteDistances[0];

// Procura o índice real do ponto correspondente à menor distância
      for (int i = 0; i < dataset.size(); i++) {
        float d = squaredDistance(query, i);
        if (Math.abs(d - missingDistance) < 1e-7f) {
          missingPoint = i;
          break;
        }
      }

      if (missingPoint != -1) {
        System.out.println();
        System.out.println("=== MELHOR VIZINHO PERDIDO ===");
        System.out.println("pointIndex = " + missingPoint);
        System.out.println("distance   = " + missingDistance);

        StringBuilder path = new StringBuilder();

        if (findPointInTree(0, missingPoint, path)) {
          System.out.println();
          System.out.println("=== CAMINHO ATÉ O PONTO NA VP-TREE ===");
          System.out.println(path);
        } else {
          System.out.println("Ponto não encontrado na árvore!");
        }
      }

      throw new RuntimeException("VP-Tree difere da força bruta");
    }
  }

  private boolean findPointInTree(int node, int targetPoint, StringBuilder path) {
    if (node == -1) {
      return false;
    }

    int point = tree.pointIndex(node);

    if (point == targetPoint) {
      path.append(" -> node=").append(node)
        .append(" point=").append(point);
      return true;
    }

    path.append(" -> node=").append(node)
      .append(" point=").append(point);

    if (findPointInTree(tree.left(node), targetPoint, path)) {
      return true;
    }

    if (findPointInTree(tree.right(node), targetPoint, path)) {
      return true;
    }

    int idx = path.lastIndexOf(" -> node=");
    if (idx >= 0) {
      path.setLength(idx);
    }

    return false;
  }
}
