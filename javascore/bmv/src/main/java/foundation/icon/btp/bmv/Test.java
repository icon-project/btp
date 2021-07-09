package foundation.icon.btp.bmv;

import score.Context;
import score.annotation.External;

public class Test {
    public Test(byte[] base64) {
        //byte[] _msg=Base64.getUrlDecoder().decode(base64);
    }

    @External
    public void loopTest(byte[] msg) {
        String[] strArr = {"hello", "", "1"};
        Context.revert(1, "test");
        for (String str : strArr) {
            Context.revert(1, "test" + str);
        }
    }
}
