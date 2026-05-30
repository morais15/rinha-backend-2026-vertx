package rinha.backend;

import java.time.Duration;
import java.time.OffsetDateTime;

import static rinha.backend.Constants.*;

public final class Vectorizer {

  private Vectorizer() {
  }

  public static float[] vectorize(FraudRequest request) {
    float[] v = new float[14];

    // 0 - amount
    v[0] = clamp(
      (float) request.transaction().amount() / MAX_AMOUNT
    );

    // 1 - installments
    v[1] = clamp(
      (float) request.transaction().installments() / MAX_INSTALLMENTS
    );

    // 2 - amount_vs_avg
    double customerAvgAmount =
      request.customer().avgAmount();

    if (customerAvgAmount > 0) {
      v[2] = clamp(
        (float) (
          (request.transaction().amount() / customerAvgAmount)
            / AMOUNT_VS_AVG_RATIO
        )
      );
    } else {
      v[2] = 1.0f;
    }

    OffsetDateTime requestedAt =
      OffsetDateTime.parse(
        request.transaction().requestedAt()
      );

    // 3 - hour_of_day
    v[3] =
      requestedAt.getHour() / 23.0f;

    // 4 - day_of_week (segunda=0 ... domingo=6)
    v[4] =
      (requestedAt.getDayOfWeek().getValue() - 1) / 6.0f;

    // 5 - minutes_since_last_tx
    // 6 - km_from_last_tx
    FraudRequest.LastTransaction lastTransaction =
      request.lastTransaction();

    if (lastTransaction == null) {
      v[5] = -1.0f;
      v[6] = -1.0f;
    } else {
      OffsetDateTime lastTimestamp =
        OffsetDateTime.parse(
          lastTransaction.timestamp()
        );

      long minutes =
        Duration.between(lastTimestamp, requestedAt)
          .toMinutes();

      if (minutes < 0) {
        minutes = 0;
      }

      v[5] = clamp(
        (float) minutes / MAX_MINUTES
      );

      v[6] = clamp(
        (float) lastTransaction.kmFromCurrent() / MAX_KM
      );
    }

    // 7 - km_from_home
    v[7] = clamp(
      (float) request.terminal().kmFromHome() / MAX_KM
    );

    // 8 - tx_count_24h
    v[8] = clamp(
      (float) request.customer().txCount24h() / MAX_TX_COUNT_24H
    );

    // 9 - is_online
    v[9] =
      request.terminal().isOnline() ? 1.0f : 0.0f;

    // 10 - card_present
    v[10] =
      request.terminal().cardPresent() ? 1.0f : 0.0f;

    // 11 - unknown_merchant
    v[11] =
      isKnownMerchant(
        request.merchant().id(),
        request.customer().knownMerchants()
      ) ? 0.0f : 1.0f;

    // 12 - mcc_risk
    v[12] = getMccRisk(request.merchant().mcc());

    // 13 - merchant_avg_amount
    v[13] = clamp(
      (float) request.merchant().avgAmount()
        / MAX_MERCHANT_AVG_AMOUNT
    );

    return v;
  }

  private static boolean isKnownMerchant(
    String merchantId,
    java.util.List<String> knownMerchants
  ) {
    if (knownMerchants == null) {
      return false;
    }

    for (String knownMerchant : knownMerchants) {
      if (merchantId.equals(knownMerchant)) {
        return true;
      }
    }

    return false;
  }

  private static float clamp(float value) {
    if (value < 0.0f) {
      return 0.0f;
    }

    if (value > 1.0f) {
      return 1.0f;
    }

    return value;
  }
}
