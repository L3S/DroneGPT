package com.l3s.dronegpt.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.l3s.dronegpt.data.database.ChatContent
import dji.sampleV5.aircraft.R


class ChatContentAdapter(val context : Context, private val dataSet : List<ChatContent>) : RecyclerView.Adapter<ChatContentAdapter.ViewHolder>() {
    companion object {
        private const val Gpt = 1
        private const val User = 2
    }

    interface ViewCodeClick {
        fun onLongClick(view : View, position: Int)
    }
    var viewCodeClick : ViewCodeClick? = null

    inner class ViewHolder(view : View) : RecyclerView.ViewHolder(view) {
        val contentTV : TextView = view.findViewById(R.id.rvItemTV)
        val delChatLayout : ConstraintLayout = view.findViewById(R.id.chatLayout)
        val idHolder : TextView = view.findViewById(R.id.holdingId)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        if (viewType == Gpt) {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.dronegpt_gpt_content_item, parent, false)
            return ViewHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.dronegpt_user_content_item, parent, false)
            return ViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.contentTV.text = dataSet[position].content
        holder.idHolder.text = dataSet[position].id.toString()

        holder.delChatLayout.setOnLongClickListener { view ->
            viewCodeClick?.onLongClick(view, position)
            return@setOnLongClickListener true
        }

    }

    override fun getItemCount(): Int {
        return dataSet.size
    }

    override fun getItemViewType(position: Int): Int {
        return if (dataSet[position].isUserContent) {
            User
        } else {
            Gpt
        }
    }
}