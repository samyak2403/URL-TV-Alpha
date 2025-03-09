package com.samyak.urltvalpha

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.Typeface
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.WindowManager
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.navigation.NavigationView
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.samyak.urltvalpha.models.Category
import android.content.res.ColorStateList
import androidx.recyclerview.widget.DividerItemDecoration
import com.samyak.urltvalpha.utils.LinkUtils
import com.samyak.urltvalpha.utils.ToolbarUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.os.Handler
import android.os.Looper

class MainActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var categoryAdapter: CategoryAdapter
    private lateinit var databaseReference: DatabaseReference
    private lateinit var loadingLayout: View
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var toolbar: Toolbar
    private val categoryList = mutableListOf<Category>()
    private var originalList = mutableListOf<Category>()
    
    // Add coroutine scope for background operations
    private val coroutineScope = CoroutineScope(Dispatchers.Main + Job())
    
    // Network callback
    private lateinit var connectivityManager: ConnectivityManager
    private lateinit var networkCallback: ConnectivityManager.NetworkCallback
    private var isNetworkCallbackRegistered = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Setup toolbar
        toolbar = findViewById(R.id.toolbar)
        ToolbarUtils.setupCenteredToolbar(this, toolbar, getString(R.string.app_name), false)

        // Setup drawer layout
        setupNavigationDrawer(toolbar)

        // Initialize views
        loadingLayout = findViewById(R.id.loadingLayout)
        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        // Set background color
        window.decorView.setBackgroundColor(resources.getColor(R.color.col_blue_2))

        // Initialize Firebase
        databaseReference = FirebaseDatabase.getInstance().getReference("categories")

        // Setup adapter
        categoryAdapter = CategoryAdapter(categoryList, this)
        recyclerView.adapter = categoryAdapter

        // Optimize RecyclerView
        recyclerView.apply {
            setHasFixedSize(true)
            itemAnimator = null // Disable animations for better performance
            layoutManager = LinearLayoutManager(this@MainActivity)
            addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
            
            // Add ViewHolder pooling
            recycledViewPool.setMaxRecycledViews(0, 20)
        }

        // Initialize connectivity manager
        connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        
        // Register network callback
        registerNetworkCallback()
        
        // Check network connectivity
        checkNetworkConnectivity()

        // Fetch categories
        fetchCategories()

        // Set status bar color
        window.apply {
            clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
            addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            statusBarColor = resources.getColor(R.color.col_blue_header)
        }
    }

    private fun setupNavigationDrawer(toolbar: Toolbar) {
        drawerLayout = findViewById(R.id.drawer_layout)
        val navigationView = findViewById<NavigationView>(R.id.nav_view)

        // Create ActionBarDrawerToggle
        val toggle = ActionBarDrawerToggle(
            this, drawerLayout, toolbar,
            R.string.navigation_drawer_open,
            R.string.navigation_drawer_close
        )

        // Set the drawer toggle color to red
        toggle.drawerArrowDrawable.color = resources.getColor(R.color.white)

        // Add animation to drawer opening/closing
        drawerLayout.addDrawerListener(object : DrawerLayout.DrawerListener {
            override fun onDrawerSlide(drawerView: View, slideOffset: Float) {
                // Scale the main content when drawer is opened
                val slideX = drawerView.width * slideOffset
                findViewById<View>(R.id.app_bar_layout).translationX = slideX / 2
            }

            override fun onDrawerOpened(drawerView: View) {
                // Animate the logo when drawer is fully opened
                val logo = drawerView.findViewById<ImageView>(R.id.imageView)
                logo?.let {
                    it.animate()
                        .scaleX(1.1f)
                        .scaleY(1.1f)
                        .setDuration(200)
                        .start()
                }
            }

            override fun onDrawerClosed(drawerView: View) {
                // Reset logo animation when drawer is closed
                val logo = drawerView.findViewById<ImageView>(R.id.imageView)
                logo?.let {
                    it.animate()
                        .scaleX(1.0f)
                        .scaleY(1.0f)
                        .setDuration(200)
                        .start()
                }
            }

            override fun onDrawerStateChanged(newState: Int) {
                // Not needed for our animation
            }
        })

        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        // Set navigation view background color to red
        navigationView.setBackgroundColor(resources.getColor(R.color.col_blue_2))
        
        // Set navigation view text color to white
        val navigationViewMenu = navigationView.menu
        for (i in 0 until navigationViewMenu.size()) {
            val menuItem = navigationViewMenu.getItem(i)
            val subMenu = menuItem.subMenu
            if (subMenu != null) {
                for (j in 0 until subMenu.size()) {
                    val subMenuItem = subMenu.getItem(j)
                    subMenuItem.title = SpannableString(subMenuItem.title).apply {
                        setSpan(ForegroundColorSpan(Color.WHITE), 0, length, 0)
                    }
                }
            }
            menuItem.title = SpannableString(menuItem.title).apply {
                setSpan(ForegroundColorSpan(Color.WHITE), 0, length, 0)
            }
        }

        // Set navigation view icon tint to white
        navigationView.itemIconTintList = ColorStateList.valueOf(resources.getColor(R.color.white))

        // Handle navigation item clicks
        navigationView.setNavigationItemSelectedListener { menuItem ->
            // Add a small delay to allow for the ripple effect to be visible
            Handler(Looper.getMainLooper()).postDelayed({
                drawerLayout.closeDrawer(GravityCompat.START)
            }, 300)
            
            when (menuItem.itemId) {
                R.id.nav_contact -> {
                    contactUs()
                    true
                }
                R.id.nav_messenger -> {
                    LinkUtils.openMessenger(this)
                    true
                }
                R.id.nav_facebook -> {
                    LinkUtils.openFacebook(this)
                    true
                }
                R.id.nav_instagram -> {
                    LinkUtils.openInstagram(this)
                    true
                }
                R.id.nav_youtube -> {
                    LinkUtils.openYouTube(this)
                    true
                }
                R.id.nav_twitter -> {
                    LinkUtils.openTwitter(this)
                    true
                }
                R.id.nav_telegram -> {
                    LinkUtils.openTelegram(this)
                    true
                }
                R.id.nav_share -> {
                    shareApp()
                    true
                }
                R.id.nav_privacy -> {
                    openPrivacyPolicy()
                    true
                }
                R.id.nav_about -> {
                    // Launch AboutActivity
                    val intent = Intent(this, AboutActivity::class.java)
                    startActivity(intent)
                    true
                }
                R.id.nav_linkedin -> {
                    LinkUtils.openLinkedIn(this)
                    true
                }
                else -> false
            }
        }
    }

    private fun rateApp() {
        try {
            val uri = Uri.parse(LinkUtils.getMarketUrl(packageName))
            val intent = Intent(Intent.ACTION_VIEW, uri)
            intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY or Intent.FLAG_ACTIVITY_NEW_DOCUMENT or Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
            startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            LinkUtils.openInBrowser(this, LinkUtils.getPlayStoreUrl(packageName))
        }
    }

    private fun shareApp() {
        val shareIntent = Intent(Intent.ACTION_SEND)
        shareIntent.type = "text/plain"
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.app_name))
        val shareMessage = "Check out this app: ${LinkUtils.getPlayStoreUrl(packageName)}"
        shareIntent.putExtra(Intent.EXTRA_TEXT, shareMessage)
        startActivity(Intent.createChooser(shareIntent, getString(R.string.share_message)))
    }

    private fun openPrivacyPolicy() {
        LinkUtils.openInBrowser(this, LinkUtils.PRIVACY_POLICY_URL)
    }

    private fun contactUs() {
        try {
            val intent = Intent(Intent.ACTION_SENDTO)
            intent.data = Uri.parse(LinkUtils.getMailtoUrl())
            intent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.email_subject))
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, getString(R.string.error_no_email), Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_search_activity -> {
                // Check network connectivity before launching SearchActivity
                if (!isNetworkAvailable()) {
                    Toast.makeText(
                        this,
                        "No internet connection. Search may not work properly.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                // Launch the SearchActivity
                val intent = Intent(this, SearchActivity::class.java)
                startActivity(intent)
                true
            }
            android.R.id.home -> {
                drawerLayout.openDrawer(GravityCompat.START)
                true
            }
            else -> super.onOptionsItemSelected(item)
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
            .setPositiveButton("Retry") { _, _ -> fetchCategories() }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun filterCategories(query: String?) {
        coroutineScope.launch(Dispatchers.Default) {
            val filteredList = if (query.isNullOrBlank()) {
                originalList.toList()
            } else {
                originalList.filter {
                    it.name.contains(query, ignoreCase = true)
                }
            }
            
            withContext(Dispatchers.Main) {
                categoryList.clear()
                categoryList.addAll(filteredList)
                categoryAdapter.notifyDataSetChanged()
            }
        }
    }

    private fun fetchCategories() {
        showLoading()
        
        // Check for network connectivity
        if (!isNetworkAvailable()) {
            hideLoading()
            showError("No internet connection. Please check your network settings and try again.")
            return
        }
        
        // Add keepSynced for offline capability
        databaseReference.keepSynced(true)
        
        // Optimize query to fetch only needed fields
        databaseReference.orderByChild("name")
            .addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                coroutineScope.launch(Dispatchers.Default) {
                    try {
                        val newCategories = mutableListOf<Category>()
                        val uniqueCategories = mutableSetOf<Category>()

                        for (categorySnapshot in snapshot.children) {
                            val category = categorySnapshot.getValue(Category::class.java)
                            category?.let {
                                if (!uniqueCategories.contains(it)) {
                                    uniqueCategories.add(it)
                                    newCategories.add(it)
                                }
                            }
                        }
                        
                        // Sort categories alphabetically by name
                        newCategories.sortBy { it.name.lowercase() }

                        withContext(Dispatchers.Main) {
                            if (newCategories.isEmpty()) {
                                showError("No categories found")
                            } else {
                                originalList.clear()
                                categoryList.clear()
                                originalList.addAll(newCategories)
                                categoryList.addAll(newCategories)
                                categoryAdapter.notifyDataSetChanged()
                            }
                            hideLoading()
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            hideLoading()
                            showError("Error loading categories: ${e.message}")
                        }
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                hideLoading()
                showError("Database error: ${error.message}")
            }
        })
    }

    // Add network check
    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork
        val capabilities = connectivityManager.getNetworkCapabilities(network)
        return capabilities != null && (
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
                )
    }

    private fun checkNetworkConnectivity() {
        if (!isNetworkAvailable()) {
            // Show a toast message
            Toast.makeText(
                this,
                "No internet connection. Some features may not work properly.",
                Toast.LENGTH_LONG
            ).show()
            
            // You could also use a Snackbar for a more modern UI
            // Snackbar.make(findViewById(android.R.id.content), 
            //     "No internet connection. Some features may not work properly.", 
            //     Snackbar.LENGTH_LONG).show()
        }
    }

    private fun registerNetworkCallback() {
        if (isNetworkCallbackRegistered) return
        
        networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                // Network is available
                runOnUiThread {
                    Toast.makeText(
                        this@MainActivity,
                        "Network connection restored",
                        Toast.LENGTH_SHORT
                    ).show()
                    
                    // Refresh data if needed
                    if (categoryList.isEmpty()) {
                        fetchCategories()
                    }
                }
            }

            override fun onLost(network: Network) {
                // Network is lost
                runOnUiThread {
                    Toast.makeText(
                        this@MainActivity,
                        "Network connection lost",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }

        val networkRequest = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        connectivityManager.registerNetworkCallback(networkRequest, networkCallback)
        isNetworkCallbackRegistered = true
    }

    private fun unregisterNetworkCallback() {
        if (isNetworkCallbackRegistered) {
            try {
                connectivityManager.unregisterNetworkCallback(networkCallback)
                isNetworkCallbackRegistered = false
            } catch (e: Exception) {
                // Handle exception
            }
        }
    }

    override fun onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterNetworkCallback()
        coroutineScope.cancel()
    }
}