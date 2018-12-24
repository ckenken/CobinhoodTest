package com.ckenken.cobinhoodtest.structure

class TradePair (val mChannelId : String, val mTimeStamp : String, val mHighestBid : String, val mLowestAsk : String,
                 val m24hVolume : String, val m24hLow : String, val m24hHigh : String, val m24hOpen : String,
                 val mLastTradePrice : String) {

//    channel id: trading pair ID
//    timestamp: ticker timestamp in milliseconds
//    highest_bid: best bid price in current order book
//    lowest_ask: best ask price in current order book
//    24h_volume: trading volume of the last 24 hours
//    24h_low: lowest trade price of the last 24 hours
//    24h_high: highest trade price of the last 24 hours
//    24h_open: first trade price of the last 24 hours
//    last_trade_price: latest trade price

    fun riseFall() : Double {
        val trade24hOpenDouble = m24hOpen.toDouble()
        val lastTradePairDouble = mLastTradePrice.toDouble()

        if (trade24hOpenDouble != 0.0) {
            return (lastTradePairDouble - trade24hOpenDouble) / (trade24hOpenDouble)
        } else {
            return 0.0
        }
    }

    fun riseFallPercentage() : String {
        if (riseFall() == 0.0) {
            return "0 %"
        } else {
            return String.format("%.2f", riseFall() * 100) + " %"
        }
    }

    fun copy() : TradePair {
        return TradePair(this.mChannelId, this.mTimeStamp, this.mHighestBid, this.mLowestAsk, this.m24hVolume, this.m24hLow, this.m24hHigh, this.m24hOpen, this.mLastTradePrice)
    }

    companion object {
        private const val TAG = "TradePair"

    }
}