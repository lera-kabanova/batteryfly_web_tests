package org.example.charging;

/**
 * Конфигурация тестового окружения модуля Charging (qa-discovery/test-modules.md, модуль 8).
 * Значения подтверждены живой проверкой 2026-07-16 через
 * tools/playwright-codegen/explore-charging-wizard-safe.js и explore-charging-next-step.js.
 */
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

    /**
     * data-testid карточек в каруселях "Объём зарядки" / "Способ оплаты" (подтверждено живой
     * проверкой tools/playwright-codegen/explore-full-testid-dump.js, 2026-07-22) — стабильнее,
     * чем поиск по видимому тексту: сайт в остальном практически не имеет data-testid
     * (см. qa-discovery/locators.md), но именно на этом экране они внезапно есть.
     */
    public static final String VOLUME_CARD_TESTID_FULL_TANK = "charge-volume-card-full-tank";
    public static final String VOLUME_CARD_TESTID_80_PERCENT = "charge-volume-card-80-percent";
    public static final String PAYMENT_CARD_TESTID_BALANCE = "payment-method-balance";
    public static final String PAYMENT_CARD_TESTID_CARD = "payment-method-card";

    /** Процент, до которого длится зарядка*/
    public static final int TARGET_CHARGE_PERCENT = 40;

    /**
     * Бронирование/очередь — ОДНА И ТА ЖЕ карточка карусели объёма зарядки (не отдельный флоу):
     * подтверждено живой проверкой tools/playwright-codegen/explore-booking-reserve-flow.js,
     * 2026-07-24. Если станция СВОБОДНА, карточка показывает "Забронировать"; если станция
     * ЗАНЯТА - она же показывает "Стать в очередь" с тем же условием "15 мин. - бесплатно,
     * далее 1 мин - 0.1 BYN" (т.е. после подхода очереди у пользователя есть 15 бесплатных минут
     * на старт зарядки, дальше - платно). Модуль Booking/Reservation и Queue поэтому не разделены
     * в продукте - решение, в каком Java-модуле разместить тесты, принимается отдельно.
     */
    public static final String VOLUME_CARD_TESTID_RESERVE = "charge-volume-card-reserve";

    /**
     * КОГДА СТАНЦИЯ ЗАНЯТА АКТИВНОЙ ЗАРЯДКОЙ (не просто чужой бронью), карусель объёма
     * схлопывается до ОДНОГО варианта ("Стать в очередь") без собственного
     * {@code charge-volume-card-*} testid у самой карточки - остаётся только пагинационная точка
     * {@code charge-volume-dot-0} (8px, НЕ кликабельная карточка). Подтверждено живой проверкой
     * 2026-07-24 (QueueTest, станция занята реальной сессией lerakab5): единственный вариант уже
     * активен по умолчанию, отдельный выбор карточки не нужен - сразу жать "Далее". Если станция
     * занята ЧУЖОЙ БРОНЬЮ (не зарядкой) - полный набор карточек, включая
     * {@link #VOLUME_CARD_TESTID_RESERVE}, остаётся на месте (см. explore-check-join-queue-screen.js).
     */

    /** Точный текст блокирующего диалога при попытке брони/очереди с нулевым внутренним балансом. */
    public static final String INSUFFICIENT_BALANCE_FOR_BOOKING_TEXT =
            "На внутреннем счете недостаточно средств для бронирования. Пополнить счет можно из "
                    + "раздела профиль или на экране выбора средств оплаты";

    /**
     * Найдено живой проверкой tools/playwright-codegen/explore-booking-happy-path.js +
     * BookingExplorationTest, 2026-07-24 (аккаунт cinemawebwelcome@gmail.com, баланс 4.42 BYN,
     * станция #49 свободна). Экран подтверждения брони - ОТДЕЛЬНЫЙ от обычной зарядки
     * ("Бронирование" вместо "Начните зарядку", кнопка "Активировать" вместо
     * "Оплатить и зарядить", "Стоимость: 0 BYN").
     */
    public static final String BOOKING_ACTIVATE_BUTTON_TEXT = "Активировать";

    /** Лейбл плашки активной брони на главном экране (рядом с обратным отсчётом HH:MM). */
    public static final String BOOKING_BANNER_LABEL = "Забронировано";

    /** Заголовок и пункты раскрытого меню действий плашки брони ("Действие"). */
    public static final String BOOKING_ACTION_START_CHARGING = "Начать заправку";
    public static final String BOOKING_ACTION_OPEN_BARRIER = "Открыть шлагбаум";
    public static final String BOOKING_ACTION_BUILD_ROUTE = "Проложить маршрут";
    public static final String BOOKING_ACTION_CANCEL = "Отменить";

    /** Точный текст диалога подтверждения отмены брони (2 строки) + текст обеих кнопок выбора. */
    public static final String BOOKING_CANCEL_DIALOG_LINE_1 = "Ваше бронирование будет отменено.";
    public static final String BOOKING_CANCEL_DIALOG_LINE_2 = "Желаете отменить бронирование?";
    public static final String BOOKING_CANCEL_CONFIRM_BUTTON = "Отменить бронирование";
    public static final String BOOKING_CANCEL_KEEP_BUTTON = "Сохранить бронь";

    /** Стартовое значение обратного отсчёта брони - 15 минут (может быть уже 14:5x к моменту чтения). */
    public static final int BOOKING_INITIAL_MINUTES = 15;

    /**
     * Точный текст блокирующего диалога при попытке повторной брони ПОСЛЕ отмены предыдущей
     * брони БЕЗ выполненной зарядки (сценарий 15) — найдено случайно живой диагностикой
     * {@code BookingDiagnosticTest}, 2026-07-24: аккаунт cinemawebwelcome@gmail.com был
     * заблокирован от повторной брони после того, как ранее забронировал и отменил бронь
     * (не зарядившись) в ходе разведки {@code BookingExplorationTest}. Подтверждает бизнес-правило
     * "повторное бронирование доступно только после выполнения зарядки" на реальных данных.
     */
    public static final String REBOOKING_BLOCKED_TEXT = "Повторное бронирование будет доступно после выполнения зарядки";

    /**
     * Плашка активной зарядки на главном экране — точный текст подтверждён живой диагностикой
     * 2026-07-24: "33 % | Идет зарядка | 58 kW", ТРИ отдельных элемента в одной строке (БЕЗ "ё"
     * в "Идет", не "Идёт зарядка", как ошибочно предполагали qa-discovery/ui-elements.md и
     * ui-inventory.md по неподтверждённым наблюдениям).
     */
    public static final String ACTIVE_CHARGING_BANNER_LABEL = "Идет зарядка";
}
