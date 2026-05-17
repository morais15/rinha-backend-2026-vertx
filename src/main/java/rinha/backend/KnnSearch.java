package rinha.backend;

public final class KnnSearch {

  private static final int K = 5;
  private static final int DIMENSIONS = 14;

  private final ReferenceDataset dataset;

  public KnnSearch(ReferenceDataset dataset) {
    this.dataset = dataset;
  }

  public double search(float[] query) {

    // Top 5 menores distâncias
    float[] bestDistances = {
      Float.MAX_VALUE,
      Float.MAX_VALUE,
      Float.MAX_VALUE,
      Float.MAX_VALUE,
      Float.MAX_VALUE
    };

    // Labels correspondentes
    byte[] bestLabels = new byte[K];

    // Percorre todos os 3 milhões de registros
    for (int i = 0; i < dataset.size(); i++) {

      float distance = 0f;

      // Distância euclidiana (sem sqrt)
      for (int d = 0; d < DIMENSIONS; d++) {
        float diff = query[d] - dataset.get(i, d);

        distance += diff * diff;
      }

      // Verifica se entra no top 5
      if (distance < bestDistances[K - 1]) {

        int pos = K - 1;

        while (pos > 0 && distance < bestDistances[pos - 1]) {
          bestDistances[pos] = bestDistances[pos - 1];
          bestLabels[pos] = bestLabels[pos - 1];
          pos--;
        }

        bestDistances[pos] = distance;
        bestLabels[pos] = dataset.label(i);
      }
    }

    // fraud = 1, legit = 0
    int fraudCount = 0;

    for (int i = 0; i < K; i++) {
      fraudCount += bestLabels[i];
    }

    return fraudCount / 5.0;
  }
}
