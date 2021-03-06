/*
 *    Copyright 2018-2019 Prebid.org, Inc.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package org.prebid.mobile;

import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.prebid.mobile.testutils.BaseSetup;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static junit.framework.Assert.assertEquals;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = BaseSetup.testSDK)
public class VideoAdUnitTest {
    @Test
    public void testVideoAdUnitCreation() throws Exception {
        VideoAdUnit adUnit = new VideoAdUnit("123456", 320, 50, VideoAdUnit.PlacementType.IN_BANNER);
        assertEquals(new AdSize(320, 50), adUnit.getAdSize());
        assertEquals("123456", FieldUtils.readField(adUnit, "configId", true));
        assertEquals(AdType.VIDEO, FieldUtils.readField(adUnit, "adType", true));
        assertEquals(VideoAdUnit.PlacementType.IN_BANNER, FieldUtils.readField(adUnit, "type", true));
    }
}
