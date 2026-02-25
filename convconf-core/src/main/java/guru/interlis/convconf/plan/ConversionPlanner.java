package guru.interlis.convconf.plan;

import guru.interlis.convconf.lm.DataDecl;
import guru.interlis.convconf.lm.LmModel;

import java.util.ArrayList;
import java.util.List;

/** Builds an execution plan (KK) from source/target LMs. */
public final class ConversionPlanner {
    public ConversionPlan build(String kmName, LmModel source, LmModel target) {
        List<ConversionPlan.PlanStep> steps = new ArrayList<>();
        for (DataDecl d : source.dataDecls()) {
            steps.add(new ConversionPlan.PlanStep("READ", d.sourceTable(), d.className(), "direction=" + d.direction()));
        }
        source.inspections().forEach(i -> steps.add(new ConversionPlan.PlanStep("READ", i.sourceTable(), i.className(), "inspection")));
        for (DataDecl d : target.dataDecls()) {
            steps.add(new ConversionPlan.PlanStep("WRITE", d.className(), d.sourceTable(), "direction=" + d.direction()));
        }
        return new ConversionPlan(kmName, source.name(), target.name(), source, target, steps);
    }
}
