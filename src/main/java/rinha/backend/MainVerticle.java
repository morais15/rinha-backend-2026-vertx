package rinha.backend;

import io.vertx.core.Future;
import io.vertx.core.VerticleBase;
import io.vertx.core.Vertx;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;

public class MainVerticle extends VerticleBase {

  void main() {
    Vertx.vertx()
      .deployVerticle(new MainVerticle());
  }

  @Override
  public Future<?> start() {

    Router router = Router.router(vertx);

    router.route().handler(BodyHandler.create());

    MainHandler mainHandler = new MainHandler();

    router.get("/ready").handler(mainHandler::ready);

    router.post("/fraud-score").handler(mainHandler::fraudScore);

    return vertx.createHttpServer()
      .requestHandler(router)
      .listen(8080)
      .onSuccess(server -> System.out.println("HTTP server started on port " + server.actualPort()));
  }
}
