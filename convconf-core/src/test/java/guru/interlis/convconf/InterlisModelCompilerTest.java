package guru.interlis.convconf;

import guru.interlis.convconf.interlis.InterlisModelCompiler;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class InterlisModelCompilerTest {
    @Test
    void compilesModelAndExtractsClasses() {
        var result = new InterlisModelCompiler().compile(Path.of("../examples/h2-to-h2/km/verein.ili"));
        assertThat(result.transferDescription()).isNotNull();
        assertThat(result.kmSchema().classNames()).contains("Verein.Domain.Organisation", "Verein.Domain.Person", "Verein.Domain.Veranstaltung");
    }
}
