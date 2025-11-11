package uk.gov.hmcts.reform.timedevent.testutils;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Utils {
    public static void retryTestCodeBlock(int maxAttempts, Runnable codeBlock) {
        Throwable finalError = null;
        for (int i = 0; i < maxAttempts; i++) {
            try {
                codeBlock.run();
                return;
            } catch (AssertionError e) {
                log.error("Failed attempt {} of {} due to:", i, maxAttempts);
                log.error("{}", e.getMessage());
                finalError = e;
            }
        }
        throw new AssertionError("Failed all attempts.", finalError);
    }
}
