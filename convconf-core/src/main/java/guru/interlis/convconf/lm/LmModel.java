package guru.interlis.convconf.lm;

import java.util.List;
import java.util.Map;

/** Parsed logical model with table mapping declarations. */
public record LmModel(
        String name,
        Map<String, Map<Integer, String>> valueMaps,
        List<DataDecl> dataDecls,
        List<InspectionDecl> inspections) {
}
