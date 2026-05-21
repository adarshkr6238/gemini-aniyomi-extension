package com.example.data.blocker

import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import java.io.ByteArrayInputStream
import java.net.URI

object AdBlocker {
    private val AD_KEYWORDS = hashSetOf(
        "doubleclick", "googleads", "analytics", "popads", "popcash", "exoclick",
        "juicyads", "onclickads", "exdynsrv", "adsterra", "clktag", "coinhive",
        "adservice", "adsystem", "aaxads", "amazon-adsystem", "taboola", "outbrain",
        "mgid", "revcontent", "propellerads", "bidvertiser", "popunder", "redirect",
        "adkeeper", "adfox", "adsense", "adzerk", "smartadserver", "ads", "adnxs",
        "fastclick", "adroll", "carbonads", "buysellads", "adbtc", "hcaptcha",
        "geetest", "vidoomy", "yandex", "statcounter", "hitsproweb", "adn", "adbox",
        "banner", "popup"
    )

    fun isAd(url: String): Boolean {
        try {
            val host = URI(url).host?.lowercase() ?: return false
            val parts = host.split(".")
            for (part in parts) {
                if (AD_KEYWORDS.contains(part)) {
                    return true
                }
            }
        } catch (e: Exception) {
            // Fallback safe string checks
            val lowerUrl = url.lowercase()
            for (keyword in AD_KEYWORDS) {
                if (lowerUrl.contains(".$keyword") || lowerUrl.contains("/$keyword")) {
                    return true
                }
            }
        }
        return false
    }

    fun createEmptyResponse(): WebResourceResponse {
        return WebResourceResponse("text/plain", "UTF-8", ByteArrayInputStream("".toByteArray()))
    }

    val AD_BLOCK_JS = """
        (function() {
            // Disable window.open popup triggers
            window.open = function() {
                console.log('AniyomiBlocker: Blocked window.open popup request.');
                return null;
            };

            // Overwrite alert & confirm popups that interrupt stream
            window.alert = function(msg) { console.log('Alert blocked: ' + msg); };
            window.confirm = function(msg) { console.log('Confirm blocked'); return true; };

            function hideAdElements() {
                const selectors = [
                    'iframe[src*="kwik.cx/p"]', 'iframe[src*="ad"]', 'div[class*="ad-"]', 'div[id*="ad-"]',
                    'div[class*="banner"]', 'div[id*="banner"]', 'div[class*="pop"]', 'div[id*="pop"]',
                    'a[href*="adsterra"]', 'a[href*="onclick"]', '.popunder', '.ad-box', '.adsbygoogle',
                    '.mgbox', '.native-ad', '#popcash', '#exoclick', '#ads'
                ];
                selectors.forEach(selector => {
                    try {
                        const items = document.querySelectorAll(selector);
                        items.forEach(el => {
                            el.style.display = 'none';
                            el.remove();
                        });
                    } catch (err) {}
                });
            }

            // Continuous removal of ad banners & popups
            hideAdElements();
            setInterval(hideAdElements, 1500);

            // Click interceptor to remove scummy overlay banners that steal focus
            document.addEventListener('click', function(e) {
                const target = e.target;
                if (target && (target.style.position === 'fixed' || target.style.position === 'absolute') 
                    && (target.style.zIndex > 9999) && !target.id && !target.className) {
                    console.log('AniyomiBlocker: Stopped popup hijack overlay');
                    e.preventDefault();
                    e.stopPropagation();
                    target.remove();
                }
            }, true);
        })();
    """.trimIndent()
}
