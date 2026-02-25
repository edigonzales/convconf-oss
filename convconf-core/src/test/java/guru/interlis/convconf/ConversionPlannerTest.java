package guru.interlis.convconf;

import guru.interlis.convconf.api.ConvConfService;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class ConversionPlannerTest {
    @Test
    void buildsPlan() throws Exception {
        var plan = new ConvConfService().plan(
                Path.of("../examples/h2-to-h2/km/verein.ili"),
                Path.of("../examples/h2-to-h2/lm/source.lm"),
                Path.of("../examples/h2-to-h2/lm/target.lm")
        );
        assertThat(plan.sourceModel()).isNotNull();
        assertThat(plan.targetModel()).isNotNull();
        assertThat(plan.steps()).isNotEmpty();
        assertThat(plan.steps().stream().map(s -> s.phase()).toList()).contains("READ", "WRITE");
    }
}
