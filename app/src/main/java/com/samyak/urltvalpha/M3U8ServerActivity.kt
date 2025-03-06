package com.samyak.urltvalpha

import android.graphics.Rect
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.view.WindowManager
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ItemDecoration
import com.airbnb.lottie.LottieAnimationView
import com.google.firebase.database.*
import com.samyak.urltvalpha.models.Channel
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.view.Menu
import androidx.appcompat.widget.SearchView
import android.graphics.Color
import android.graphics.PorterDuff
import android.widget.EditText
import android.widget.ImageView

class M3U8ServerActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ChannelAdapter
    private lateinit var channelList: MutableList<Channel>
    private lateinit var databaseReference: DatabaseReference
    private lateinit var lottieAnimationViewServerDown: LottieAnimationView
    private var selectedCategory: String? = null
    private lateinit var loadingLayout: View
    private var originalList = mutableListOf<Channel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_m3_u8_server)

        // Initialize views
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
        }

        // Get selected category from intent
        selectedCategory = intent.getStringExtra("category")
        
        // Update title
        supportActionBar?.title = selectedCategory

        // Initialize RecyclerView with GridLayoutManager
        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = GridLayoutManager(this, 3) // 3 columns
        
        // Add item decoration for grid spacing
        recyclerView.addItemDecoration(object : ItemDecoration() {
            override fun getItemOffsets(
                outRect: Rect,
                view: View,
                parent: RecyclerView,
                state: RecyclerView.State
            ) {
                val spacing = resources.getDimensionPixelSize(R.dimen.grid_spacing)
                outRect.left = spacing
                outRect.right = spacing
                outRect.top = spacing
                outRect.bottom = spacing
            }
        })

        // Initialize other views
        lottieAnimationViewServerDown = findViewById(R.id.lottieAnimationViewServerDown)
        loadingLayout = findViewById(R.id.loadingLayout)

        // Initialize list and adapter
        channelList = ArrayList()
        adapter = ChannelAdapter(channelList, this)
        recyclerView.adapter = adapter

        // Set background color
        window.decorView.setBackgroundColor(resources.getColor(R.color.Red_light))

        // Initialize Firebase
        databaseReference = FirebaseDatabase.getInstance().getReference("channels")
        
        // Fetch data
        fetchChannels()

        // Set status bar color
        window.apply {
            clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
            addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            statusBarColor = resources.getColor(R.color.Red)
        }
    }

    private fun showLoading() {
        loadingLayout.visibility = View.VISIBLE
        recyclerView.visibility = View.GONE
    }

    private fun hideLoading() {
        loadingLayout.visibility = View.GONE
        recyclerView.visibility = View.VISIBLE
    }

    private fun showError(message: String) {
        AlertDialog.Builder(this)
            .setTitle("Error")
            .setMessage(message)
            .setPositiveButton("Retry") { _, _ -> fetchChannels() }
            .setNegativeButton("Cancel") { _, _ -> finish() }
            .show()
    }

    private fun fetchChannels() {
        if (!isNetworkAvailable()) {
            showError("No internet connection")
            return
        }

        showLoading()
        databaseReference.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                try {
                    channelList.clear()
                    originalList.clear()
                    for (postSnapshot in dataSnapshot.children) {
                        postSnapshot.getValue(Channel::class.java)?.let { channel ->
                            if (channel.category.equals(selectedCategory, ignoreCase = true)) {
                                originalList.add(channel)
                            }
                        }
                    }

                    originalList.sortBy { it.name }
                    channelList.addAll(originalList)
                    adapter.notifyDataSetChanged()
                    
                    hideLoading()
                    checkDataAvailability()
                } catch (e: Exception) {
                    hideLoading()
                    showError("Error loading channels: ${e.message}")
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                hideLoading()
                showError("Database error: ${databaseError.message}")
            }
        })
    }

    private fun checkDataAvailability() {
        if (channelList.isEmpty()) {
            recyclerView.visibility = View.GONE
            showServerDownAnimation()
//            showEmptyMessage()
        } else {
            recyclerView.visibility = View.VISIBLE
            hideServerDownAnimation()
            hideEmptyMessage()
        }
    }

    private fun showServerDownAnimation() {
        lottieAnimationViewServerDown.visibility = View.VISIBLE
        lottieAnimationViewServerDown.playAnimation()
    }

    private fun hideServerDownAnimation() {
        lottieAnimationViewServerDown.visibility = View.GONE
        lottieAnimationViewServerDown.cancelAnimation()
    }

    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork
        val capabilities = connectivityManager.getNetworkCapabilities(network)
        return capabilities != null && (
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
        )
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

    override fun onBackPressed() {
        super.onBackPressed()
        // Optionally, you can add an animation or specific behavior on back press
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.search_menu, menu)
        
        val searchItem = menu.findItem(R.id.action_search)
        val searchView = searchItem.actionView as androidx.appcompat.widget.SearchView

        // Style the SearchView
        val searchIcon = searchView.findViewById<ImageView>(androidx.appcompat.R.id.search_mag_icon)
        searchIcon.setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN)

        val closeIcon = searchView.findViewById<ImageView>(androidx.appcompat.R.id.search_close_btn)
        closeIcon.setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN)

        val searchText = searchView.findViewById<EditText>(androidx.appcompat.R.id.search_src_text)
        searchText.setTextColor(Color.WHITE)
        searchText.setHintTextColor(Color.WHITE)
        
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                filterChannels(newText)
                return true
            }
        })
        
        return true
    }

    private fun filterChannels(query: String?) {
        if (query.isNullOrBlank()) {
            channelList.clear()
            channelList.addAll(originalList)
        } else {
            val filteredList = originalList.filter {
                it.name.contains(query, ignoreCase = true) ||
                it.category.contains(query, ignoreCase = true)
            }
            channelList.clear()
            channelList.addAll(filteredList)
        }
        adapter.notifyDataSetChanged()
        checkDataAvailability()
    }

    private fun showEmptyMessage() {
        val message = "No channels found for $selectedCategory"
        AlertDialog.Builder(this)
            .setTitle("No Results")
            .setMessage(message)
            .setPositiveButton("Retry") { _, _ -> fetchChannels() }
            .setNegativeButton("Back") { _, _ -> finish() }
            .setCancelable(false)
            .show()
    }

    private fun hideEmptyMessage() {
        // No need to do anything here, dialog will be auto-dismissed
    }
}