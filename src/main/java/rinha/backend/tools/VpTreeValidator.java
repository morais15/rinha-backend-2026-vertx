package rinha.backend.tools;

import rinha.backend.KnnSearch;
import rinha.backend.ReferenceDataset;

public class VpTreeValidator {

  static void main() {

    ReferenceDataset dataset = new ReferenceDataset();
    KnnSearch knn = new KnnSearch(dataset);

    float[] query = new float[14];

    for (int i = 0; i < dataset.size(); i++) {

      for (int d = 0; d < 14; d++) {
        query[d] = dataset.get(i, d);
      }

      int vp = knn.search(query);
      int brute = knn.bruteForce(query);

      if (vp != brute) {

        System.out.println(
          "ERRO idx=" + i +
            " vp=" + vp +
            " brute=" + brute
        );

        break;
      }

      if ((i % 10000) == 0) {
        System.out.println("validado " + i);
      }
    }
  }
}
