package guru.interlis.convconf;

import guru.interlis.convconf.lm.LmParserFacade;
import guru.interlis.convconf.lm.MappingDirection;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class LmParserExtendedSyntaxTest {
    @Test
    void parsesExtendedConstructs() throws Exception {
        Path lm = Files.createTempFile("extended", ".lm");
        Files.writeString(lm, """
                LM Extended;
                VALUEMAP VM { 1 -> A.B; }
                DATA X FROM SRC CLASS Verein.Domain.Person {
                  DIRECTION <-;
                  IDENT ID;
                  CONVERSION VM(COL -- MAPPED);
                  ALIAS P ~ Verein.Domain.Person;
                  WITH P { COLUMN C1 -> Vorname; }
                  ANNEXE Verein.Domain.Mitglied;
                  ANNEXED Member;
                  JOIN LEFT Other ON ID = OID;
                  NESTING Child BY PID;
                  COLUMN ID -> Nummer;
                }
                """);
        var model = new LmParserFacade().parse(lm);
        var d = model.dataDecls().getFirst();
        assertThat(d.direction()).isEqualTo(MappingDirection.INPUT_ONLY);
        assertThat(d.conversions()).hasSize(1);
        assertThat(d.aliases()).hasSize(1);
        assertThat(d.withBlocks()).hasSize(1);
        assertThat(d.annexeTargets()).contains("Verein.Domain.Mitglied");
        assertThat(d.annexedSources()).contains("Member");
        assertThat(d.joins()).hasSize(1);
        assertThat(d.nestings()).hasSize(1);
    }
}
