package org.prebid.mobile;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.prebid.mobile.testutils.BaseSetup;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = BaseSetup.testSDK)
public class PrebidMobileTest extends BaseSetup {
    @Test
    public void testAccountId() throws Exception {
        PrebidMobile.setAccountId("123456");
        assertEquals("123456", PrebidMobile.getAccountId());
        PrebidMobile.setApplicationContext(activity.getApplicationContext());
        assertEquals(activity.getApplicationContext(), PrebidMobile.getApplicationContext());
        PrebidMobile.setShareGeoLocation(true);
        assertTrue(PrebidMobile.isShareGeoLocation());
        PrebidMobile.setHost(Host.RUBICON);
        assertEquals(Host.RUBICON, PrebidMobile.getHost());
    }
}