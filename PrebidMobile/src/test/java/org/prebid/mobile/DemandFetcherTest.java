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

import android.os.Bundle;

import com.socdm.d.adgeneration.ADG;

import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.prebid.mobile.testutils.BaseSetup;
import org.prebid.mobile.testutils.MockPrebidServerResponses;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLooper;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import okhttp3.HttpUrl;
import okhttp3.mockwebserver.MockResponse;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotSame;
import static junit.framework.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = BaseSetup.testSDK)
public class DemandFetcherTest extends BaseSetup {

    @Test
    public void testBaseConditions() throws Exception {
        ADG adg = new ADG(activity);
        DemandFetcher demandFetcher = new DemandFetcher(adg);
        demandFetcher.setPeriodMillis(0);
        HashSet<AdSize> sizes = new HashSet<>();
        sizes.add(new AdSize(300, 250));
        RequestParams requestParams = new RequestParams("12345", AdType.BANNER, sizes);
        demandFetcher.setRequestParams(requestParams);
        assertEquals(DemandFetcher.STATE.STOPPED, FieldUtils.readField(demandFetcher, "state", true));
        demandFetcher.start();
        assertEquals(DemandFetcher.STATE.RUNNING, FieldUtils.readField(demandFetcher, "state", true));
        ShadowLooper fetcherLooper = Shadows.shadowOf(demandFetcher.getHandler().getLooper());
        fetcherLooper.runOneTask();
        Robolectric.flushBackgroundThreadScheduler();
        Robolectric.flushForegroundThreadScheduler();
        demandFetcher.destroy();
        assertEquals(DemandFetcher.STATE.DESTROYED, FieldUtils.readField(demandFetcher, "state", true));
    }

    @Test
    public void testSingleRequestNoBidsResponse() throws Exception {
        HttpUrl httpUrl = server.url("/");
        Host.CUSTOM.setHostUrl(httpUrl.toString());
        PrebidMobile.setPrebidServerHost(Host.CUSTOM);
        server.enqueue(new MockResponse().setResponseCode(200).setBody(MockPrebidServerResponses.noBid()));
        ADG adg = new ADG(activity);
        DemandFetcher demandFetcher = new DemandFetcher(adg);
        PrebidMobile.setTimeoutMillis(Integer.MAX_VALUE);
        demandFetcher.setPeriodMillis(0);
        HashSet<AdSize> sizes = new HashSet<>();
        sizes.add(new AdSize(300, 250));
        RequestParams requestParams = new RequestParams("12345", AdType.BANNER, sizes);
        demandFetcher.setRequestParams(requestParams);
        OnCompleteListener mockListener = mock(OnCompleteListener.class);
        demandFetcher.setListener(mockListener);
        assertEquals(DemandFetcher.STATE.STOPPED, FieldUtils.readField(demandFetcher, "state", true));
        demandFetcher.start();
        assertEquals(DemandFetcher.STATE.RUNNING, FieldUtils.readField(demandFetcher, "state", true));
        ShadowLooper fetcherLooper = Shadows.shadowOf(demandFetcher.getHandler().getLooper());
        fetcherLooper.runOneTask();
        ShadowLooper demandLooper = Shadows.shadowOf(demandFetcher.getDemandHandler().getLooper());
        demandLooper.runOneTask();
        Robolectric.flushBackgroundThreadScheduler();
        Robolectric.flushForegroundThreadScheduler();
        verify(mockListener).onComplete(ResultCode.NO_BIDS);
        assertEquals(DemandFetcher.STATE.DESTROYED, FieldUtils.readField(demandFetcher, "state", true));
    }

    @Test
    public void testDestroyAutoRefresh() throws Exception {
        HttpUrl httpUrl = server.url("/");
        Host.CUSTOM.setHostUrl(httpUrl.toString());
        PrebidMobile.setPrebidServerHost(Host.CUSTOM);
        server.enqueue(new MockResponse().setResponseCode(200).setBody(MockPrebidServerResponses.noBid()));
        server.enqueue(new MockResponse().setResponseCode(200).setBody(MockPrebidServerResponses.noBid()));
        server.enqueue(new MockResponse().setResponseCode(200).setBody(MockPrebidServerResponses.noBid()));
        ADG adg = new ADG(activity);
        DemandFetcher demandFetcher = new DemandFetcher(adg);
        PrebidMobile.setTimeoutMillis(Integer.MAX_VALUE);
        demandFetcher.setPeriodMillis(30);
        HashSet<AdSize> sizes = new HashSet<>();
        sizes.add(new AdSize(300, 250));
        RequestParams requestParams = new RequestParams("12345", AdType.BANNER, sizes);
        demandFetcher.setRequestParams(requestParams);
        OnCompleteListener mockListener = mock(OnCompleteListener.class);
        demandFetcher.setListener(mockListener);
        assertEquals(DemandFetcher.STATE.STOPPED, FieldUtils.readField(demandFetcher, "state", true));
        demandFetcher.start();
        assertEquals(DemandFetcher.STATE.RUNNING, FieldUtils.readField(demandFetcher, "state", true));
        ShadowLooper fetcherLooper = Shadows.shadowOf(demandFetcher.getHandler().getLooper());
        fetcherLooper.runOneTask();
        ShadowLooper demandLooper = Shadows.shadowOf(demandFetcher.getDemandHandler().getLooper());
        demandLooper.runOneTask();
        Robolectric.flushBackgroundThreadScheduler();
        Robolectric.flushForegroundThreadScheduler();
        assertEquals(DemandFetcher.STATE.RUNNING, FieldUtils.readField(demandFetcher, "state", true));
        demandFetcher.destroy();
        assertTrue(!Robolectric.getForegroundThreadScheduler().areAnyRunnable());
        assertTrue(!Robolectric.getBackgroundThreadScheduler().areAnyRunnable());
        assertEquals(DemandFetcher.STATE.DESTROYED, FieldUtils.readField(demandFetcher, "state", true));
        verify(mockListener, Mockito.times(1)).onComplete(ResultCode.NO_BIDS);
    }

    @Test
    public void testSingleRequestOneBidResponseForADGAdObject() throws Exception {
        HttpUrl httpUrl = server.url("/");
        Host.CUSTOM.setHostUrl(httpUrl.toString());
        PrebidMobile.setPrebidServerHost(Host.CUSTOM);
        server.enqueue(new MockResponse().setResponseCode(200).setBody(MockPrebidServerResponses.oneBidFromAppNexus()));
        ADG adg = new ADG(activity);
        DemandFetcher demandFetcher = new DemandFetcher(adg);
        PrebidMobile.setTimeoutMillis(Integer.MAX_VALUE);
        demandFetcher.setPeriodMillis(0);
        HashSet<AdSize> sizes = new HashSet<>();
        sizes.add(new AdSize(300, 250));
        RequestParams requestParams = new RequestParams("12345", AdType.BANNER, sizes);
        demandFetcher.setRequestParams(requestParams);
        OnCompleteListener mockListener = mock(OnCompleteListener.class);
        demandFetcher.setListener(mockListener);
        assertEquals(DemandFetcher.STATE.STOPPED, FieldUtils.readField(demandFetcher, "state", true));
        demandFetcher.start();
        assertEquals(DemandFetcher.STATE.RUNNING, FieldUtils.readField(demandFetcher, "state", true));
        ShadowLooper fetcherLooper = Shadows.shadowOf(demandFetcher.getHandler().getLooper());
        fetcherLooper.runOneTask();
        ShadowLooper demandLooper = Shadows.shadowOf(demandFetcher.getDemandHandler().getLooper());
        demandLooper.runOneTask();
        Robolectric.flushBackgroundThreadScheduler();
        Robolectric.flushForegroundThreadScheduler();
        verify(mockListener).onComplete(ResultCode.SUCCESS);
        assertEquals(DemandFetcher.STATE.DESTROYED, FieldUtils.readField(demandFetcher, "state", true));

        Map<String, String> params = adg.getRequestOptionParams();
        assertEquals(new HashMap<String, String>(){{
            put("hb_pb", "0.50");
            put("hb_pb_appnexus", "0.50");
            put("hb_bidder", "appnexus");
            put("hb_bidder_appnexus", "appnexus");
            put("hb_cache_id", "df4aba04-5e69-44b8-8608-058ab21600b8");
            put("hb_cache_id_appnexus", "df4aba04-5e69-44b8-8608-058ab21600b8");
            put("hb_env", "mobile-app");
            put("hb_env_appnexus", "mobile-app");
            put("hb_size", "300x250");
            put("hb_size_appnexus", "300x250");
        }
        }, params);
    }

    @Test
    public void testSingleRequestOneBidRubiconResponseForADGAdObject() throws Exception {
        HttpUrl httpUrl = server.url("/");
        Host.CUSTOM.setHostUrl(httpUrl.toString());
        PrebidMobile.setPrebidServerHost(Host.CUSTOM);
        PrebidMobile.setApplicationContext(activity);
        server.enqueue(new MockResponse().setResponseCode(200).setBody(MockPrebidServerResponses.oneBidFromRubicon()));
        ADG adg = new ADG(activity);
        DemandFetcher demandFetcher = new DemandFetcher(adg);
        PrebidMobile.setTimeoutMillis(Integer.MAX_VALUE);
        demandFetcher.setPeriodMillis(0);
        HashSet<AdSize> sizes = new HashSet<>();
        sizes.add(new AdSize(300, 250));
        RequestParams requestParams = new RequestParams("12345", AdType.BANNER, sizes);
        demandFetcher.setRequestParams(requestParams);
        OnCompleteListener mockListener = mock(OnCompleteListener.class);
        demandFetcher.setListener(mockListener);
        assertEquals(DemandFetcher.STATE.STOPPED, FieldUtils.readField(demandFetcher, "state", true));
        demandFetcher.start();
        assertEquals(DemandFetcher.STATE.RUNNING, FieldUtils.readField(demandFetcher, "state", true));
        ShadowLooper fetcherLooper = Shadows.shadowOf(demandFetcher.getHandler().getLooper());
        fetcherLooper.runOneTask();
        ShadowLooper demandLooper = Shadows.shadowOf(demandFetcher.getDemandHandler().getLooper());
        demandLooper.runOneTask();
        Robolectric.flushBackgroundThreadScheduler();
        Robolectric.flushForegroundThreadScheduler();
        verify(mockListener).onComplete(ResultCode.SUCCESS);
        assertEquals(DemandFetcher.STATE.DESTROYED, FieldUtils.readField(demandFetcher, "state", true));

        Map<String, String> params = adg.getRequestOptionParams();
        assertEquals(new HashMap<String, String>(){{
            put("hb_pb", "1.20");
            put("hb_pb_rubicon", "1.20");
            put("hb_bidder", "rubicon");
            put("hb_bidder_rubicon", "rubicon");
            put("hb_cache_id", "a2f41588-4727-425c-9ef0-3b382debef1e");
            put("hb_cache_id_rubicon", "a2f41588-4727-425c-9ef0-3b382debef1e");
            put("hb_env", "mobile-app");
            put("hb_env_rubicon", "mobile-app");
            put("hb_size", "300x250");
            put("hb_size_rubicon", "300x250");
            put("hb_cache_hostpath", "https://prebid-cache-europe.rubiconproject.com/cache");
            put("hb_cache_hostpath_rubicon", "https://prebid-cache-europe.rubiconproject.com/cache");
            put("hb_cache_path", "/cache");
            put("hb_cache_path_rubicon", "/cache");
            put("hb_cache_host", "prebid-cache-europe.rubiconproject.com");
            put("hb_cache_host_rubicon", "prebid-cache-europe.rubiconproject.com");
        }
        }, params);
    }

}
