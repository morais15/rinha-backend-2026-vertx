package rinha.backend.tools;

import rinha.backend.ReferenceDataset;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

public final class VpTreeBuilder {

  private final ReferenceDataset dataset;

  private int[] pointIndex;
  private float[] distances;

  // Arrays que representam os nós
  private int[] nodePointIndex;
  private float[] nodeRadius;
  private int[] nodeLeft;
  private int[] nodeRight;

  private final AtomicInteger nodeCount = new AtomicInteger();

  private float[] vectors;
  private int dimensions;

  private static final int PARALLEL_THRESHOLD = 100_000;
  private final ForkJoinPool pool = new ForkJoinPool(Runtime.getRuntime().availableProcessors());

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
      nodeCount.get(),
      (t1 - t0) / 1_000_000_000.0
    );

    write(output);

    pool.shutdown();
  }

  /**
   * Constrói a árvore usando o intervalo [from, to).
   * Retorna o índice do nó criado.
   */
  private int build(int from, int to) throws Exception {
    int count = to - from;

    if (count <= 0) {
      return -1;
    }

    // Escolhe o primeiro elemento como vantage point
    int vp = pointIndex[from];

    // Cria o nó
    int node = nodeCount.getAndIncrement();

    int currentNodeCount = nodeCount.get();

    System.out.println("currentNodeCount: " + currentNodeCount);

    if ((currentNodeCount % 10_000) == 0) {
      System.out.printf(
        "Nós criados: %,d (%.2f%%)%n",
        currentNodeCount,
        currentNodeCount * 100.0 / dataset.size()
      );
    }

    nodePointIndex[node] = vp;

    // Folha
    if (count == 1) {
      nodeRadius[node] = 0f;
      return node;
    }

    // Calcula as distâncias do vantage point para os demais pontos
    pool.submit(() ->
      java.util.stream.IntStream
        .range(from + 1, to)
        .parallel()
        .forEach(i ->
          distances[i] = squaredDistance(vp, pointIndex[i])
        )
    ).get();

    // Encontra a mediana das distâncias
    int median = from + 1 + (count - 1) / 2;
    quickSelect(from + 1, to - 1, median);

    // Raio do nó (distância mediana)
    nodeRadius[node] = distances[median];

    // Intervalos das subárvores
    int leftFrom = from + 1;
    int leftTo = median;
    int rightFrom = median;
    int rightTo = to;

    // Para subárvores grandes, constrói em paralelo
    if (count > PARALLEL_THRESHOLD) {
      Future<Integer> leftFuture =
        pool.submit(() -> build(leftFrom, leftTo));

      // Processa a subárvore direita na thread atual
      nodeRight[node] = build(rightFrom, rightTo);

      // Aguarda a conclusão da esquerda
      nodeLeft[node] = leftFuture.get();
    } else {
      // Para subárvores menores, executa sequencialmente
      nodeLeft[node] = build(leftFrom, leftTo);
      nodeRight[node] = build(rightFrom, rightTo);
    }

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
      float diff = vectors[baseA + d] - vectors[baseB + d];
      sum += diff * diff;
    }

    return sum;
  }

  /**
   * Quickselect para posicionar o k-ésimo menor elemento.
   */
  private void quickSelect(int left, int right, int k) {
//    System.out.println("quickSelect");

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
//    System.out.println("partition");

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
//    System.out.println("swap");

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
//    System.out.println("write");

    try (
      DataOutputStream out = new DataOutputStream(
        new BufferedOutputStream(
          java.nio.file.Files.newOutputStream(output)
        )
      )
    ) {
      int totalNodes = nodeCount.get();

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
