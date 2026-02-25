package guru.interlis.convconf;

import guru.interlis.convconf.interlis.InterlisModelCompiler;
import guru.interlis.convconf.lm.LmParserFacade;
import guru.interlis.convconf.lm.LmSemanticChecker;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class LmSemanticCheckerTest {
    @Test
    void reportsUnknownAttribute() throws Exception {
        Path temp = Files.createTempFile("bad", ".lm");
        Files.writeString(temp, """
                LM Bad;
                DATA A FROM SRC_ORGANISATION CLASS Verein.Domain.Organisation {
                  IDENT ID;
                  COLUMN ID -> Nummer;
                  COLUMN NAME -> DoesNotExist;
                }
                """);
        var km = new InterlisModelCompiler().compile(Path.of("../examples/h2-to-h2/km/verein.ili")).kmSchema();
        var lm = new LmParserFacade().parse(temp);
        var errors = new LmSemanticChecker().check(lm, km);
        assertThat(errors).anyMatch(e -> e.contains("Unknown attribute"));
    }
}
