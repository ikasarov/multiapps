package com.sap.cloud.lm.sl.mta.resolvers;

import java.util.Arrays;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.sap.cloud.lm.sl.common.util.Callable;
import com.sap.cloud.lm.sl.common.util.TestUtil;
import com.sap.cloud.lm.sl.common.util.TestUtil.Expectation;

@RunWith(Parameterized.class)
public class PropertiesResolverTest {

    protected final Map<String, Object> moduleProperties;
    protected final String parameterEpression;
    protected final Expectation expectation;
    
    protected static final PropertiesResolver testResolver = new PropertiesResolver(null, null, null, "test-", false);
    
    @Parameters
    public static Iterable<Object[]> getParameters() {
        return Arrays.asList(new Object[][] {
            {
                "moduleProperties.yaml", "test-map/a-list/1", new Expectation("second-item"),
            },
            {
                "moduleProperties.yaml", "test-list/0/foo", new Expectation("@foo"),
            },
            {
                "moduleProperties.yaml", "test-list/0/foo/", new Expectation("@foo"),
            },
            {
                "moduleProperties.yaml", "test-list/1", new Expectation("{fizz=@fizz, buzz=@buzz}"),
            },
            {
                "moduleProperties.yaml", "test-list/1/buzz", new Expectation("@buzz"),
            },
            {
                "moduleProperties.yaml", "test-list/2", new Expectation("a string in list"),
            },
            {
                "moduleProperties.yaml", "hosts/0/", new Expectation("some host"),
            },
            {
                "moduleProperties.yaml", "hosts/1.0", new Expectation(Expectation.Type.EXCEPTION, "Unable to resolve \"test-#hosts/1.0\""),
            },
            {
                "moduleProperties.yaml", "test-list/10/foo/", new Expectation(Expectation.Type.EXCEPTION, "Unable to resolve \"test-#test-list/10/foo/\""),
            },
            {
                "moduleProperties.yaml", "test-list//foo/", new Expectation(Expectation.Type.EXCEPTION, "Unable to resolve \"test-#test-list//foo/\""),
            },
        });
    }
    
    public PropertiesResolverTest(String modulePropertiesLocation, String parameterExpression, Expectation expectation) {
        this.moduleProperties = TestUtil.getMap(modulePropertiesLocation, getClass());
        this.parameterEpression = parameterExpression;
        this.expectation = expectation;
    }
    
    @Test
    public void testResolve() {
        TestUtil.test(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                return testResolver.resolveInDepth(moduleProperties, parameterEpression);
            }
        }, expectation, getClass());
    }
    
}
