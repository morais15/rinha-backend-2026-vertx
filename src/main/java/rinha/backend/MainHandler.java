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
    var request = FraudRequest.parse(ctx.body().asJsonObject());

    float[] queryVector = vectorize(request);

    double fraudScore = knnSearch.search(queryVector);

    boolean approved = fraudScore < 0.6;

    JsonObject response = new JsonObject()
      .put("approved", approved)
      .put("fraud_score", fraudScore);

    ctx.response()
      .putHeader("content-type", "application/json")
      .end(response.encode());
  }
}
