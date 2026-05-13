package rinha.backend;

import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

public class MainHandler {

  public void ready(RoutingContext ctx) {
    ctx.response()
      .setStatusCode(200)
      .end();
  }

  public void fraudScore(RoutingContext ctx) {

    var request = FraudRequest.parse(ctx.body().asJsonObject());

    double fraudScore = 0.0;//todo

    boolean approved =
      fraudScore < 0.7;

    JsonObject response = new JsonObject()
      .put("approved", approved)
      .put("fraud_score", fraudScore);

    ctx.response()
      .putHeader("content-type", "application/json")
      .end(response.encode());
  }
}
