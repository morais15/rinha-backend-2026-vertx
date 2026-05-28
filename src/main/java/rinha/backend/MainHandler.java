package rinha.backend;

import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

import static rinha.backend.Vectorizer.vectorize;

public final class MainHandler {

  private final KnnSearch knnSearch;
  private final boolean ready;

  public MainHandler() {
    try {
      knnSearch = new KnnSearch(new ReferenceDataset());
      ready = true;
    } catch (Throwable e) {
      System.exit(-3);
      throw e;
    }
  }

  public void ready(RoutingContext ctx) {
    if (!ready) {
      ctx.response()
        .setStatusCode(503)
        .end();
      return;
    }

    ctx.response()
      .setStatusCode(200)
      .end();
  }

  public void fraudScore(RoutingContext ctx) {
    FraudRequest request = FraudRequest.parse(ctx.body().asJsonObject());

    short[] queryVector = vectorize(request);

    int fraudScore = knnSearch.search(queryVector);

    boolean approved = fraudScore < 3;

    JsonObject response = new JsonObject()
      .put("approved", approved)
      .put("fraud_score", (float) fraudScore);

    ctx.response()
      .putHeader("content-type", "application/json")
      .end(response.encode());
  }
}
