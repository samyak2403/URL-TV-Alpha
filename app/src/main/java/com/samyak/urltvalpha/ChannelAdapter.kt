package com.samyak.urltvalpha

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.samyak.urltvalpha.models.Channel


class ChannelAdapter(
    private val channelList: List<Channel>,
    private val context: Context
) : RecyclerView.Adapter<ChannelAdapter.ChannelViewHolder>() {

    private val glideOptions = RequestOptions()
        .diskCacheStrategy(DiskCacheStrategy.ALL)
        .placeholder(R.drawable.placeholder_channel)
        .error(R.drawable.placeholder_channel)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChannelViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_channel, parent, false)
        return ChannelViewHolder(view)
    }

    override fun onBindViewHolder(holder: ChannelViewHolder, position: Int) {
        val channel = channelList[position]
        
        holder.tvChannelName.text = channel.name
        holder.tvChannelLink.text = channel.link

        // Load channel logo if available
        if (channel.logo.isNotEmpty()) {
            Glide.with(context)
                .load(channel.logo)
                .apply(glideOptions)
                .into(holder.ivPlay)
        }

        holder.itemView.setOnClickListener {
            playUrl(channel)
        }
    }

    private fun playUrl(channel: Channel) {
        try {
            // Create an explicit intent to the URL Player Beta app
            val intent = Intent().apply {
                // Set the correct package and activity name
                setClassName("com.samyak.urlplayerbeta", "com.samyak.urlplayerbeta.screen.PlayerActivity")
                // Use the correct extra keys that the PlayerActivity expects
                putExtra("URL", channel.link)
                putExtra("CHANNEL_NAME", channel.name)
                putExtra("USER_AGENT", "URLPlayerBeta")
                // Add flags for launching from another app
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
            
            // Try to start the activity
            context.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
            // Show custom dialog to install URL Player Beta
            showCustomInstallDialog()
        }
    }
    
    private fun showCustomInstallDialog() {
        // Create custom dialog
        val dialog = android.app.Dialog(context)
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_install_player)
        dialog.window?.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))
        
        // Set dialog width to 90% of screen width
        val displayMetrics = context.resources.displayMetrics
        val width = (displayMetrics.widthPixels * 0.9).toInt()
        dialog.window?.setLayout(width, android.view.ViewGroup.LayoutParams.WRAP_CONTENT)
        
        // Set up buttons
        val btnInstall = dialog.findViewById<android.widget.Button>(R.id.btnInstall)
        val btnCancel = dialog.findViewById<android.widget.Button>(R.id.btnCancel)
        
        btnInstall.setOnClickListener {
            try {
                // Open Play Store to URL Player Beta app
                val playStoreIntent = Intent(Intent.ACTION_VIEW).apply {
                    data = Uri.parse("market://details?id=com.samyak.urlplayerbeta")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(playStoreIntent)
            } catch (e: Exception) {
                // If Play Store app is not available, open browser
                val webIntent = Intent(Intent.ACTION_VIEW).apply {
                    data = Uri.parse("https://play.google.com/store/apps/details?id=com.samyak.urlplayerbeta")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(webIntent)
            }
            dialog.dismiss()
        }
        
        btnCancel.setOnClickListener {
            dialog.dismiss()
        }
        
        dialog.show()
    }

    override fun getItemCount(): Int = channelList.size

    class ChannelViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvChannelName: TextView = itemView.findViewById(R.id.tvChannelName)
        val tvChannelLink: TextView = itemView.findViewById(R.id.tvChannelLink)
        val ivPlay: ImageView = itemView.findViewById(R.id.ivPlay)
    }
}