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

object Constants {

    internal val AD_TYPE_NAME = "adType"
    internal val AD_SERVER_NAME = "adServer"
    internal val AD_SIZE_NAME = "adSize"
    internal val AUTO_REFRESH_NAME = "autoRefresh"

    //AppNexus
    // Prebid server config ids
    internal val PBS_ACCOUNT_ID_APPNEXUS = "bfa84af2-bd16-4d35-96ad-31c6bb888df0"
    internal val PBS_CONFIG_ID_300x250_APPNEXUS = "6ace8c7d-88c0-4623-8117-75bc3f0a2e45"
    internal val PBS_CONFIG_ID_320x50_APPNEXUS = "625c6125-f19e-4d5b-95c5-55501526b2a4"
    internal val PBS_CONFIG_ID_INTERSTITIAL_APPNEXUS = "625c6125-f19e-4d5b-95c5-55501526b2a4"
    // ad unit ids
    internal val BANNER_ADUNIT_ID_300x250_APPNEXUS = "83561"
    internal val BANNER_ADUNIT_ID_320x50_APPNEXUS = "79994"
    internal val INTERSTITIAL_ADUNIT_ID_APPNEXUS = ""

    internal var PBS_ACCOUNT_ID = PBS_ACCOUNT_ID_APPNEXUS
    internal var PBS_CONFIG_ID_300x250 = PBS_CONFIG_ID_300x250_APPNEXUS
    internal var PBS_CONFIG_ID_320x50 = PBS_CONFIG_ID_320x50_APPNEXUS
    internal var PBS_CONFIG_ID_INTERSTITIAL = PBS_CONFIG_ID_INTERSTITIAL_APPNEXUS
    // ad unit ids
    internal var BANNER_ADUNIT_ID_300x250 = BANNER_ADUNIT_ID_300x250_APPNEXUS
    internal var BANNER_ADUNIT_ID_320x50 = BANNER_ADUNIT_ID_320x50_APPNEXUS
    internal var INTERSTITIAL_ADUNIT_ID = INTERSTITIAL_ADUNIT_ID_APPNEXUS

}
