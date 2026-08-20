package org.example.charging;

import org.example.charging.pages.ChargingConfirmationPage;
import org.example.charging.pages.StationConnectorWizardPage;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ChargingWizardTest extends ChargingTestBase {

    @Test
    @DisplayName("CHG-WIZ-01: выбор коннектора открывает карусель выбора объёма, «Далее» доступна")
    void selectingConnector_opensVolumeWizard() {
        loginAsValidUser();
        StationConnectorWizardPage wizard = openStationWizard();

        Assertions.assertTrue(wizard.isNextButtonEnabled(),
                "Кнопка «Далее» должна быть доступна сразу после выбора коннектора "
                        + "(объём «Полный бак» уже выбран по умолчанию)");
    }

    @Test
    @DisplayName("CHG-WIZ-02: «Далее» с объёмом по умолчанию открывает экран подтверждения «Начните зарядку»")
    void clickingNext_withDefaultVolume_opensConfirmationScreen() {
        loginAsValidUser();
        StationConnectorWizardPage wizard = openStationWizard();

        ChargingConfirmationPage confirmation = wizard.clickNext();

        Assertions.assertTrue(confirmation.isLoaded(), "Экран подтверждения не открылся");
        Assertions.assertTrue(confirmation.getPageBodyText().contains(ChargingTestConfig.VOLUME_FULL_TANK),
                "Ожидался режим «" + ChargingTestConfig.VOLUME_FULL_TANK + "» на экране подтверждения (по умолчанию)");
    }

    @Test
    @DisplayName("CHG-WIZ-03: выбор «Зарядить на 80%» в карусели отражается на экране подтверждения")
    void selectingEightyPercentVolume_reflectsOnConfirmationScreen() {
        loginAsValidUser();
        StationConnectorWizardPage wizard = openStationWizard();

        wizard.carousel().selectByTestId(ChargingTestConfig.VOLUME_CARD_TESTID_80_PERCENT);
        ChargingConfirmationPage confirmation = wizard.clickNext();

        Assertions.assertTrue(confirmation.isLoaded());
        Assertions.assertTrue(confirmation.getPageBodyText().contains("80%"),
                "Ожидался режим «80%» на экране подтверждения после выбора «"
                        + ChargingTestConfig.VOLUME_80_PERCENT + "» в карусели");
    }
}
