package com.samyak.urltvalpha

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.Typeface
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Bundle
import android.view.Menu
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
import com.samyak.urltvalpha.utils.ToolbarUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
        window.decorView.setBackgroundColor(resources.getColor(R.color.Red_light))

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

        // Fetch categories
        fetchCategories()

        // Set status bar color
        window.apply {
            clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
            addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            statusBarColor = resources.getColor(R.color.Red)
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

        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        // Set navigation view icon tint to red
        navigationView.itemIconTintList = ColorStateList.valueOf(resources.getColor(R.color.Red))

        // Handle navigation item clicks
        navigationView.setNavigationItemSelectedListener { menuItem ->
            drawerLayout.closeDrawer(GravityCompat.START)
            when (menuItem.itemId) {
                R.id.nav_home -> {
                    // Handle home click
                    Toast.makeText(this, "Home", Toast.LENGTH_SHORT).show()
                    true
                }
                R.id.nav_favorites -> {
                    // Handle favorites click
                    Toast.makeText(this, "Favorites", Toast.LENGTH_SHORT).show()
                    true
                }
                R.id.nav_settings -> {
                    // Handle settings click
                    Toast.makeText(this, "Settings", Toast.LENGTH_SHORT).show()
                    true
                }
                R.id.nav_rate -> {
                    rateApp()
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
                R.id.nav_contact -> {
                    contactUs()
                    true
                }
                R.id.nav_about -> {
                    startActivity(Intent(this, AboutActivity::class.java))
                    true
                }
                else -> false
            }
        }
    }

    private fun rateApp() {
        try {
            val uri = Uri.parse("market://details?id=$packageName")
            val intent = Intent(Intent.ACTION_VIEW, uri)
            intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY or Intent.FLAG_ACTIVITY_NEW_DOCUMENT or Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
            startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            val uri = Uri.parse("http://play.google.com/store/apps/details?id=$packageName")
            startActivity(Intent(Intent.ACTION_VIEW, uri))
        }
    }

    private fun shareApp() {
        val shareIntent = Intent(Intent.ACTION_SEND)
        shareIntent.type = "text/plain"
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.app_name))
        val shareMessage = "Check out this app: https://play.google.com/store/apps/details?id=$packageName"
        shareIntent.putExtra(Intent.EXTRA_TEXT, shareMessage)
        startActivity(Intent.createChooser(shareIntent, getString(R.string.share_message)))
    }

    private fun openPrivacyPolicy() {
        try {
            val intent = Intent(Intent.ACTION_VIEW)
            intent.data = Uri.parse("https://yourprivacypolicyurl.com")
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, getString(R.string.error_privacy_policy), Toast.LENGTH_SHORT).show()
        }
    }

    private fun contactUs() {
        try {
            val intent = Intent(Intent.ACTION_SENDTO)
            intent.data = Uri.parse("mailto:your-email@example.com")
            intent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.email_subject))
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, getString(R.string.error_no_email), Toast.LENGTH_SHORT).show()
        }
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
                filterCategories(newText)
                return true
            }
        })

        return true
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

    override fun onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        coroutineScope.cancel()
    }
}