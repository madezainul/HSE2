package ahqpck.hse.safety.repositories;

import static org.assertj.core.api.Assertions.assertThat;

import ahqpck.hse.safety.model.entity.HseCertification;
import ahqpck.hse.safety.model.entity.HseExternalTraining;
import ahqpck.hse.safety.model.entity.HseInduction;
import ahqpck.hse.safety.model.entity.HseInternalTraining;
import ahqpck.hse.safety.repository.HseCertificationRepository;
import ahqpck.hse.safety.repository.HseExternalTrainingRepository;
import ahqpck.hse.safety.repository.HseInductionRepository;
import ahqpck.hse.safety.repository.HseInternalTrainingRepository;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("h2test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class CodeNumberQueryTest {

    @Autowired private HseInductionRepository inductionRepo;
    @Autowired private HseInternalTrainingRepository internalRepo;
    @Autowired private HseExternalTrainingRepository externalRepo;
    @Autowired private HseCertificationRepository certRepo;

    private void induction(String code) {
        inductionRepo.saveAndFlush(HseInduction.builder()
                .code(code).traineeId("T1").inductionDate(LocalDate.of(2025, 1, 1)).build());
    }

    private void internal(String code) {
        internalRepo.saveAndFlush(HseInternalTraining.builder()
                .code(code).employeeId("E1").trainingName("Fire").trainerName("Tr")
                .trainingDate(LocalDate.of(2025, 1, 1)).build());
    }

    private void external(String code) {
        externalRepo.saveAndFlush(HseExternalTraining.builder()
                .code(code).employeeId("E1").trainingName("Crane").provider("Prov")
                .trainingDate(LocalDate.of(2025, 1, 1)).build());
    }

    private void cert(String code) {
        certRepo.saveAndFlush(HseCertification.builder()
                .code(code).employeeId("E1").certificateName("Forklift")
                .issuedDate(LocalDate.of(2025, 1, 1)).build());
    }

    // ---- Induction ----

    @Test
    void induction_emptyTable_returnsNull() {
        assertThat(inductionRepo.getMaxCodeNumber()).isNull();
    }

    @Test
    void induction_singleCode() {
        induction("IND-000007");
        assertThat(inductionRepo.getMaxCodeNumber()).isEqualTo(7);
    }

    @Test
    void induction_returnsNumericMaxNotLexicalMax() {
        induction("IND-000009");
        induction("IND-000010");
        induction("IND-000002");
        assertThat(inductionRepo.getMaxCodeNumber()).isEqualTo(10);
    }

    @Test
    void induction_ignoresCodesWithOtherPrefix() {
        induction("IND-000003");
        induction("XYZ-000099");
        assertThat(inductionRepo.getMaxCodeNumber()).isEqualTo(3);
    }

    @Test
    void induction_numberLongerThanSixDigits() {
        induction("IND-1234567");
        induction("IND-000005");
        assertThat(inductionRepo.getMaxCodeNumber()).isEqualTo(1234567);
    }

    @Test
    void induction_findByCode() {
        induction("IND-000001");
        assertThat(inductionRepo.findByCode("IND-000001")).isNotNull();
        assertThat(inductionRepo.findByCode("IND-000002")).isNull();
    }

    // ---- Internal training ----

    @Test
    void internal_emptyTable_returnsNull() {
        assertThat(internalRepo.getMaxCodeNumber()).isNull();
    }

    @Test
    void internal_singleCode_offsetIsCorrect() {
        internal("INT-TRAIN-000012");
        assertThat(internalRepo.getMaxCodeNumber()).isEqualTo(12);
    }

    @Test
    void internal_severalRows_returnsHighest() {
        internal("INT-TRAIN-000001");
        internal("INT-TRAIN-000012");
        internal("INT-TRAIN-000009");
        assertThat(internalRepo.getMaxCodeNumber()).isEqualTo(12);
    }

    @Test
    void internal_ignoresOtherPrefix() {
        internal("INT-TRAIN-000004");
        internal("EXT-TRAIN-000099");
        assertThat(internalRepo.getMaxCodeNumber()).isEqualTo(4);
    }

    @Test
    void internal_findByCode() {
        internal("INT-TRAIN-000001");
        assertThat(internalRepo.findByCode("INT-TRAIN-000001")).isNotNull();
        assertThat(internalRepo.findByCode("nope")).isNull();
    }

    // ---- External training ----

    @Test
    void external_emptyTable_returnsNull() {
        assertThat(externalRepo.getMaxCodeNumber()).isNull();
    }

    @Test
    void external_singleCode_offsetIsCorrect() {
        external("EXT-TRAIN-000003");
        assertThat(externalRepo.getMaxCodeNumber()).isEqualTo(3);
    }

    @Test
    void external_severalRows_returnsHighest() {
        external("EXT-TRAIN-000003");
        external("EXT-TRAIN-000100");
        external("EXT-TRAIN-000020");
        assertThat(externalRepo.getMaxCodeNumber()).isEqualTo(100);
    }

    @Test
    void external_ignoresOtherPrefix() {
        external("EXT-TRAIN-000002");
        external("INT-TRAIN-000050");
        assertThat(externalRepo.getMaxCodeNumber()).isEqualTo(2);
    }

    @Test
    void external_prePersistGeneratesStringId() {
        external("EXT-TRAIN-000001");
        assertThat(externalRepo.findByCode("EXT-TRAIN-000001").getId()).isNotBlank();
    }

    // ---- Certification ----

    @Test
    void cert_emptyTable_returnsNull() {
        assertThat(certRepo.getMaxCodeNumber()).isNull();
    }

    @Test
    void cert_singleCode_offsetIsCorrect() {
        cert("CERT-000009");
        assertThat(certRepo.getMaxCodeNumber()).isEqualTo(9);
    }

    @Test
    void cert_severalRows_returnsHighest() {
        cert("CERT-000009");
        cert("CERT-000011");
        cert("CERT-000010");
        assertThat(certRepo.getMaxCodeNumber()).isEqualTo(11);
    }

    @Test
    void cert_ignoresOtherPrefix() {
        cert("CERT-000001");
        cert("OTHER-000500");
        assertThat(certRepo.getMaxCodeNumber()).isEqualTo(1);
    }

    @Test
    void cert_findByCode() {
        cert("CERT-000001");
        assertThat(certRepo.findByCode("CERT-000001")).isNotNull();
        assertThat(certRepo.findByCode("CERT-000002")).isNull();
    }

    // ---- generateCode round trip: next code derived from max is not already taken ----

    @Test
    void nextCodeFromMax_isUnusedForEachRepository() {
        induction("IND-000007");
        internal("INT-TRAIN-000012");
        external("EXT-TRAIN-000003");
        cert("CERT-000009");

        assertThat(inductionRepo.findByCode(String.format("IND-%06d", inductionRepo.getMaxCodeNumber() + 1))).isNull();
        assertThat(internalRepo.findByCode(String.format("INT-TRAIN-%06d", internalRepo.getMaxCodeNumber() + 1))).isNull();
        assertThat(externalRepo.findByCode(String.format("EXT-TRAIN-%06d", externalRepo.getMaxCodeNumber() + 1))).isNull();
        assertThat(certRepo.findByCode(String.format("CERT-%06d", certRepo.getMaxCodeNumber() + 1))).isNull();
    }
}
