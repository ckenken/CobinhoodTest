package com.ckenken.cobinhoodtest

import android.text.TextUtils
import android.util.Log
import com.ckenken.cobinhoodtest.structure.TradePair
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

class ViewModel : DataModel.DataReceiveCallBack, DataModel.AllTradeFetchCallBack{
    companion object {
        private const val TAG = "ViewModel"
        private const val JSON_KEY_RESULT = "result"
        private const val JSON_KEY_TICKERS = "tickers"
        private const val JSON_KEY_TRADING_PAIR_ID = "trading_pair_id"
        private const val JSON_KEY_TIMESTAMP = "timestamp"
        private const val JSON_KEY_24H_HIGH = "24h_high"
        private const val JSON_KEY_24H_LOW = "24h_low"
        private const val JSON_KEY_24H_OPEN = "24h_open"
        private const val JSON_KEY_24H_VOLUME = "24h_volume"
        private const val JSON_KEY_LAST_TRADE_PRICE = "last_trade_price"
        private const val JSON_KEY_HIGHEST_BID = "highest_bid"
        private const val JSON_KEY_LOWEST_ASK = "lowest_ask"
    }

    var searchString : String = ""
    set(value){
        Log.d(TAG, "setter(): field = $field")
        field = value
        val newMap = generateFilteredTradeMap()
        for (listener in mListenerList) {
            listener.onDataChanged(newMap)
        }
    }

    private val mListenerList : ArrayList<ViewModelCallBack> by lazy { ArrayList<ViewModelCallBack>() }

    private var mTradeMap : HashMap<String, TradePair> = HashMap<String, TradePair>()

    private fun parseDataStringToTradePairAll(dataString : String): HashMap<String, TradePair> {
        val tradeMap = HashMap<String, TradePair>()
        try {
            val result : JSONObject = JSONObject(dataString).getJSONObject(JSON_KEY_RESULT)
            val tradeArray : JSONArray = result.getJSONArray(JSON_KEY_TICKERS)
            var i = 0
            while(i < tradeArray.length()) {
                val tradeObject : JSONObject = tradeArray.getJSONObject(i)
                val tradePairId = tradeObject.getString(JSON_KEY_TRADING_PAIR_ID)
                val timestamp = tradeObject.getLong(JSON_KEY_TIMESTAMP).toString()
                val highestBid = tradeObject.getString(JSON_KEY_HIGHEST_BID)
                val lowestAsk = tradeObject.getString(JSON_KEY_LOWEST_ASK)
                val trade24hVolume = tradeObject.getString(JSON_KEY_24H_VOLUME)
                val trade24hLow = tradeObject.getString(JSON_KEY_24H_LOW)
                val trade24hHigh = tradeObject.getString(JSON_KEY_24H_HIGH)
                val trade24hOpen = tradeObject.getString(JSON_KEY_24H_OPEN)
                val lastTradePrice = tradeObject.getString(JSON_KEY_LAST_TRADE_PRICE)

                val newTradePair = TradePair(tradePairId, timestamp, highestBid, lowestAsk, trade24hVolume, trade24hLow, trade24hHigh, trade24hOpen, lastTradePrice)

                DataModel.sChannelIdList.add(tradePairId)  // for WebSocket api

                tradeMap[tradePairId] = newTradePair
                i++
            }
        } catch (e : JSONException) {
            e.printStackTrace()
            Log.d(TAG, "parseDataStringToTradePairAll(): JSON error!")
        }
        return tradeMap
    }

    private fun parseDataStringToTradePairIndividual(dataString : String) : TradePair {

        // {"h":["ticker.COB-ETH","2","s"],"d":["1545543360000","0.0001066","0.0001089","2944107.851707899","0.0001092","0.000105","0.0001054","0.0001078"]}

        //    timestamp: ticker timestamp in milliseconds
        //    highest_bid: best bid price in current order book
        //    lowest_ask: best ask price in current order book
        //    24h_volume: trading volume of the last 24 hours
        //    24h_low: lowest trade price of the last 24 hours
        //    24h_high: highest trade price of the last 24 hours
        //    24h_open: first trade price of the last 24 hours
        //    last_trade_price: latest trade price

        val json = JSONObject(dataString)
        val hArray = json.getJSONArray("h")
        val dArray : JSONArray = json.getJSONArray("d")

        val channelId = hArray[0].toString().replace("ticker.", "")
        val timestamp = dArray[0].toString()
        val highestBid = dArray[1].toString()
        val lowestAsk = dArray[2].toString()
        val trade24hVolume = dArray[3].toString()
        val trade24hLow = dArray[4].toString()
        val trade24hHigh = dArray[5].toString()
        val trade24hOpen = dArray[6].toString()
        val lastTradePrice = dArray[7].toString()

        return TradePair(channelId, timestamp, highestBid, lowestAsk, trade24hVolume, trade24hHigh, trade24hLow, trade24hOpen, lastTradePrice)
    }

    private fun generateFilteredTradeMap() : HashMap<String, TradePair> {
        val tempMap = HashMap<String, TradePair>()

        Log.d(TAG, "searchString = $searchString")

        if (TextUtils.isEmpty(searchString)) {
            tempMap.putAll(mTradeMap)
            return tempMap
        }

        for (item in mTradeMap) {
            if (item.value.mChannelId.contains(searchString, true)) {
                tempMap[item.key] = item.value
            }
        }
        return tempMap
    }

    override fun onDataReceived(dataString: String) {
//        Log.d(TAG, "onDataReceived(): dataString = $dataString")
        val tradePair = parseDataStringToTradePairIndividual(dataString)

        mTradeMap[tradePair.mChannelId] = tradePair

        for (listener in mListenerList) {
            listener.onDataChanged(generateFilteredTradeMap())
        }
    }

    override fun onAllTradeFetchingFinished(dataString: String) {
//        Log.d(TAG, "onAllTradeFetchingFinished(): dataInitString = $dataString")
        mTradeMap = parseDataStringToTradePairAll(dataString)
        for (listener : ViewModelCallBack in mListenerList) {
            listener.onDataChanged(mTradeMap)
        }
        DataModel.registerDataModel(this)
    }

    fun registerViewModel(listener : ViewModelCallBack) {
        if (mListenerList.size == 0) {
            DataModel.startAllTradeListFetching(this)
        }
        mListenerList.add(listener)
    }

    fun unRegisterViewModel(listener : ViewModelCallBack) {
        mListenerList.remove(listener)
        if (mListenerList.size == 0) {
            DataModel.unRegisterDataModel(this)
        }
    }

    interface ViewModelCallBack {
        fun onDataChanged(tradeMap : HashMap<String, TradePair>)
    }
}