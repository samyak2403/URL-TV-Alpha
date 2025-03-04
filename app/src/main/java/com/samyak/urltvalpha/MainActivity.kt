package com.samyak.urltvalpha

import android.content.Context
import android.graphics.Color
import android.graphics.PorterDuff
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.view.Menu
import android.view.View
import android.view.WindowManager
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.samyak.urltvalpha.models.Category

class MainActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var categoryAdapter: CategoryAdapter
    private lateinit var databaseReference: DatabaseReference
    private lateinit var loadingLayout: View
    private val categoryList = mutableListOf<Category>()
    private var originalList = mutableListOf<Category>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Setup toolbar
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.title = "Yacine TV"

        // Initialize views
        loadingLayout = findViewById(R.id.loadingLayout)
        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        // Set background color
        window.decorView.setBackgroundColor(resources.getColor(R.color.bein_red))

        // Initialize Firebase
        databaseReference = FirebaseDatabase.getInstance().getReference("categories")
        
        // Setup adapter
        categoryAdapter = CategoryAdapter(categoryList, this)
        recyclerView.adapter = categoryAdapter

        // Fetch categories
        fetchCategories()

        // Set status bar color
        window.apply {
            clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
            addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            statusBarColor = resources.getColor(R.color.bein_red)
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
        if (query.isNullOrBlank()) {
            categoryList.clear()
            categoryList.addAll(originalList)
        } else {
            val filteredList = originalList.filter {
                it.name.contains(query, ignoreCase = true)
            }
            categoryList.clear()
            categoryList.addAll(filteredList)
        }
        categoryAdapter.notifyDataSetChanged()
    }

    private fun fetchCategories() {
        showLoading()
        databaseReference.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                try {
                    categoryList.clear()
                    originalList.clear()
                    val uniqueCategories = mutableSetOf<Category>()

                    for (categorySnapshot in snapshot.children) {
                        val category = categorySnapshot.getValue(Category::class.java)
                        category?.let {
                            if (!uniqueCategories.contains(it)) {
                                uniqueCategories.add(it)
                                originalList.add(it)
                            }
                        }
                    }

                    if (originalList.isEmpty()) {
                        showError("No categories found")
                    } else {
                        originalList.sortBy { it.name }
                        categoryList.addAll(originalList)
                        categoryAdapter.notifyDataSetChanged()
                    }
                } catch (e: Exception) {
                    showError("Error loading categories: ${e.message}")
                } finally {
                    hideLoading()
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
}