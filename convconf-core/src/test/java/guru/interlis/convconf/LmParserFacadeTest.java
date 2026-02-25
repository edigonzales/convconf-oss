package guru.interlis.convconf;

import guru.interlis.convconf.lm.LmParserFacade;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class LmParserFacadeTest {
    @Test
    void parsesSourceLm() throws Exception {
        var lm = new LmParserFacade().parse(Path.of("../examples/h2-to-h2/lm/source.lm"));
        assertThat(lm.name()).isEqualTo("Source");
        assertThat(lm.valueMaps()).containsKey("StructAttrMap");
        assertThat(lm.inspections()).hasSize(1);
    }
}
