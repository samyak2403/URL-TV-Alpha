package com.samyak.urltvalpha

import android.content.Context
import android.graphics.Rect
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.MenuItem
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.ImageView
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
import com.samyak.urltvalpha.databinding.ActivitySearchBinding
import com.samyak.urltvalpha.models.Channel
import com.samyak.urltvalpha.utils.ToolbarUtils
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import java.util.concurrent.TimeUnit

class SearchActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySearchBinding
    private lateinit var adapter: ChannelAdapter
    private lateinit var databaseReference: DatabaseReference
    private var valueEventListener: ValueEventListener? = null
    
    // Use StateFlow for search query to enable debouncing
    private val searchQueryFlow = MutableStateFlow("")
    
    // Use immutable list for better state management
    private val channelList = mutableListOf<Channel>()
    private val originalList = mutableListOf<Channel>()
    
    // Coroutine job for search to enable cancellation
    private var searchJob: Job? = null
    
    // Search UI elements
    private lateinit var searchEditText: EditText
    private lateinit var clearSearchIcon: ImageView

    companion object {
        private const val GRID_SPAN_COUNT = 3
        private const val SEARCH_DEBOUNCE_MS = 300L
        private const val GRID_SPACING_DP = 8
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySearchBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Find search UI elements
        searchEditText = findViewById(R.id.searchEditText)
        clearSearchIcon = findViewById(R.id.clearSearchIcon)

        initializeViews()
        setupRecyclerView()
        setupSearchListener()
        
        // Setup Firebase and load channels in background but don't display them initially
        setupFirebase()
        
        setupStatusBar()
    }

    private fun initializeViews() {
        // Setup toolbar with centered title
        ToolbarUtils.setupCenteredToolbar(
            this,
            binding.toolbar,
            getString(R.string.search_activity_title),
            true
        )

        // Set background color
        window.decorView.setBackgroundColor(ContextCompat.getColor(this, R.color.Red_light))
        
        // Setup search edit text
        searchEditText.hint = getString(R.string.search_hint)
        
        // Setup clear button
        clearSearchIcon.setOnClickListener {
            searchEditText.text.clear()
            clearSearchIcon.visibility = View.GONE
            filterChannels("")  // Show all channels when search is cleared
        }
        
        // Setup search edit text action listener
        searchEditText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                val query = searchEditText.text.toString().trim()
                searchQueryFlow.value = query
                return@setOnEditorActionListener true
            }
            false
        }
        
        // Add text change listener to show/hide clear button and trigger search
        searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                clearSearchIcon.visibility = if (s.isNullOrEmpty()) View.GONE else View.VISIBLE
                searchQueryFlow.value = s?.toString()?.trim() ?: ""
            }
            
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun setupRecyclerView() {
        // Use fixed span count of 3 as shown in the screenshot
        val spanCount = GRID_SPAN_COUNT
        
        // Get spacing dimension
        val spacing = resources.getDimensionPixelSize(R.dimen.search_grid_spacing)
        
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
            
            // Set initial visibility to GONE since we want to show empty RecyclerView by default
            visibility = View.GONE
        }

        // Initialize adapter with efficient update strategy
        adapter = ChannelAdapter(channelList, this)
        binding.recyclerView.adapter = adapter
    }
    
    private fun setupSearchListener() {
        // Setup debounced search using Flow
        lifecycleScope.launch {
            searchQueryFlow
                .debounce(SEARCH_DEBOUNCE_MS)
                .distinctUntilChanged()
                .flowOn(Dispatchers.Default)
                .collect { query ->
                    filterChannels(query)
                }
        }
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
            fetchChannelsInBackground()
        } else {
            // Just show a toast instead of error dialog for background fetch
            binding.recyclerView.visibility = View.GONE
            hideServerDownAnimation()
        }
    }

    private fun setupStatusBar() {
        window.apply {
            clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
            addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            statusBarColor = ContextCompat.getColor(this@SearchActivity, R.color.Red)
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
        binding.loadingLayout.root.visibility = View.VISIBLE
        binding.recyclerView.visibility = View.GONE
        binding.lottieAnimationViewServerDown.visibility = View.GONE
    }

    private fun hideLoading() {
        binding.loadingLayout.root.visibility = View.GONE
        binding.recyclerView.visibility = View.VISIBLE
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
                                tempList.add(channel)
                            }
                        }
                        
                        // Sort by name
                        tempList.sortBy { it.name }
                        
                        // Update UI on main thread
                        withContext(Dispatchers.Main) {
                            if (tempList.isNotEmpty()) {
                                updateChannelList(tempList)
                                // Only show RecyclerView if there's an active search
                                val currentQuery = searchEditText.text.toString().trim()
                                binding.recyclerView.visibility = if (currentQuery.isNotEmpty()) View.VISIBLE else View.GONE
                                hideServerDownAnimation()
                            } else {
                                binding.recyclerView.visibility = View.GONE
                                showServerDownAnimation()
                            }
                            hideLoading()
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
    
    // New method to fetch channels in background without showing them initially
    private fun fetchChannelsInBackground() {
        // Don't show loading indicator for initial background fetch
        
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
                                tempList.add(channel)
                            }
                        }
                        
                        // Sort by name
                        tempList.sortBy { it.name }
                        
                        // Update data but keep RecyclerView hidden
                        withContext(Dispatchers.Main) {
                            if (tempList.isNotEmpty()) {
                                // Update the original list but don't show in RecyclerView yet
                                originalList.clear()
                                originalList.addAll(tempList)
                                
                                // Keep RecyclerView hidden until search is performed
                                binding.recyclerView.visibility = View.GONE
                                hideServerDownAnimation()
                            } else {
                                binding.recyclerView.visibility = View.GONE
                                showServerDownAnimation()
                            }
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            showError(getString(R.string.error_loading_channels_with_message, e.message))
                        }
                    }
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
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
        
        // Update UI state based on data availability
        checkDataAvailability()
    }

    private fun checkDataAvailability() {
        // Only check if there's an active search
        val currentQuery = searchEditText.text.toString().trim()
        
        if (channelList.isEmpty()) {
            binding.recyclerView.visibility = View.GONE
            if (currentQuery.isNotEmpty()) {
                // Only show animation if there's an active search with no results
                showServerDownAnimation()
            } else {
                hideServerDownAnimation()
            }
        } else {
            // Only show results if there's an active search
            binding.recyclerView.visibility = if (currentQuery.isNotEmpty()) View.VISIBLE else View.GONE
            hideServerDownAnimation()
        }
    }

    private fun showServerDownAnimation() {
        binding.lottieAnimationViewServerDown.apply {
            visibility = View.VISIBLE
            playAnimation()
        }
    }

    private fun hideServerDownAnimation() {
        binding.lottieAnimationViewServerDown.apply {
            visibility = View.GONE
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

    private fun filterChannels(query: String) {
        // Cancel previous search job if exists
        searchJob?.cancel()
        
        // Show loading indicator for better UX during search
        if (query.isNotBlank()) {
            binding.loadingLayout.root.visibility = View.VISIBLE
        }
        
        searchJob = lifecycleScope.launch(Dispatchers.Default) {
            val filteredList = if (query.isBlank()) {
                // When search is empty, show empty list instead of all channels
                emptyList()
            } else {
                // Improved search logic with more comprehensive matching
                originalList.filter { channel ->
                    channel.name?.contains(query, ignoreCase = true) == true ||
                    channel.category?.contains(query, ignoreCase = true) == true
                }
            }
            
            // Small delay to prevent UI flickering for fast typing
            delay(100)
            
            withContext(Dispatchers.Main) {
                // Hide loading indicator
                binding.loadingLayout.root.visibility = View.GONE
                
                // Update the channel list with filtered results
                val diffCallback = object : DiffUtil.Callback() {
                    override fun getOldListSize(): Int = channelList.size
                    override fun getNewListSize(): Int = filteredList.size
                    
                    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                        return if (oldItemPosition < channelList.size && newItemPosition < filteredList.size) {
                            channelList[oldItemPosition].id == filteredList[newItemPosition].id
                        } else {
                            false
                        }
                    }
                    
                    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                        return if (oldItemPosition < channelList.size && newItemPosition < filteredList.size) {
                            channelList[oldItemPosition] == filteredList[newItemPosition]
                        } else {
                            false
                        }
                    }
                }
                
                val diffResult = DiffUtil.calculateDiff(diffCallback)
                
                // Update data
                channelList.clear()
                channelList.addAll(filteredList)
                
                // Apply changes
                diffResult.dispatchUpdatesTo(adapter)
                
                // Update UI state based on search results
                if (filteredList.isEmpty()) {
                    // No results found or empty search
                    if (query.isBlank()) {
                        // Empty search, hide RecyclerView
                        binding.recyclerView.visibility = View.GONE
                        hideServerDownAnimation() // Don't show error animation for empty search
                    } else {
                        // No results for search query
                        binding.recyclerView.visibility = View.GONE
                        showServerDownAnimation() // Show "no results" animation
                    }
                } else {
                    // Results found
                    binding.recyclerView.visibility = View.VISIBLE
                    hideServerDownAnimation()
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Clean up resources and listeners
        valueEventListener?.let { databaseReference.removeEventListener(it) }
        binding.lottieAnimationViewServerDown.cancelAnimation()
        
        // Cancel all coroutines
        searchJob?.cancel()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
} 