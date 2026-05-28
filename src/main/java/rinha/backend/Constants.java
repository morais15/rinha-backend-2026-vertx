package rinha.backend;

public final class Constants {

  private Constants() {
  }

  public static final float MAX_AMOUNT = 10_000f;
  public static final float MAX_INSTALLMENTS = 12f;
  public static final float AMOUNT_VS_AVG_RATIO = 10f;
  public static final float MAX_MINUTES = 1_440f;
  public static final float MAX_KM = 1_000f;
  public static final float MAX_TX_COUNT_24H = 20f;
  public static final float MAX_MERCHANT_AVG_AMOUNT = 10_000f;

  public static final float SHORT_SCALE = 32767f;

  public static float getMccRisk(String mcc) {
    return switch (mcc) {
      case "5411" -> 0.15f;
      case "5812" -> 0.30f;
      case "5912" -> 0.20f;
      case "5944" -> 0.45f;
      case "7801" -> 0.80f;
      case "7802" -> 0.75f;
      case "7995" -> 0.85f;
      case "4511" -> 0.35f;
      case "5311" -> 0.25f;
      case "5999" -> 0.50f;
      default -> 0.5f;
    };
  }
}
