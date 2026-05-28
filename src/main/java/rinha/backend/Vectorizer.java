package rinha.backend;

import java.time.Duration;
import java.time.OffsetDateTime;

import static rinha.backend.Constants.*;

public final class Vectorizer {

  private Vectorizer() {
  }

  public static short[] vectorize(
    FraudRequest request
  ) {

    final short[] v = new short[14];

    // 0 - amount
    v[0] = quantize(
      clamp(
        (float) request.transaction().amount()
          / MAX_AMOUNT
      )
    );

    // 1 - installments
    v[1] = quantize(
      clamp(
        (float) request.transaction().installments()
          / MAX_INSTALLMENTS
      )
    );

    // 2 - amount_vs_avg
    final double customerAvgAmount =
      request.customer().avgAmount();

    if (customerAvgAmount > 0) {

      v[2] = quantize(
        clamp(
          (float) (
            (request.transaction().amount()
              / customerAvgAmount)
              / AMOUNT_VS_AVG_RATIO
          )
        )
      );

    } else {
      v[2] = quantize(1.0f);
    }

    final OffsetDateTime requestedAt =
      OffsetDateTime.parse(
        request.transaction().requestedAt()
      );

    // 3 - hour_of_day
    v[3] = quantize(
      requestedAt.getHour() / 23.0f
    );

    // 4 - day_of_week
    v[4] = quantize(
      (requestedAt.getDayOfWeek().getValue() - 1)
        / 6.0f
    );

    // 5 - minutes_since_last_tx
    // 6 - km_from_last_tx

    final FraudRequest.LastTransaction lastTransaction =
      request.lastTransaction();

    if (lastTransaction == null) {

      v[5] = -1;
      v[6] = -1;

    } else {

      final OffsetDateTime lastTimestamp =
        OffsetDateTime.parse(
          lastTransaction.timestamp()
        );

      long minutes =
        Duration
          .between(lastTimestamp, requestedAt)
          .toMinutes();

      if (minutes < 0) {
        minutes = 0;
      }

      v[5] = quantize(
        clamp(
          (float) minutes / MAX_MINUTES
        )
      );

      v[6] = quantize(
        clamp(
          (float) lastTransaction.kmFromCurrent()
            / MAX_KM
        )
      );
    }

    // 7 - km_from_home
    v[7] = quantize(
      clamp(
        (float) request.terminal().kmFromHome()
          / MAX_KM
      )
    );

    // 8 - tx_count_24h
    v[8] = quantize(
      clamp(
        (float) request.customer().txCount24h()
          / MAX_TX_COUNT_24H
      )
    );

    // 9 - is_online
    v[9] =
      request.terminal().isOnline()
        ? (short) SHORT_SCALE
        : 0;

    // 10 - card_present
    v[10] =
      request.terminal().cardPresent()
        ? (short) SHORT_SCALE
        : 0;

    // 11 - unknown_merchant
    v[11] =
      isKnownMerchant(
        request.merchant().id(),
        request.customer().knownMerchants()
      )
        ? 0
        : (short) SHORT_SCALE;

    // 12 - mcc_risk
    v[12] = quantize(
      getMccRisk(request.merchant().mcc())
    );

    // 13 - merchant_avg_amount
    v[13] = quantize(
      clamp(
        (float) request.merchant().avgAmount()
          / MAX_MERCHANT_AVG_AMOUNT
      )
    );

    return v;
  }

  private static short quantize(float value) {

    return (short) (
      value * SHORT_SCALE
    );
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
