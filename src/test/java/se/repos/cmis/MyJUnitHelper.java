package se.repos.cmis;

import java.io.File;
import java.io.PrintWriter;

import org.apache.chemistry.opencmis.tck.CmisTest;
import org.apache.chemistry.opencmis.tck.CmisTestGroup;
import org.apache.chemistry.opencmis.tck.CmisTestProgressMonitor;
import org.apache.chemistry.opencmis.tck.CmisTestReport;
import org.apache.chemistry.opencmis.tck.CmisTestResult;
import org.apache.chemistry.opencmis.tck.CmisTestResultStatus;
import org.apache.chemistry.opencmis.tck.impl.WrapperCmisTestGroup;
import org.apache.chemistry.opencmis.tck.report.TextReport;
import org.apache.chemistry.opencmis.tck.runner.AbstractRunner;
import org.junit.Assert;

/**
 * A reimplementation of a Chemistry class that runs unit tests.
 * The only difference is that it provides better stack traces
 * by re-throwing exceptions with their cause.
 */
public class MyJUnitHelper {

    public static final String JUNIT_PARAMETERS = "org.apache.chemistry.opencmis.tck.junit.parameters";

    private MyJUnitHelper() {
    }

    public static void run(CmisTest test) {
        try {
            run(new WrapperCmisTestGroup(test));
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }
    }

    public static void run(CmisTestGroup group) {
        try {
            JUnitRunner runner = new JUnitRunner();

            String parametersFile = System.getProperty(JUNIT_PARAMETERS);
            if (parametersFile == null) {
                runner.setParameters(null);
            } else {
                runner.loadParameters(new File(parametersFile));
            }

            runner.addGroup(group);
            runner.run(new JUnitProgressMonitor());

            CmisTestReport report = new TextReport();
            report.createReport(runner.getParameters(), runner.getGroups(),
                    new PrintWriter(System.out));

            checkForFailures(runner);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static void checkForFailures(JUnitRunner runner) {
        for (CmisTestGroup group : runner.getGroups()) {
            for (CmisTest test : group.getTests()) {
                for (CmisTestResult result : test.getResults()) {
                    if (result.getStatus().getLevel() >= CmisTestResultStatus.FAILURE
                            .getLevel()) {
                        Assert.fail(result.getMessage());
                    }
                }
            }
        }
    }

    private static class JUnitRunner extends AbstractRunner {}

    private static class JUnitProgressMonitor implements CmisTestProgressMonitor {
        @Override
        public void startGroup(CmisTestGroup group) {
            System.out.println(group.getName() + " (" + group.getTests().size()
                    + " tests)");
        }

        @Override
        public void endGroup(CmisTestGroup group) {
            return;
        }

        @Override
        public void startTest(CmisTest test) {
            System.out.println("  " + test.getName());
        }

        @Override
        public void endTest(CmisTest test) {
            return;
        }

        @Override
        public void message(String msg) {
            System.out.println(msg);
        }
    }
}
