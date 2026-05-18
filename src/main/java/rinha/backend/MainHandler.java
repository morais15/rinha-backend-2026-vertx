package rinha.backend;

import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

import static rinha.backend.Vectorizer.vectorize;

public class MainHandler {

  private final ReferenceDataset referenceDataset = new ReferenceDataset();
  private final KnnSearch knnSearch = new KnnSearch(referenceDataset);

  public void ready(RoutingContext ctx) {
    ctx.response()
      .setStatusCode(200)
      .end();
  }

  public void fraudScore(RoutingContext ctx) {
    long t0 = System.nanoTime();

    // Parse do JSON para FraudRequest
    var request = FraudRequest.parse(ctx.body().asJsonObject());
    long t1 = System.nanoTime();

    // Normalização / vetorização
    float[] queryVector = vectorize(request);
    long t2 = System.nanoTime();

    // Busca KNN (agora usando VP-Tree)
    double fraudScore = knnSearch.search(queryVector);
    long t3 = System.nanoTime();

    // Regra de aprovação
    boolean approved = fraudScore < 0.6;
    long t4 = System.nanoTime();

    // Montagem do JSON de resposta
    JsonObject response = new JsonObject()
      .put("approved", approved)
      .put("fraud_score", fraudScore);
    long t5 = System.nanoTime();

    // Serialização para String
    String responseBody = response.encode();
    long t6 = System.nanoTime();

    // Log detalhado
    System.out.printf(
      "parse=%.3f ms | vectorize=%.3f ms | knn=%.3f ms | decision=%.3f ms | response=%.3f ms | encode=%.3f ms | total=%.3f ms%n",
      (t1 - t0) / 1_000_000.0,
      (t2 - t1) / 1_000_000.0,
      (t3 - t2) / 1_000_000.0,
      (t4 - t3) / 1_000_000.0,
      (t5 - t4) / 1_000_000.0,
      (t6 - t5) / 1_000_000.0,
      (t6 - t0) / 1_000_000.0
    );

    // Envio da resposta
    ctx.response()
      .putHeader("content-type", "application/json")
      .end(responseBody);
  }
}
