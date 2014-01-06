package se.repos.cmis;

import org.apache.chemistry.opencmis.tck.tests.basics.BasicsTestGroup;
import org.junit.Test;

public class MyBasicTests extends BasicsTestGroup {
    @Test
    @Override
    public void junit() {
        MyJUnitHelper.run(this);
    }
}
