package org.example.charging;

public final class ChargingTestConfig {

    private ChargingTestConfig() {
    }

    public static final String STATION_ID = "49";
    public static final String STATION_DEEP_LINK_PATH = "station/" + STATION_ID;

    public static final String CONNECTOR_TEXT_FRAGMENT = "CCS2";

    public static final String VOLUME_FULL_TANK = "Полный бак";
    public static final String VOLUME_80_PERCENT = "Зарядить на 80%";
    public static final String VOLUME_MANUAL = "Данные вручную";
    public static final String PAYMENT_BALANCE = "Мой баланс";
    public static final String PAYMENT_CARD_SUFFIX = "•••• 0000";


    public static final String VOLUME_CARD_TESTID_FULL_TANK = "charge-volume-card-full-tank";
    public static final String VOLUME_CARD_TESTID_80_PERCENT = "charge-volume-card-80-percent";
    public static final String PAYMENT_CARD_TESTID_BALANCE = "payment-method-balance";
    public static final String PAYMENT_CARD_TESTID_CARD = "payment-method-card";


    public static final String VOLUME_CARD_TESTID_CUSTOM = "charge-volume-card-custom";

    public static final String CUSTOM_KWH_AMOUNT = "1";

    // до какого заряжать
    public static final int TARGET_CHARGE_PERCENT = 40;


    public static final String VOLUME_CARD_TESTID_RESERVE = "charge-volume-card-reserve";


    public static final String INSUFFICIENT_BALANCE_FOR_BOOKING_TEXT =
            "На внутреннем счете недостаточно средств для бронирования. Пополнить счет можно из "
                    + "раздела профиль или на экране выбора средств оплаты";


    public static final String BOOKING_ACTIVATE_BUTTON_TEXT = "Активировать";

    public static final String BOOKING_BANNER_LABEL = "Забронировано";

    public static final String BOOKING_ACTION_START_CHARGING = "Начать заправку";
    public static final String BOOKING_ACTION_OPEN_BARRIER = "Открыть шлагбаум";
    public static final String BOOKING_ACTION_BUILD_ROUTE = "Проложить маршрут";
    public static final String BOOKING_ACTION_CANCEL = "Отменить";

    public static final String BOOKING_CANCEL_DIALOG_LINE_1 = "Ваше бронирование будет отменено.";
    public static final String BOOKING_CANCEL_DIALOG_LINE_2 = "Желаете отменить бронирование?";
    public static final String BOOKING_CANCEL_CONFIRM_BUTTON = "Отменить бронирование";
    public static final String BOOKING_CANCEL_KEEP_BUTTON = "Сохранить бронь";

    // обратный отсчет 15 мин
    public static final int BOOKING_INITIAL_MINUTES = 15;

    public static final String REBOOKING_BLOCKED_TEXT = "Повторное бронирование будет доступно после выполнения зарядки";

    //плашка на главном экане
    public static final String ACTIVE_CHARGING_BANNER_LABEL = "Идет зарядка";

    /**
     * Текст, который карусель объёма показывает вместо обычных карточек, когда станция занята
     * ЧУЖОЙ активной зарядкой (не нашей бронью) - карусель схлопывается до одного варианта "Стать
     * в очередь" без собственного {@code charge-volume-card-*} testid, поэтому обычный
     * {@code selectByTestId(...)} падает с NoSuchElementException вместо понятной причины.
     * Подтверждено живой проверкой 2026-08-17.
     */
    public static final String STATION_OCCUPIED_TEXT = "Выбранная зарядная станция занята. Вы можете стать в очередь";
}
