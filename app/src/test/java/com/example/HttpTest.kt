package com.example

import org.junit.Test
import okhttp3.OkHttpClient
import okhttp3.Request

class HttpTest {
    @Test
    fun testLinks() {
        val client = OkHttpClient()
        // get latest release, then get episodes, then get links
        var req = Request.Builder().url("https://animepahe.ru/api?m=release").header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36").build()
        var body = client.newCall(req).execute().body?.string()
        println("release: " + body?.substring(0, 100))
        
        req = Request.Builder().url("https://animepahe.ru/api?m=search&q=naruto").header("User-Agent", "Mozilla/5.0").build()
        body = client.newCall(req).execute().body?.string()
        println("search: " + body?.substring(0, 500))
        
        val jsonObj = org.json.JSONObject(body!!)
        val firstId = jsonObj.getJSONArray("data").getJSONObject(0).getString("session")
        
        req = Request.Builder().url("https://animepahe.ru/api?m=release&id=$firstId&sort=asc&page=1").header("User-Agent", "Mozilla/5.0").build()
        body = client.newCall(req).execute().body?.string()
        val epsObj = org.json.JSONObject(body!!)
        val firstEpSession = epsObj.getJSONArray("data").getJSONObject(0).getString("session")
        
        req = Request.Builder().url("https://animepahe.ru/api?m=links&id=$firstEpSession").header("User-Agent", "Mozilla/5.0").build()
        body = client.newCall(req).execute().body?.string()
        println("links api: $body")
        
    }
}
