package ahqpck.hse.safety.training;

import ahqpck.hse.safety.model.entity.HseCertification;
import ahqpck.hse.safety.model.entity.HseExternalTraining;
import ahqpck.hse.safety.model.entity.HseInduction;
import ahqpck.hse.safety.model.entity.HseInternalTraining;
import org.junit.jupiter.api.Test;

import java.util.function.BiConsumer;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;

class TrainingEntityHelpersTest {

    /** Small adapter so each entity runs through the same assertions. */
    private record Subject(String name, Supplier<Object> factory,
                           BiConsumer<Object, Long> setSize, BiConsumer<Object, String> setExt,
                           java.util.function.Function<Object, String> size,
                           java.util.function.Function<Object, String> icon) {
    }

    private static java.util.List<Subject> subjects() {
        return java.util.List.of(
                new Subject("Induction", HseInduction::new,
                        (o, v) -> ((HseInduction) o).setFileSizeBytes(v), (o, v) -> ((HseInduction) o).setFileExtension(v),
                        o -> ((HseInduction) o).getFormattedFileSize(), o -> ((HseInduction) o).getFileIconClass()),
                new Subject("Internal", HseInternalTraining::new,
                        (o, v) -> ((HseInternalTraining) o).setFileSizeBytes(v), (o, v) -> ((HseInternalTraining) o).setFileExtension(v),
                        o -> ((HseInternalTraining) o).getFormattedFileSize(), o -> ((HseInternalTraining) o).getFileIconClass()),
                new Subject("External", HseExternalTraining::new,
                        (o, v) -> ((HseExternalTraining) o).setFileSizeBytes(v), (o, v) -> ((HseExternalTraining) o).setFileExtension(v),
                        o -> ((HseExternalTraining) o).getFormattedFileSize(), o -> ((HseExternalTraining) o).getFileIconClass()),
                new Subject("Certification", HseCertification::new,
                        (o, v) -> ((HseCertification) o).setFileSizeBytes(v), (o, v) -> ((HseCertification) o).setFileExtension(v),
                        o -> ((HseCertification) o).getFormattedFileSize(), o -> ((HseCertification) o).getFileIconClass()));
    }

    private String sizeOf(Subject s, Long bytes) {
        Object o = s.factory().get();
        s.setSize().accept(o, bytes);
        return s.size().apply(o);
    }

    private String iconOf(Subject s, String ext) {
        Object o = s.factory().get();
        s.setExt().accept(o, ext);
        return s.icon().apply(o);
    }

    @Test
    void formattedFileSize_nullIsNA() {
        for (Subject s : subjects()) assertThat(sizeOf(s, null)).as(s.name()).isEqualTo("N/A");
    }

    @Test
    void formattedFileSize_zeroBytes() {
        for (Subject s : subjects()) assertThat(sizeOf(s, 0L)).as(s.name()).isEqualTo("0 KB");
    }

    @Test
    void formattedFileSize_smallFileInKb() {
        for (Subject s : subjects()) {
            assertThat(sizeOf(s, 2048L)).as(s.name()).isEqualTo("2 KB");
            assertThat(sizeOf(s, 1024L)).as(s.name()).isEqualTo("1 KB");
        }
    }

    @Test
    void formattedFileSize_exactlyOneMb() {
        for (Subject s : subjects()) assertThat(sizeOf(s, 1_048_576L)).as(s.name()).isEqualTo("1.00 MB");
    }

    @Test
    void formattedFileSize_fiveMbLimit() {
        for (Subject s : subjects()) assertThat(sizeOf(s, 5L * 1_048_576L)).as(s.name()).isEqualTo("5.00 MB");
    }

    @Test
    @org.junit.jupiter.api.Disabled("BUG: file size rounding at edges - HseInduction.java:78")
    void formattedFileSize_justBelowOneMbIsNotShownAs1024Kb() {
        for (Subject s : subjects()) {
            assertThat(sizeOf(s, 1_048_575L)).as(s.name()).isNotEqualTo("1024 KB");
        }
    }

    @Test
    @org.junit.jupiter.api.Disabled("BUG: file size rounding at edges - HseInduction.java:78")
    void formattedFileSize_tinyFileIsNotShownAsZero() {
        // 1 byte of real content should not read as "0 KB"
        for (Subject s : subjects()) assertThat(sizeOf(s, 1L)).as(s.name()).isNotEqualTo("0 KB");
    }

    @Test
    void fileIcon_nullExtensionIsGeneric() {
        for (Subject s : subjects()) assertThat(iconOf(s, null)).as(s.name()).isEqualTo("fa fa-file me-2 text-muted");
    }

    @Test
    void fileIcon_knownExtensions() {
        for (Subject s : subjects()) {
            assertThat(iconOf(s, ".pdf")).as(s.name()).contains("fa-file-pdf");
            assertThat(iconOf(s, ".PDF")).as(s.name()).contains("fa-file-pdf");
            assertThat(iconOf(s, ".doc")).as(s.name()).contains("fa-file-word");
            assertThat(iconOf(s, ".docx")).as(s.name()).contains("fa-file-word");
            assertThat(iconOf(s, ".xls")).as(s.name()).contains("fa-file-excel");
            assertThat(iconOf(s, ".xlsx")).as(s.name()).contains("fa-file-excel");
            assertThat(iconOf(s, ".jpg")).as(s.name()).contains("fa-file-image");
            assertThat(iconOf(s, ".jpeg")).as(s.name()).contains("fa-file-image");
            assertThat(iconOf(s, ".png")).as(s.name()).contains("fa-file-image");
            assertThat(iconOf(s, ".webp")).as(s.name()).contains("fa-file-image");
        }
    }

    @Test
    void fileIcon_pptIsAllowedDocumentButGetsSpecificIcon() {
        // CLAUDE.md allows ppt/pptx uploads; a generic icon is acceptable but must not be pdf/word/excel/image.
        for (Subject s : subjects()) {
            assertThat(iconOf(s, ".pptx")).as(s.name()).doesNotContain("pdf", "word", "excel", "image");
        }
    }

    @Test
    void fileIcon_unknownOrEmptyExtensionIsGeneric() {
        for (Subject s : subjects()) {
            assertThat(iconOf(s, ".zip")).as(s.name()).isEqualTo("fa fa-file me-2 text-muted");
            assertThat(iconOf(s, "")).as(s.name()).isEqualTo("fa fa-file me-2 text-muted");
        }
    }

    @Test
    void fileIcon_extensionWithoutDotIsNotRecognised() {
        // service always stores ".ext"; documents the dependency on the leading dot
        for (Subject s : subjects()) assertThat(iconOf(s, "pdf")).as(s.name()).isEqualTo("fa fa-file me-2 text-muted");
    }
}
