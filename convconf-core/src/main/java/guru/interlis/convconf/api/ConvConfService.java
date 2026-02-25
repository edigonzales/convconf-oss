package guru.interlis.convconf.api;

import guru.interlis.convconf.interlis.InterlisModelCompiler;
import guru.interlis.convconf.lm.LmParserFacade;
import guru.interlis.convconf.lm.LmSemanticChecker;
import guru.interlis.convconf.plan.ConversionPlan;
import guru.interlis.convconf.plan.ConversionPlanner;
import guru.interlis.convconf.runtime.ConversionEngine;
import guru.interlis.convconf.runtime.RecordSourceReader;
import guru.interlis.convconf.runtime.RecordTargetWriter;
import guru.interlis.convconf.validate.KmResultValidator;
import guru.interlis.convconf.validate.LmDataValidator;

import java.nio.file.Path;
import java.sql.Connection;
import java.util.List;

/**
 * Public façade for library users.
 * <p>
 * A conversion run follows three conceptual stages:
 * </p>
 * <ol>
 *   <li>Compile KM and parse both LM files.</li>
 *   <li>Validate semantic/data consistency and create a conversion plan.</li>
 *   <li>Execute the plan through source/target adapters.</li>
 * </ol>
 */
public final class ConvConfService {
    private final InterlisModelCompiler interlisModelCompiler = new InterlisModelCompiler();
    private final LmParserFacade parser = new LmParserFacade();
    private final LmSemanticChecker checker = new LmSemanticChecker();
    private final ConversionPlanner planner = new ConversionPlanner();
    private final ConversionEngine conversionEngine = new ConversionEngine();
    private final LmDataValidator lmDataValidator = new LmDataValidator();
    private final KmResultValidator kmResultValidator = new KmResultValidator();

    /**
     * Validates a single LM file against a KM model.
     *
     * @param kmIli path to the INTERLIS KM model
     * @param lmFile path to the LM definition to validate
     * @return list of validation errors (empty if valid)
     * @throws Exception if KM compile or LM parse fails
     */
    public List<String> check(Path kmIli, Path lmFile) throws Exception {
        var km = interlisModelCompiler.compile(kmIli).kmSchema();
        var lm = parser.parse(lmFile);
        return checker.check(lm, km);
    }

    /**
     * Builds a validated conversion plan from KM + source LM + target LM.
     *
     * @param kmIli path to the INTERLIS KM model
     * @param sourceLm path to source LM
     * @param targetLm path to target LM
     * @return plan including execution steps and parsed LM models
     * @throws Exception if parsing/compilation fails or semantic checks fail
     */
    public ConversionPlan plan(Path kmIli, Path sourceLm, Path targetLm) throws Exception {
        var km = interlisModelCompiler.compile(kmIli).kmSchema();
        var sLm = parser.parse(sourceLm);
        var tLm = parser.parse(targetLm);
        var errors = checker.check(sLm, km);
        errors.addAll(checker.check(tLm, km));
        if (!errors.isEmpty()) {
            throw new IllegalArgumentException("LM validation failed: " + errors);
        }
        return planner.build(kmIli.getFileName().toString(), sLm, tLm);
    }

    /**
     * Executes a pre-built conversion plan.
     *
     * @param kmIli path to KM model used for final result validation
     * @param plan pre-built conversion plan
     * @param sourceReader adapter used to read source records
     * @param targetWriter adapter used to write target records
     * @param traceEnabled if {@code true}, runtime trace events are collected
     * @return conversion result with canonical records and optional trace events
     * @throws Exception if read/write fails or validation fails
     */
    public ConversionEngine.ConversionResult executePlan(Path kmIli,
                                                         ConversionPlan plan,
                                                         RecordSourceReader sourceReader,
                                                         RecordTargetWriter targetWriter,
                                                         boolean traceEnabled) throws Exception {
        var km = interlisModelCompiler.compile(kmIli).kmSchema();
        var errors = lmDataValidator.validate(sourceReader, plan.sourceModel());
        if (!errors.isEmpty()) {
            throw new IllegalArgumentException("Validation failed: " + errors);
        }
        var result = conversionEngine.convert(sourceReader, targetWriter, plan, traceEnabled);
        var kmErrors = kmResultValidator.validate(km, result.canonicalRecords());
        if (!kmErrors.isEmpty()) {
            throw new IllegalArgumentException("KM result validation failed: " + kmErrors);
        }
        return result;
    }

    /**
     * Full convert flow using LM paths (plan is built internally).
     */
    public ConversionEngine.ConversionResult convert(Path kmIli, Path sourceLm, Path targetLm,
                                                     RecordSourceReader sourceReader,
                                                     RecordTargetWriter targetWriter,
                                                     boolean traceEnabled) throws Exception {
        var conversionPlan = plan(kmIli, sourceLm, targetLm);
        return executePlan(kmIli, conversionPlan, sourceReader, targetWriter, traceEnabled);
    }

    /**
     * Full convert flow with trace collection disabled.
     */
    public ConversionEngine.ConversionResult convert(Path kmIli, Path sourceLm, Path targetLm,
                                                     RecordSourceReader sourceReader,
                                                     RecordTargetWriter targetWriter) throws Exception {
        return convert(kmIli, sourceLm, targetLm, sourceReader, targetWriter, false);
    }

    /**
     * Convenience overload that wraps JDBC connections with {@code JdbcRecordAdapter}.
     */
    public ConversionEngine.ConversionResult convert(Path kmIli, Path sourceLm, Path targetLm, Connection source, Connection target) throws Exception {
        return convert(kmIli, sourceLm, targetLm,
                new guru.interlis.convconf.runtime.JdbcRecordAdapter(source),
                new guru.interlis.convconf.runtime.JdbcRecordAdapter(target),
                false);
    }
}
