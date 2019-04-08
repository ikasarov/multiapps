package com.sap.cloud.lm.sl.mta.resolvers.v2;

import java.io.InputStream;
import java.util.Collections;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.sap.cloud.lm.sl.common.util.TestUtil;
import com.sap.cloud.lm.sl.common.util.TestUtil.Expectation;
import com.sap.cloud.lm.sl.mta.handlers.DescriptorParserFacade;
import com.sap.cloud.lm.sl.mta.model.DeploymentDescriptor;
import com.sap.cloud.lm.sl.mta.resolvers.ResolverBuilder;

public class DescriptorPlaceholderResolverTest {

    protected static final String SYSTEM_PARAMETERS_LOCATION = "system-parameters.json";

    protected DescriptorPlaceholderResolver resolver;

    public static Stream<Arguments> testResolve() {
        return Stream.of(
// @formatter:off            // (00)
            Arguments.of(
                "mtad-with-placeholders-in-requires-dependency.yaml", new Expectation(Expectation.Type.RESOURCE, "result-from-placeholders-in-requires-dependency.json")
            ),
            // (01)
            Arguments.of(
                "mtad-with-placeholders-in-provides-dependency.yaml", new Expectation(Expectation.Type.RESOURCE, "result-from-placeholders-in-provides-dependency.json")
            ),
            // (02)
            Arguments.of(
                "mtad-with-placeholders-in-resource.yaml", new Expectation(Expectation.Type.RESOURCE, "result-from-placeholders-in-resource.json")
            ),
            // (03)
            Arguments.of(
                "mtad-with-placeholders-in-module.yaml", new Expectation(Expectation.Type.RESOURCE, "result-from-placeholders-in-module.json")
            ),
            // (04)
            Arguments.of(
                "mtad-with-placeholders-in-general-parameters.yaml", new Expectation(Expectation.Type.RESOURCE, "result-from-placeholders-in-general-parameters.json")
            ),
            // (05)
            Arguments.of(
                "mtad-with-concatenated-placeholders.yaml", new Expectation(Expectation.Type.RESOURCE, "result-from-concatenated-parameters.json")
            ),
            // (06)
            Arguments.of(
                "mtad-with-unresolvable-requires-dependency-properties.yaml", new Expectation(Expectation.Type.EXCEPTION, "Unable to resolve \"foo#bar#non-existing\"")
            ),
            // (07)
            Arguments.of(
                "mtad-with-unresolvable-requires-dependency-parameters.yaml", new Expectation(Expectation.Type.EXCEPTION, "Unable to resolve \"foo#bar#non-existing\"")
            ),
            // (08)
            Arguments.of(
                "mtad-with-unresolvable-general-parameters.yaml", new Expectation(Expectation.Type.EXCEPTION, "Unable to resolve \"non-existing\"")
            ),
            // (09)
            Arguments.of(
                "mtad-with-unresolvable-module-properties.yaml", new Expectation(Expectation.Type.EXCEPTION, "Unable to resolve \"foo#non-existing\"")
            ),
            // (10)
            Arguments.of(
                "mtad-with-unresolvable-module-parameters.yaml", new Expectation(Expectation.Type.EXCEPTION, "Unable to resolve \"foo#non-existing\"")
            ),
            // (11)
            Arguments.of(
                "mtad-preservation-of-types.yaml", new Expectation(Expectation.Type.RESOURCE, "result-preservation-of-types.json")
            ),
            // (12)
            Arguments.of(
                "mtad-with-placeholder-in-a-nested-structure.yaml", new Expectation(Expectation.Type.RESOURCE, "result-from-placeholder-in-a-nested-structure.json")
            ),
            // (13)
            Arguments.of(
                "mtad-with-circular-reference-in-requires-dependency.yaml", new Expectation(Expectation.Type.EXCEPTION, "Circular reference detected in \"foo#bar#a\"")
            ),
            // (14)
            Arguments.of(
                "mtad-with-circular-reference-in-resource.yaml", new Expectation(Expectation.Type.EXCEPTION, "Circular reference detected in \"bar#a\"")
            ),
            // (15)
            Arguments.of(
                "mtad-with-circular-reference-in-module.yaml", new Expectation(Expectation.Type.EXCEPTION, "Circular reference detected in \"foo#a\"")
            ),
            // (16)
            Arguments.of(
                "mtad-with-complex-circular-reference.yaml", new Expectation(Expectation.Type.EXCEPTION, "Circular reference detected in \"a\"")
            ),
            // (17)
            Arguments.of(
                "mtad-with-repeating-placeholder.yaml", new Expectation(Expectation.Type.RESOURCE, "result-from-repeating-placeholder.json")
            ),
            // (19)
            Arguments.of(
                "mtad-with-placeholders-with-depth.yaml", new Expectation(Expectation.Type.RESOURCE, "result-from-placeholders-with-depth.json")
            )
// @formatter:on
        );
    }

    public void init(String descriptorLocation) {
        DeploymentDescriptor deploymentDescriptor = parseDeploymentDescriptor(descriptorLocation);
        this.resolver = createDescriptorPlaceholderResolver(deploymentDescriptor);
    }
        
    @ParameterizedTest(name = "{index}: \"{1}.\"")
    @MethodSource
    public void testResolve(String descriptorLocation, Expectation expectation) {
        init(descriptorLocation);

        TestUtil.test(() -> resolver.resolve(), expectation, getClass());
    }

    protected DeploymentDescriptor parseDeploymentDescriptor(String descriptorLocation) {
        DescriptorParserFacade parser = new DescriptorParserFacade();
        InputStream descriptor = getClass().getResourceAsStream(descriptorLocation);
        return parser.parseDeploymentDescriptor(descriptor);
    }

    protected DescriptorPlaceholderResolver createDescriptorPlaceholderResolver(DeploymentDescriptor deploymentDescriptor) {
        return new DescriptorPlaceholderResolver(deploymentDescriptor, new ResolverBuilder(), new ResolverBuilder(),
            Collections.emptyMap());
    }
}
