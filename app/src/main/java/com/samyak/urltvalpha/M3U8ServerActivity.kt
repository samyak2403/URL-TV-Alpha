package com.samyak.urltvalpha

import android.content.Context
import android.content.Intent
import android.graphics.Rect
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.WindowManager
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.*
import com.samyak.urltvalpha.databinding.ActivityM3U8ServerBinding
import com.samyak.urltvalpha.models.Channel
import com.samyak.urltvalpha.utils.ToolbarUtils
import kotlinx.coroutines.*

class M3U8ServerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityM3U8ServerBinding
    private lateinit var adapter: ChannelAdapter
    private lateinit var databaseReference: DatabaseReference
    private var selectedCategory: String? = null
    private var valueEventListener: ValueEventListener? = null
    
    // Use immutable list for better state management
    private val channelList = mutableListOf<Channel>()
    private val originalList = mutableListOf<Channel>()
    
    // Reference to loading layout
    private lateinit var loadingLayoutView: View

    companion object {
        private const val GRID_SPAN_COUNT = 3
        private const val CATEGORY_KEY = "category"
        private const val GRID_SPACING_DP = 8
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityM3U8ServerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Find loading layout view
        loadingLayoutView = findViewById(R.id.loadingLayout)

        initializeViews()
        setupRecyclerView()
        setupFirebase()
        setupStatusBar()
    }

    private fun initializeViews() {
        // Get selected category from intent
        selectedCategory = intent.getStringExtra(CATEGORY_KEY)
        
        // Setup toolbar with centered title
        ToolbarUtils.setupCenteredToolbar(
            this,
            binding.toolbar,
            selectedCategory ?: getString(R.string.app_name),
            true
        )

        // Set background color
        window.decorView.setBackgroundColor(ContextCompat.getColor(this, R.color.col_blue_2))
    }

    private fun setupRecyclerView() {
        // Use fixed span count of 3 as shown in the screenshot
        val spanCount = GRID_SPAN_COUNT
        
        // Get spacing dimension
        val spacing = resources.getDimensionPixelSize(R.dimen.grid_spacing)
        
        // Use GridLayoutManager with efficient configuration
        val layoutManager = GridLayoutManager(this, spanCount).apply {
            // Optimize span lookup
            spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
                override fun getSpanSize(position: Int): Int = 1
            }
        }
        
        binding.recyclerView.apply {
            this.layoutManager = layoutManager
            // Optimize RecyclerView performance
            setHasFixedSize(true)
            itemAnimator = DefaultItemAnimator()
            // Apply padding to match the UI in the image
            setPadding(spacing, spacing, spacing, spacing)
            clipToPadding = false
            // Add item decoration for grid spacing
            addItemDecoration(GridSpacingItemDecoration(spacing))
        }

        // Initialize adapter with efficient update strategy
        adapter = ChannelAdapter(channelList, this)
        binding.recyclerView.adapter = adapter
    }
    
    private fun setupFirebase() {
        // Initialize Firebase with optimized reference
        databaseReference = FirebaseDatabase.getInstance().reference.child("channels")
        
        // Enable disk persistence for offline capability if not already enabled
        try {
            FirebaseDatabase.getInstance().setPersistenceEnabled(true)
        } catch (e: Exception) {
            // Already enabled, ignore
        }
        
        // Keep synced for offline access
        databaseReference.keepSynced(true)
        
        // Fetch data if network is available
        if (isNetworkAvailable()) {
            fetchChannels()
        } else {
            showError(getString(R.string.no_internet_connection))
        }
    }

    private fun setupStatusBar() {
        window.apply {
            clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
            addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            statusBarColor = ContextCompat.getColor(this@M3U8ServerActivity, R.color.col_blue_header)
        }
    }

    private inner class GridSpacingItemDecoration(private val spacing: Int) : RecyclerView.ItemDecoration() {
        override fun getItemOffsets(
            outRect: Rect,
            view: View,
            parent: RecyclerView,
            state: RecyclerView.State
        ) {
            val position = parent.getChildAdapterPosition(view)
            if (position < 0) return // Invalid position
            
            // Apply equal spacing to all sides of each item
            // This creates a uniform grid with equal spacing between items
            outRect.left = spacing / 2
            outRect.right = spacing / 2
            outRect.top = spacing / 2
            outRect.bottom = spacing / 2
        }
    }

    private fun showLoading() {
        loadingLayoutView.isVisible = true
        binding.recyclerView.isVisible = false
        binding.lottieAnimationViewServerDown.isVisible = false
    }

    private fun hideLoading() {
        loadingLayoutView.isVisible = false
        binding.recyclerView.isVisible = true
    }

    private fun showError(message: String) {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.error_title))
            .setMessage(message)
            .setPositiveButton(getString(R.string.retry)) { _, _ -> fetchChannels() }
            .setNegativeButton(getString(R.string.cancel)) { _, _ -> finish() }
            .setCancelable(false)
            .show()
    }

    private fun fetchChannels() {
        showLoading()
        
        // Remove previous listener if exists to prevent memory leaks
        valueEventListener?.let { databaseReference.removeEventListener(it) }
        
        valueEventListener = object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                lifecycleScope.launch(Dispatchers.IO) {
                    try {
                        val tempList = mutableListOf<Channel>()
                        
                        // Process data in background thread
                        for (postSnapshot in dataSnapshot.children) {
                            postSnapshot.getValue(Channel::class.java)?.let { channel ->
                                if (channel.category.equals(selectedCategory, ignoreCase = true)) {
                                    tempList.add(channel)
                                }
                            }
                        }
                        
                        // Sort by name
                        tempList.sortBy { it.name }
                        
                        // Update UI on main thread
                        withContext(Dispatchers.Main) {
                            updateChannelList(tempList)
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            hideLoading()
                            showError(getString(R.string.error_loading_channels_with_message, e.message))
                        }
                    }
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                hideLoading()
                showError(getString(R.string.database_error, databaseError.message))
            }
        }
        
        // Add the listener
        valueEventListener?.let { databaseReference.addValueEventListener(it) }
    }
    
    private fun updateChannelList(newList: List<Channel>) {
        // Calculate diff to optimize RecyclerView updates
        val diffCallback = object : DiffUtil.Callback() {
            override fun getOldListSize(): Int = channelList.size
            override fun getNewListSize(): Int = newList.size
            
            override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                return channelList[oldItemPosition].id == newList[newItemPosition].id
            }
            
            override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                return channelList[oldItemPosition] == newList[newItemPosition]
            }
        }
        
        val diffResult = DiffUtil.calculateDiff(diffCallback)
        
        // Update data
        originalList.clear()
        originalList.addAll(newList)
        
        channelList.clear()
        channelList.addAll(newList)
        
        // Apply changes
        diffResult.dispatchUpdatesTo(adapter)
        
        hideLoading()
        checkDataAvailability()
    }

    private fun checkDataAvailability() {
        if (channelList.isEmpty()) {
            binding.recyclerView.isVisible = false
            showServerDownAnimation()
        } else {
            binding.recyclerView.isVisible = true
            hideServerDownAnimation()
        }
    }

    private fun showServerDownAnimation() {
        binding.lottieAnimationViewServerDown.apply {
            isVisible = true
            playAnimation()
        }
    }

    private fun hideServerDownAnimation() {
        binding.lottieAnimationViewServerDown.apply {
            isVisible = false
            cancelAnimation()
        }
    }

    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        
        return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
               capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
               capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                true
            }
            R.id.action_search_activity -> {
                // Launch the SearchActivity
                val intent = Intent(this, SearchActivity::class.java)
                startActivity(intent)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.search_menu, menu)
        return true
    }
    
    override fun onDestroy() {
        super.onDestroy()
        // Clean up resources and listeners
        valueEventListener?.let { databaseReference.removeEventListener(it) }
        binding.lottieAnimationViewServerDown.cancelAnimation()
    }
}