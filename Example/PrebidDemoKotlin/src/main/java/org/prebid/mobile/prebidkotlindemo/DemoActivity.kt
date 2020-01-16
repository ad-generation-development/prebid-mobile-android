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
package org.prebid.mobile.prebidkotlindemo

import android.os.Bundle
import android.os.Handler
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import android.widget.FrameLayout
import com.socdm.d.adgeneration.ADG
import com.socdm.d.adgeneration.ADGConsts
import com.socdm.d.adgeneration.ADGListener
import com.socdm.d.adgeneration.interstitial.ADGInterstitial
import com.socdm.d.adgeneration.interstitial.ADGInterstitialListener
import org.prebid.mobile.*
import org.prebid.mobile.prebidkotlindemo.Constants.BANNER_ADUNIT_ID_300x250
import org.prebid.mobile.prebidkotlindemo.Constants.BANNER_ADUNIT_ID_320x50

class DemoActivity : AppCompatActivity() {
    internal var refreshCount: Int = 0
    internal var adUnit: AdUnit? = null
    lateinit var resultCode: ResultCode
    internal var adg: ADG? = null
    internal var adgInterstitial: ADGInterstitial? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        refreshCount = 0
        setContentView(R.layout.activity_demo)
        val intent = intent
        if ("ADG" == intent.getStringExtra(Constants.AD_SERVER_NAME) && "Banner" == intent.getStringExtra(Constants.AD_TYPE_NAME)) {
            createBanner(intent.getStringExtra(Constants.AD_SIZE_NAME))
        } else if ("ADG" == intent.getStringExtra(Constants.AD_SERVER_NAME) && "Interstitial" == intent.getStringExtra(
                Constants.AD_TYPE_NAME
            )
        ) {
            createInterstitial()
        }
    }

    internal fun createBanner(size: String) {
        val adFrame = findViewById(R.id.adFrame) as FrameLayout
        adFrame.removeAllViews()
        val wAndH = size.split("x".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        val width = Integer.valueOf(wAndH[0])
        val height = Integer.valueOf(wAndH[1])
        adg = ADG(this)
        if (width == 300 && height == 250) {
            adg!!.locationId = BANNER_ADUNIT_ID_300x250
            adg!!.setAdFrameSize(ADG.AdFrameSize.RECT)
            adUnit = BannerAdUnit(Constants.PBS_CONFIG_ID_300x250, 300, 250)
        } else {
            adg!!.locationId = BANNER_ADUNIT_ID_320x50
            adg!!.setAdFrameSize(ADG.AdFrameSize.SP)
            adUnit = BannerAdUnit(Constants.PBS_CONFIG_ID_320x50, 320, 50)
        }
        adg!!.adListener = (object : ADGListener() {
            private val TAG = "ADGListener"
            override fun onReceiveAd() {
                Log.d(TAG, "Received an ad.")
            }
            override fun onFailedToReceiveAd(code: ADGConsts.ADGErrorCode?) {
                Log.d(TAG, "Failed to received an ad. code: " + code)
                when (code) {
                    ADGConsts.ADGErrorCode.EXCEED_LIMIT      // エラー多発
                        , ADGConsts.ADGErrorCode.NEED_CONNECTION   // ネットワーク不通
                        , ADGConsts.ADGErrorCode.NO_AD             // 広告レスポンスなし
                    -> {

                    }
                    else -> if (adg != null) {
                        adg?.start()
                    }
                }
            }
        })
        adg!!.setUsePartsResponse(true)
        adg!!.setEnableMraidMode(true)
        adg!!.isEnableTestMode = true
        adFrame.addView(adg)

        adUnit!!.setAutoRefreshPeriodMillis(intent.getIntExtra(Constants.AUTO_REFRESH_NAME, 0))
        adUnit!!.fetchDemand(adg!!, object : OnCompleteListener {
            override fun onComplete(resultCode: ResultCode) {
                this@DemoActivity.resultCode = resultCode
                adg?.start()
                refreshCount++
            }
        })
    }

    internal fun createInterstitial() {
        adgInterstitial = ADGInterstitial(this)
        adgInterstitial!!.setLocationId(Constants.INTERSTITIAL_ADUNIT_ID);
        adgInterstitial!!.setAdListener(object : ADGInterstitialListener() {
            private val TAG = "ADGInterstitialListener"
            override fun onReceiveAd() {
                Log.d(TAG, "Received an ad.")
                adgInterstitial?.show()
            }

            override fun onFailedToReceiveAd(code: ADGConsts.ADGErrorCode) {
                Log.d(TAG, "Failed to receive an ad:$code")
                // ネットワーク不通/エラー多発/広告レスポンスなし 以外はリトライしてください
                when (code) {
                    ADGConsts.ADGErrorCode.EXCEED_LIMIT      // エラー多発
                        , ADGConsts.ADGErrorCode.NEED_CONNECTION   // ネットワーク不通
                        , ADGConsts.ADGErrorCode.NO_AD             // 広告レスポンスなし
                    -> {
                    }
                    else -> if (adgInterstitial != null) {
                        adgInterstitial?.preload()
                    }
                }
            }

            override fun onClickAd() {
                Log.d(TAG, "Did click ad.")
            }

            override fun onCloseInterstitial() {
                Log.d(TAG, "Did close interstitial ads.")
            }
        })
        adUnit = InterstitialAdUnit(Constants.PBS_CONFIG_ID_INTERSTITIAL)
        val millis = intent.getIntExtra(Constants.AUTO_REFRESH_NAME, 0)
        adUnit!!.setAutoRefreshPeriodMillis(millis)
        adUnit!!.fetchDemand(adgInterstitial!!, object : OnCompleteListener {
            override fun onComplete(resultCode: ResultCode) {
                this@DemoActivity.resultCode = resultCode
                adgInterstitial?.preload()
                refreshCount++
            }
        })
    }

    override fun onResume() {
        super.onResume()
        // 広告表示/ローテーション再開
        adg?.start()
    }

    override fun onPause() {
        super.onPause()
        // ローテーション停止
        adg?.pause()
    }

    override fun onStop() {
        super.onStop()
        adg?.stop()
        adgInterstitial?.dismiss()

    }

    override fun onDestroy() {
        super.onDestroy()
        if (adUnit != null) {
            adUnit!!.stopAutoRefresh()
            adUnit = null
        }
    }

    internal fun stopAutoRefresh() {
        if (adUnit != null) {
            adUnit!!.stopAutoRefresh()
        }
    }
}
