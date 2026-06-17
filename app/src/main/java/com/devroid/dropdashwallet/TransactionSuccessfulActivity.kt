package com.devroid.dropdashwallet

import android.os.Bundle
import com.devroid.dropdashwallet.databinding.ActivityTransactionSuccessfulBinding

class TransactionSuccessfulActivity : BaseActivity() {
    private lateinit var binding: ActivityTransactionSuccessfulBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTransactionSuccessfulBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }
}