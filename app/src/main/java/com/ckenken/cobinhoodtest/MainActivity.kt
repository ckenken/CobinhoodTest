package com.ckenken.cobinhoodtest

import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.support.v7.widget.RecyclerView
import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import com.ckenken.cobinhoodtest.structure.TradePair
import android.widget.TextView
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.LinearLayoutManager
import android.util.Log
import android.view.*


class MainActivity : Activity(), ViewModel.ViewModelCallBack {

    companion object {
        const val TAG = "MainActivity"
    }

    private val mViewModel by lazy { ViewModel() }

    private var mTradeMap : HashMap<String, TradePair> = HashMap<String, TradePair>()

    private var mTradePairList = emptyList<TradePair>()

    private val mRecyclerView by lazy { findViewById<RecyclerView>(R.id.recycleView) }

    private val mRecyclerAdapter by lazy { RecyclerAdapter(this) }

    private val mHandler by lazy { Handler(Looper.getMainLooper()) }  // Main looper Handler for handling callBack to avoid multi-thread issue

    // Observer for filtering trade pair
    private val searchEditTextTextWatcher = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {

        }

        override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
            mViewModel.searchString = s.toString()
        }

        override fun afterTextChanged(s: Editable) {

        }
    }

    inner class RecyclerAdapter(context : Context) : RecyclerView.Adapter<RecyclerAdapter.ViewHolder>() {

        private var mInflater: LayoutInflater = LayoutInflater.from(context)

        override fun onCreateViewHolder(parent : ViewGroup?, viewType: Int): RecyclerAdapter.ViewHolder {
            val view : View = mInflater.inflate(R.layout.recycler_item_layout, parent, false)
            return ViewHolder(view)
        }

        override fun getItemCount(): Int {
            return mTradePairList.size
        }

        override fun onBindViewHolder(holder : RecyclerAdapter.ViewHolder, position: Int) {
            holder.mChannelIdTextView.text = mTradePairList[position].mChannelId
            holder.mLastTradeTextView.text = mTradePairList[position].mLastTradePrice
            holder.mRiseFallPercentageTextView.text = mTradePairList[position].riseFallPercentage()
            val rise = mTradePairList[position].riseFall()
            if (rise < 0) {
                holder.mRiseFallPercentageTextView.setTextColor(Color.RED)
            } else if (rise > 0) {
                holder.mRiseFallPercentageTextView.setTextColor(Color.GREEN)
            } else {
                holder.mRiseFallPercentageTextView.setTextColor(Color.WHITE)
            }
        }

        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val mChannelIdTextView : TextView = itemView.findViewById(R.id.channelIdTextView)
            val mLastTradeTextView : TextView = itemView.findViewById(R.id.lastTradeTextView)
            val mRiseFallPercentageTextView : TextView = itemView.findViewById(R.id.riseFallPercentageTextView)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId) {
            R.id.menu_grid_mode -> {
                Log.d(TAG, "onOptionsItemSelected(): grid")
                mRecyclerView.layoutManager = GridLayoutManager(this, 2)
            }
            R.id.menu_list_mode -> {
                Log.d(TAG, "onOptionsItemSelected(): list")
                mRecyclerView.layoutManager = LinearLayoutManager(this)
            }
        }

        return super.onOptionsItemSelected(item)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val searchEditText : EditText = findViewById(R.id.searchEditText)
        searchEditText.addTextChangedListener(searchEditTextTextWatcher)

        mRecyclerView.adapter = mRecyclerAdapter
        mRecyclerView.layoutManager = LinearLayoutManager(this)  // default using LinearLayoutManager
        mRecyclerView.addItemDecoration(DividerItemDecoration(this, DividerItemDecoration.VERTICAL))
    }

    override fun onStart() {
        super.onStart()
        mViewModel.registerViewModel(this)
    }

    override fun onStop() {
        super.onStop()
        mViewModel.unRegisterViewModel(this)
    }

    // Update list using data from ViewModel
    override fun onDataChanged(tradeMap: HashMap<String, TradePair>) {
        val newMap: HashMap<String, TradePair> = HashMap()
        newMap.putAll(tradeMap)
        mTradeMap = newMap

        val newList = ArrayList<TradePair>()
        for (tradeItem in newMap) {
            newList.add(tradeItem.value)
        }
        mTradePairList = newList

        mHandler.post {
            mRecyclerAdapter.notifyDataSetChanged()
        }
    }
}
