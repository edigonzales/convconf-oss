package guru.interlis.convconf.cli;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ConvertValidationTest {
    @Test
    void requiresJdbcForH2Source() {
        assertThatThrownBy(() -> ConvConfCli.Convert.validateEndpoint(
                "source",
                ConvConfCli.EndpointType.H2,
                new ConvConfCli.EndpointOptions(null, null, null, null, null)
        )).isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("--source-jdbc is required");
    }

    @Test
    void rejectsJdbcForCsvTarget() {
        assertThatThrownBy(() -> ConvConfCli.Convert.validateEndpoint(
                "target",
                ConvConfCli.EndpointType.CSV,
                new ConvConfCli.EndpointOptions("jdbc:h2:mem:x", null, null, java.nio.file.Path.of("out"), null)
        )).isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("--target-jdbc is not allowed");
    }

    @Test
    void acceptsPostgresToXlsxConfiguration() {
        ConvConfCli.Convert.validateEndpoint(
                "source",
                ConvConfCli.EndpointType.POSTGRES,
                new ConvConfCli.EndpointOptions("jdbc:postgresql://localhost/db", "u", "p", null, null)
        );
        ConvConfCli.Convert.validateEndpoint(
                "target",
                ConvConfCli.EndpointType.XLSX,
                new ConvConfCli.EndpointOptions(null, null, null, null, java.nio.file.Path.of("target.xlsx"))
        );
    }
}
