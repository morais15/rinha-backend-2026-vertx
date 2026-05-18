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

  /**
   * Constrói a árvore usando o intervalo [from, to).
   * Retorna o índice do nó criado.
   */
  private int build(int from, int to) {
    int count = to - from;

    if (count <= 0) {
      return -1;
    }

    // Escolhe o primeiro elemento como vantage point
    int vp = pointIndex[from];

    // Cria o nó
    int node = nodeCount++;

    if ((nodeCount % 10_000) == 0) {
      System.out.printf(
        "Nós criados: %,d (%.2f%%)%n",
        nodeCount,
        nodeCount * 100.0 / dataset.size()
      );
    }

    nodePointIndex[node] = vp;

    // Folha
    if (count == 1) {
      nodeRadius[node] = 0f;
      return node;
    }

    // Calcula as distâncias do vantage point para os demais pontos
    for (int i = from + 1; i < to; i++) {
      float dist = squaredDistance(vp, pointIndex[i]);

      if (!Float.isFinite(dist)) {
        System.out.printf(
          "Distância inválida! vp=%d point=%d dist=%f%n",
          vp,
          pointIndex[i],
          dist
        );
        throw new RuntimeException("Distância inválida");
      }

      distances[i] = dist;
    }

    // Encontra a mediana das distâncias
    int median = from + 1 + (count - 2) / 2;
    quickSelect(from + 1, to - 1, median);

    // Raio do nó (distância mediana)
    nodeRadius[node] = distances[median];

    int leftFrom = from + 1;
    int leftTo = median;
    int rightFrom = median;
    int rightTo = to;

    // Construção sequencial
    nodeLeft[node] = build(leftFrom, leftTo);
    nodeRight[node] = build(rightFrom, rightTo);

    return node;
  }

  /**
   * Distância euclidiana ao quadrado (evita sqrt).
   */
  private float squaredDistance(int a, int b) {
    int baseA = a * dimensions;
    int baseB = b * dimensions;

    float sum = 0f;

    for (int d = 0; d < dimensions; d++) {
      float va = vectors[baseA + d];
      float vb = vectors[baseB + d];

      // Log detalhado caso algum valor seja inválido
      if (!Float.isFinite(va) || !Float.isFinite(vb)) {
        System.out.printf(
          "Valor inválido encontrado! a=%d b=%d dim=%d va=%f vb=%f%n",
          a,
          b,
          d,
          va,
          vb
        );

        // Mostra todas as dimensões dos dois vetores
        System.out.println("=== Vetor A ===");
        for (int i = 0; i < dimensions; i++) {
          System.out.printf("A[%d] = %f%n", i, vectors[baseA + i]);
        }

        System.out.println("=== Vetor B ===");
        for (int i = 0; i < dimensions; i++) {
          System.out.printf("B[%d] = %f%n", i, vectors[baseB + i]);
        }

        throw new RuntimeException("Valor inválido no vetor");
      }

      float diff = va - vb;
      float term = diff * diff;

      // Log caso o termo individual estoure
      if (!Float.isFinite(term)) {
        System.out.printf(
          "Overflow no termo! a=%d b=%d dim=%d va=%f vb=%f diff=%f term=%f%n",
          a,
          b,
          d,
          va,
          vb,
          diff,
          term
        );
        throw new RuntimeException("Overflow no termo da distância");
      }

      sum += term;

      // Log caso a soma fique inválida
      if (!Float.isFinite(sum)) {
        System.out.printf(
          "Overflow na soma! a=%d b=%d dim=%d term=%f sum=%f%n",
          a,
          b,
          d,
          term,
          sum
        );

        System.out.println("=== Vetor A ===");
        for (int i = 0; i < dimensions; i++) {
          System.out.printf("A[%d] = %f%n", i, vectors[baseA + i]);
        }

        System.out.println("=== Vetor B ===");
        for (int i = 0; i < dimensions; i++) {
          System.out.printf("B[%d] = %f%n", i, vectors[baseB + i]);
        }

        throw new RuntimeException("Overflow na soma da distância");
      }
    }

    return sum;
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
