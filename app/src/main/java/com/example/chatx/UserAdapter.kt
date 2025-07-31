package com.example.chatx

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class UserAdapter(
    private val users: MutableList<User>,
    private val onUserClick: (User) -> Unit
) : RecyclerView.Adapter<UserAdapter.UserViewHolder>() {

    init { setHasStableIds(true) }

    inner class UserViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val nameText: TextView = view.findViewById(R.id.tvUserName)
        private val lastMsgText: TextView = view.findViewById(R.id.tvLastMessage)
        private val timeText: TextView = view.findViewById(R.id.tvTime)
        private val pendingText: TextView = view.findViewById(R.id.tvPendingMessages)

        fun bind(user: User) {
            val shownName = when {
                user.name.isNotBlank() -> user.name
                user.email.isNotBlank() -> user.email.substringBefore("@")
                else -> "User"
            }
            nameText.text = shownName

            if (user.lastMessage.isNotEmpty()) {
                lastMsgText.text = user.lastMessage
                lastMsgText.visibility = View.VISIBLE
            } else {
                lastMsgText.text = ""
                lastMsgText.visibility = View.GONE
            }

            if (user.lastMessageTime.isNotEmpty()) {
                timeText.text = user.lastMessageTime
                timeText.visibility = View.VISIBLE
            } else {
                timeText.text = ""
                timeText.visibility = View.GONE
            }

            if (user.pendingMessages > 0) {
                pendingText.text = user.pendingMessages.toString()
                pendingText.visibility = View.VISIBLE
            } else {
                pendingText.visibility = View.GONE
            }

            itemView.setOnClickListener { onUserClick(user) }
        }
    }

    override fun getItemId(position: Int): Long = users[position].uid.hashCode().toLong()
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_user_chat, parent, false)
        return UserViewHolder(v)
    }
    override fun onBindViewHolder(holder: UserViewHolder, position: Int) = holder.bind(users[position])
    override fun getItemCount(): Int = users.size

    fun updateList(newList: List<User>) {
        users.clear(); users.addAll(newList); notifyDataSetChanged()
    }
    fun updatePending(uid: String, pending: Int) {
        val i = users.indexOfFirst { it.uid == uid }; if (i >= 0) { users[i].pendingMessages = pending; notifyItemChanged(i) }
    }
    fun updateLastMessage(uid: String, lastMessage: String) {
        val i = users.indexOfFirst { it.uid == uid }; if (i >= 0) { users[i].lastMessage = lastMessage; notifyItemChanged(i) }
    }
    fun updateTime(uid: String, timeTextValue: String) {
        val i = users.indexOfFirst { it.uid == uid }; if (i >= 0) { users[i].lastMessageTime = timeTextValue; notifyItemChanged(i) }
    }
}
