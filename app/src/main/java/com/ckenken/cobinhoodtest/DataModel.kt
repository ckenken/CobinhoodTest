package com.ckenken.cobinhoodtest

import android.os.Handler
import android.os.Looper
import android.util.Log
import org.java_websocket.client.WebSocketClient
import org.java_websocket.handshake.ServerHandshake
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL

object DataModel {
    private const val TAG = "DataModel"
    private const val PING_JSON = "{\n" +
            "    \"action\": \"ping\",\n" +
            "    \"id\": \"sample_id\"\n" +
            "}\n"

    private const val KEY_TICKER_REQUEST_ACTION = "action"
    private const val KEY_TICKER_REQUEST_TYPE = "type"
    private const val KEY_TICKER_REQUEST_TRADING_PAIR_ID = "trading_pair_id"
    private const val VALUE_TICKER_REQUEST_ACTION = "subscribe"
    private const val VALUE_TICKER_REQUEST_TYPE = "ticker"

    val sChannelIdList : ArrayList<String> by lazy { ArrayList<String>() }

    private val mListenerList by lazy { ArrayList<DataReceiveCallBack>() }

    private var mWebSocketClient : WebSocketClient? = null

    private val handler = Handler(Looper.getMainLooper())

    fun startDataInit(callback: DataInitCallBack) {
        Thread {
            val s = firstTimeFetch()
            handler.post {
                callback.onDataInitFinished(s)
            }
        }.start()
    }

    private fun startDataReceive() {
        Log.d(TAG, "startDataReceive(): ")

        val map = HashMap<String, String>()
        map["authorization"] =
                "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJhcGlfdG9rZW5faWQiOiJhYjY3MTk0NS05NjA4LTRmYWMtYWIyMC1jYTRmZTZhMDQxYWQiLCJzY29wZSI6WyJzY29wZV9leGNoYW5nZV90cmFkZV9yZWFkIl0sInVzZXJfaWQiOiI5NmVjZDU0MS03YWJiLTQ4MzktOThiYS05YzgyYTQyY2M4NzAifQ.Tb-Jv_XWkCYqnSX4qEDsilBzRqiE2MzysLGlpCH1des.V2:0e1d023592bc0bceda07b0089f0ffc2692e2a79c42c01ea0da69014d957c61d7"
        mWebSocketClient = object : WebSocketClient(URI("wss://ws.cobinhood.com/v2/ws"), map) {

            override fun onOpen(handshakedata: ServerHandshake) {
                Log.d(TAG, "onOpen(): start!")
//                    mWebSocketClient?.send(s)
                for (channelId : String in sChannelIdList) {
                    val requestJSONObject = JSONObject()
                    requestJSONObject.put(KEY_TICKER_REQUEST_ACTION, VALUE_TICKER_REQUEST_ACTION)
                    requestJSONObject.put(KEY_TICKER_REQUEST_TYPE, VALUE_TICKER_REQUEST_TYPE)
                    requestJSONObject.put(KEY_TICKER_REQUEST_TRADING_PAIR_ID, channelId)

                    Log.d(TAG, "onOpen(): requestString = " + requestJSONObject.toString())
                    mWebSocketClient?.send(requestJSONObject.toString())
                }
                mWebSocketClient?.send(PING_JSON)
            }

            override fun onMessage(message: String) {
                Log.d(TAG, "onMessage(): message = $message")
                val json = JSONObject(message)
                val hArray = json.getJSONArray("h")

                if (hArray[2] == "pong") {
                    Handler(Looper.getMainLooper()).postDelayed({
                        if (isOpen) {
                            mWebSocketClient?.send(PING_JSON)
                        }
                    }, 3000)
                } else {
                    if (hArray[2] != "subscribed") {
                        handler.post {
                            for (listener : DataReceiveCallBack in mListenerList) {
                                listener.onDataReceived(message)
                            }
                        }
                    }
                }
            }

            override fun onClose(code: Int, reason: String, remote: Boolean) {
                Log.d(TAG, "onClose()")
            }

            override fun onError(ex: Exception) {
                Log.d(TAG, "onError()")
                ex.printStackTrace()
            }
        }
        mWebSocketClient?.connect()
    }

    private fun firstTimeFetch() : String {
        val result = StringBuilder()

        val url = URL("https://api.cobinhood.com/v1/market/tickers")

        val conn : HttpURLConnection = url.openConnection() as HttpURLConnection
        conn.requestMethod = "GET"
        val inputStream = InputStreamReader(conn.inputStream,"utf8")
        val rd = BufferedReader(inputStream)

        var line : String? = ""
        do  {
            result.append(line)
            line = rd.readLine()
            Log.d(TAG, "line = $line")
        } while (line != null)
        rd.close()
        inputStream.close()

        Log.d(TAG, "all = " + result.toString())
        return result.toString()
    }

    fun registerDataModel(listener : DataReceiveCallBack) {
        if (mListenerList.size == 0) {
            startDataReceive()
        }
        mListenerList.add(listener)
    }

    fun unRegisterDataModel(listener : DataReceiveCallBack) {
        mListenerList.remove(listener)
        if (mListenerList.size == 0 && mWebSocketClient != null) {
            mWebSocketClient?.close()
            mWebSocketClient = null
        }
    }

    interface DataReceiveCallBack {
        fun onDataReceived(dataString : String)
    }

    interface DataInitCallBack{
        fun onDataInitFinished(dataString : String)
    }
}