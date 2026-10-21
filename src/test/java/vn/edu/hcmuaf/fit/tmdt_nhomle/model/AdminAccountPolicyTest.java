package vn.edu.hcmuaf.fit.tmdt_nhomle.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

class AdminAccountPolicyTest {
    @Test
    void preventsSelfLock() {
        assertThrows(IllegalArgumentException.class,
                () -> AdminAccountPolicy.assertCanDisable(7, 7, 2));
    }

    @Test
    void preventsDisablingTheLastActiveAdmin() {
        assertThrows(IllegalArgumentException.class,
                () -> AdminAccountPolicy.assertCanDisable(9, 7, 1));
    }

    @Test
    void allowsDisablingAnotherAdminWhenOneRemains() {
        AdminAccountPolicy.assertCanDisable(9, 7, 2);
    }
}
