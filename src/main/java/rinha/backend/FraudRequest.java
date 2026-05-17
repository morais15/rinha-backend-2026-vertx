package rinha.backend;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.ArrayList;
import java.util.List;

public record FraudRequest(
  String id,
  Transaction transaction,
  Customer customer,
  Merchant merchant,
  Terminal terminal,
  LastTransaction lastTransaction
) {

  public static FraudRequest parse(JsonObject body) {

    // transaction
    JsonObject transactionJson =
      body.getJsonObject("transaction");

    Transaction transaction = new Transaction(
      transactionJson.getDouble("amount"),
      transactionJson.getInteger("installments"),
      transactionJson.getString("requested_at")
    );

    // customer
    JsonObject customerJson =
      body.getJsonObject("customer");

    JsonArray knownMerchantsJson =
      customerJson.getJsonArray("known_merchants");

    List<String> knownMerchants =
      new ArrayList<>(knownMerchantsJson.size());

    for (Object merchant : knownMerchantsJson) {
      knownMerchants.add((String) merchant);
    }

    Customer customer = new Customer(
      customerJson.getDouble("avg_amount"),
      customerJson.getInteger("tx_count_24h"),
      knownMerchants
    );

    // merchant
    JsonObject merchantJson =
      body.getJsonObject("merchant");

    Merchant merchant = new Merchant(
      merchantJson.getString("id"),
      merchantJson.getString("mcc"),
      merchantJson.getDouble("avg_amount")
    );

    // terminal
    JsonObject terminalJson =
      body.getJsonObject("terminal");

    Terminal terminal = new Terminal(
      terminalJson.getBoolean("is_online"),
      terminalJson.getBoolean("card_present"),
      terminalJson.getDouble("km_from_home")
    );

    // last transaction
    JsonObject lastTransactionJson =
      body.getJsonObject("last_transaction");

    LastTransaction lastTransaction = null;

    if (lastTransactionJson != null) {
      lastTransaction = new LastTransaction(
        lastTransactionJson.getString("timestamp"),
        lastTransactionJson.getDouble("km_from_current")
      );
    }

    return new FraudRequest(
      body.getString("id"),
      transaction,
      customer,
      merchant,
      terminal,
      lastTransaction
    );
  }

  public record Transaction(
    double amount,
    int installments,
    String requestedAt
  ) {
  }

  public record Customer(
    double avgAmount,
    int txCount24h,
    List<String> knownMerchants
  ) {
  }

  public record Merchant(
    String id,
    String mcc,
    double avgAmount
  ) {
  }

  public record Terminal(
    boolean isOnline,
    boolean cardPresent,
    double kmFromHome
  ) {
  }

  public record LastTransaction(
    String timestamp,
    double kmFromCurrent
  ) {
  }
}
