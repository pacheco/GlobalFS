package ch.usi.paxosfs.client.jmeter;

import org.apache.jmeter.config.Arguments;
import org.apache.jmeter.protocol.java.sampler.AbstractJavaSamplerClient;
import org.apache.jmeter.protocol.java.sampler.JavaSamplerContext;
import org.apache.jmeter.samplers.SampleResult;

/**
 * Created by pacheco on 10/9/15.
 */
public class FSOperSampler extends AbstractJavaSamplerClient {
    // set up default arguments for the JMeter GUI
    @Override
    public Arguments getDefaultParameters() {
        Arguments defaultParameters = new Arguments();
        defaultParameters.addArgument("ARGFOO", "default value");
        return defaultParameters;
    }

    @Override
    public SampleResult runTest(JavaSamplerContext javaSamplerContext) {
        return null;
    }
}
