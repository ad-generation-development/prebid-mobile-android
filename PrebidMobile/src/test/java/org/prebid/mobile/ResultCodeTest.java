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

import android.content.Context;
import android.net.ConnectivityManager;
import android.os.Bundle;


import com.socdm.d.adgeneration.ADG;

import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.prebid.mobile.testutils.BaseSetup;
import org.prebid.mobile.testutils.MockPrebidServerResponses;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLooper;
import org.robolectric.shadows.ShadowNetworkInfo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.UUID;

import okhttp3.HttpUrl;
import okhttp3.mockwebserver.MockResponse;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.robolectric.Shadows.shadowOf;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = BaseSetup.testSDK)
public class ResultCodeTest extends BaseSetup {
    @Test
    public void testInvalidContext() throws Exception {

        HttpUrl httpUrl = server.url("/");
        Host.CUSTOM.setHostUrl(httpUrl.toString());
        PrebidMobile.setPrebidServerHost(Host.CUSTOM);
        PrebidMobile.setPrebidServerAccountId("123456");
        PrebidMobile.setApplicationContext(null);
        server.enqueue(new MockResponse().setResponseCode(200).setBody(MockPrebidServerResponses.oneBidFromAppNexus()));
        InterstitialAdUnit adUnit = new InterstitialAdUnit("123456");
        ADG testView = new ADG(activity);
        OnCompleteListener mockListener = mock(OnCompleteListener.class);
        adUnit.fetchDemand(testView, mockListener);
        verify(mockListener).onComplete(ResultCode.INVALID_CONTEXT);

    }

    @Test
    public void testSuccessForMoPub() throws Exception {

        HttpUrl httpUrl = server.url("/");
        Host.CUSTOM.setHostUrl(httpUrl.toString());
        PrebidMobile.setPrebidServerHost(Host.CUSTOM);
        PrebidMobile.setApplicationContext(activity.getApplicationContext());
        PrebidMobile.setPrebidServerAccountId("123456");
        server.enqueue(new MockResponse().setResponseCode(200).setBody(MockPrebidServerResponses.oneBidFromAppNexus()));
        BannerAdUnit adUnit = new BannerAdUnit("123456", 300, 250);
        ADG testView = new ADG(activity);
        OnCompleteListener mockListener = mock(OnCompleteListener.class);
        adUnit.fetchDemand(testView, mockListener);
        DemandFetcher fetcher = (DemandFetcher) FieldUtils.readField(adUnit, "fetcher", true);
        PrebidMobile.setTimeoutMillis(Integer.MAX_VALUE);
        ShadowLooper fetcherLooper = shadowOf(fetcher.getHandler().getLooper());
        fetcherLooper.runOneTask();
        ShadowLooper demandLooper = shadowOf(fetcher.getDemandHandler().getLooper());
        demandLooper.runOneTask();
        Robolectric.flushBackgroundThreadScheduler();
        Robolectric.flushForegroundThreadScheduler();
        verify(mockListener).onComplete(ResultCode.SUCCESS);
        assertEquals(new HashMap(){{
            put("hb_pb", "0.50");
            put("hb_env", "mobile-app");
            put("hb_pb_appnexus","0.50");
            put("hb_size","300x250");
            put("hb_bidder_appnexus","appnexus");
            put("hb_bidder","appnexus");
            put("hb_cache_id","df4aba04-5e69-44b8-8608-058ab21600b8");
            put("hb_env_appnexus","mobile-app");
            put("hb_size_appnexus","300x250");
            put("hb_cache_id_appnexus","df4aba04-5e69-44b8-8608-058ab21600b8");
        }},testView.getRequestOptionParams());
    }

    @Test
    public void testNetworkError() {
        PrebidMobile.setPrebidServerHost(Host.APPNEXUS);
        PrebidMobile.setApplicationContext(activity.getApplicationContext());
        PrebidMobile.setPrebidServerAccountId("123456");
        ConnectivityManager connectivityManager = (ConnectivityManager) activity.getSystemService(Context.CONNECTIVITY_SERVICE);
        ShadowNetworkInfo shadowOfActiveNetworkInfo = shadowOf(connectivityManager.getActiveNetworkInfo());
        shadowOfActiveNetworkInfo.setConnectionStatus(false);
        BannerAdUnit adUnit = new BannerAdUnit("123456", 300, 250);
        ADG testView = new ADG(activity);
        OnCompleteListener mockListener = mock(OnCompleteListener.class);
        adUnit.fetchDemand(testView, mockListener);
        verify(mockListener).onComplete(ResultCode.NETWORK_ERROR);
    }

    @Test
    public void testTimeOut() throws Exception {
        PrebidMobile.setPrebidServerHost(Host.APPNEXUS);
        PrebidMobile.setTimeoutMillis(30);
        PrebidMobile.setPrebidServerAccountId("b7adad2c-e042-4126-8ca1-b3caac7d3e5c");
        PrebidMobile.setShareGeoLocation(true);
        PrebidMobile.setApplicationContext(activity.getApplicationContext());
        DemandAdapter.DemandAdapterListener mockListener = mock(DemandAdapter.DemandAdapterListener.class);
        PrebidServerAdapter adapter = new PrebidServerAdapter();
        HashSet<AdSize> sizes = new HashSet<>();
        sizes.add(new AdSize(0, 250));
        RequestParams requestParams = new RequestParams("e2edc23f-0b3b-4203-81b5-7cc97132f418", AdType.BANNER, sizes);
        String uuid = UUID.randomUUID().toString();
        adapter.requestDemand(requestParams, mockListener, uuid);

        @SuppressWarnings("unchecked")
        ArrayList<PrebidServerAdapter.ServerConnector> connectors = (ArrayList<PrebidServerAdapter.ServerConnector>) FieldUtils.readDeclaredField(adapter, "serverConnectors", true);
        PrebidServerAdapter.ServerConnector connector = connectors.get(0);
        PrebidServerAdapter.ServerConnector.TimeoutCountDownTimer timeoutCountDownTimer = (PrebidServerAdapter.ServerConnector.TimeoutCountDownTimer) FieldUtils.readDeclaredField(connector, "timeoutCountDownTimer", true);
        shadowOf(timeoutCountDownTimer).invokeFinish();

        Robolectric.flushBackgroundThreadScheduler();
        Robolectric.flushForegroundThreadScheduler();
        verify(mockListener).onDemandFailed(ResultCode.TIMEOUT, uuid);
    }

    @Test
    public void testNoBids() throws Exception {
        HttpUrl httpUrl = server.url("/");
        Host.CUSTOM.setHostUrl(httpUrl.toString());
        PrebidMobile.setPrebidServerHost(Host.CUSTOM);
        PrebidMobile.setApplicationContext(activity.getApplicationContext());
        PrebidMobile.setPrebidServerAccountId("123456");
        server.enqueue(new MockResponse().setResponseCode(200).setBody(MockPrebidServerResponses.noBid()));
        BannerAdUnit adUnit = new BannerAdUnit("123456", 300, 250);
        ADG testView = new ADG(activity);
        OnCompleteListener mockListener = mock(OnCompleteListener.class);
        adUnit.fetchDemand(testView, mockListener);
        DemandFetcher fetcher = (DemandFetcher) FieldUtils.readField(adUnit, "fetcher", true);
        PrebidMobile.setTimeoutMillis(Integer.MAX_VALUE);
        ShadowLooper fetcherLooper = shadowOf(fetcher.getHandler().getLooper());
        fetcherLooper.runOneTask();
        ShadowLooper demandLooper = shadowOf(fetcher.getDemandHandler().getLooper());
        demandLooper.runOneTask();
        Robolectric.flushBackgroundThreadScheduler();
        Robolectric.flushForegroundThreadScheduler();
        verify(mockListener).onComplete(ResultCode.NO_BIDS);
        assertEquals(null, testView.getRequestOptionParams());
    }

    @Test
    public void testNoBidsRubicon() throws Exception {
        HttpUrl httpUrl = server.url("/");
        Host.CUSTOM.setHostUrl(httpUrl.toString());
        PrebidMobile.setPrebidServerHost(Host.CUSTOM);
        PrebidMobile.setApplicationContext(activity.getApplicationContext());
        PrebidMobile.setPrebidServerAccountId("123456");
        server.enqueue(new MockResponse().setResponseCode(200).setBody(MockPrebidServerResponses.noBidFromRubicon()));
        BannerAdUnit adUnit = new BannerAdUnit("123456", 300, 250);
        ADG testView = new ADG(activity);
        OnCompleteListener mockListener = mock(OnCompleteListener.class);
        adUnit.fetchDemand(testView, mockListener);
        DemandFetcher fetcher = (DemandFetcher) FieldUtils.readField(adUnit, "fetcher", true);
        PrebidMobile.setTimeoutMillis(Integer.MAX_VALUE);
        ShadowLooper fetcherLooper = shadowOf(fetcher.getHandler().getLooper());
        fetcherLooper.runOneTask();
        ShadowLooper demandLooper = shadowOf(fetcher.getDemandHandler().getLooper());
        demandLooper.runOneTask();
        Robolectric.flushBackgroundThreadScheduler();
        Robolectric.flushForegroundThreadScheduler();
        verify(mockListener).onComplete(ResultCode.NO_BIDS);

        assertNull(testView.getRequestOptionParams());
    }

    @Test
    public void testEmptyAccountId() throws Exception {
        PrebidMobile.setPrebidServerAccountId("");
        BannerAdUnit adUnit = new BannerAdUnit("123456", 320, 50);
        ADG testView = new ADG(activity);
        OnCompleteListener mockListener = mock(OnCompleteListener.class);
        adUnit.fetchDemand(testView, mockListener);
        verify(mockListener).onComplete(ResultCode.INVALID_ACCOUNT_ID);
    }

    @Test
    public void testEmptyConfigId() throws Exception {
        PrebidMobile.setPrebidServerAccountId("123456");
        BannerAdUnit adUnit = new BannerAdUnit("", 320, 50);
        ADG testView = new ADG(activity);
        OnCompleteListener mockListener = mock(OnCompleteListener.class);
        adUnit.fetchDemand(testView, mockListener);
        verify(mockListener).onComplete(ResultCode.INVALID_CONFIG_ID);
    }

    @Test
    public void testEmptyHostUrl() throws Exception {
        PrebidMobile.setPrebidServerAccountId("123456");
        Host.CUSTOM.setHostUrl("");
        PrebidMobile.setPrebidServerHost(Host.CUSTOM);
        BannerAdUnit adUnit = new BannerAdUnit("123456", 320, 50);
        ADG testView = new ADG(activity);
        OnCompleteListener mockListener = mock(OnCompleteListener.class);
        adUnit.fetchDemand(testView, mockListener);
        verify(mockListener).onComplete(ResultCode.INVALID_HOST_URL);
    }

    @Test
    public void testNoNegativeSizeForBanner() {
        PrebidMobile.setPrebidServerAccountId("123456");
        BannerAdUnit adUnit = new BannerAdUnit("123456", 320, 50);
        adUnit.addAdditionalSize(-1, 250);
        ADG testView = new ADG(activity);
        OnCompleteListener mockListener = mock(OnCompleteListener.class);
        adUnit.fetchDemand(testView, mockListener);
        verify(mockListener).onComplete(ResultCode.INVALID_SIZE);
    }

    @Test
    public void testInvalidSizeForBanner() {
        PrebidMobile.setPrebidServerHost(Host.APPNEXUS);
        PrebidMobile.setPrebidServerAccountId("b7adad2c-e042-4126-8ca1-b3caac7d3e5c");
        PrebidMobile.setShareGeoLocation(true);
        PrebidMobile.setApplicationContext(activity.getApplicationContext());
        DemandAdapter.DemandAdapterListener mockListener = mock(DemandAdapter.DemandAdapterListener.class);
        PrebidServerAdapter adapter = new PrebidServerAdapter();
        HashSet<AdSize> sizes = new HashSet<>();
        sizes.add(new AdSize(0, 250));
        RequestParams requestParams = new RequestParams("e2edc23f-0b3b-4203-81b5-7cc97132f418", AdType.BANNER, sizes);
        String uuid = UUID.randomUUID().toString();
        adapter.requestDemand(requestParams, mockListener, uuid);
        Robolectric.flushBackgroundThreadScheduler();
        Robolectric.flushForegroundThreadScheduler();
        verify(mockListener).onDemandFailed(ResultCode.INVALID_SIZE, uuid);
    }

    @Test
    public void testInvalidAdObject() {
        PrebidMobile.setPrebidServerAccountId("123456");
        PrebidMobile.setApplicationContext(activity.getApplicationContext());
        BannerAdUnit adUnit = new BannerAdUnit("123456", 300, 250);
        OnCompleteListener mockListener = mock(OnCompleteListener.class);
        adUnit.fetchDemand(new Object(), mockListener);
        verify(mockListener).onComplete(ResultCode.INVALID_AD_OBJECT);
    }
}
