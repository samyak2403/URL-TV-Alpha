package com.samyak.urlplayerbeta

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.samyak.urltvalpha.R
import com.samyak.urltvalpha.models.Channel

class ChannelAdapter(
    private val channelList: List<Channel>,
    private val context: Context
) : RecyclerView.Adapter<ChannelAdapter.ChannelViewHolder>() {

    companion object {
        private const val URL_PLAYER_PACKAGE = "com.samyak.urlplayerbeta"
        private const val PLAYSTORE_URL = "https://play.google.com/store/apps/details?id=$URL_PLAYER_PACKAGE"
        private const val PLAYER_ACTIVITY = "com.samyak.urlplayerbeta.screen.PlayerActivity"
    }

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
            launchUrlPlayer(channel)
        }
    }

    private fun launchUrlPlayer(channel: Channel) {
        try {
            val intent = Intent().apply {
                setClassName(URL_PLAYER_PACKAGE, PLAYER_ACTIVITY)
                putExtra("videoUrl", channel.link)
                putExtra("videoTitle", channel.name)
                putExtra("videoType", "m3u8")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            AlertDialog.Builder(context)
                .setTitle("URL Player Beta Required")
                .setMessage("Please install URL Player Beta to play the channels")
                .setPositiveButton("Install") { _, _ ->
                    try {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(PLAYSTORE_URL)))
                    } catch (e: Exception) {
                        Toast.makeText(context, "Could not open Play Store", Toast.LENGTH_SHORT).show()
                    }
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    override fun getItemCount(): Int = channelList.size

    class ChannelViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvChannelName: TextView = itemView.findViewById(R.id.tvChannelName)
        val tvChannelLink: TextView = itemView.findViewById(R.id.tvChannelLink)
        val ivPlay: ImageView = itemView.findViewById(R.id.ivPlay)
    }
}