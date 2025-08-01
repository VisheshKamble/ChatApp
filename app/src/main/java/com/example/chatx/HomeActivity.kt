package com.example.chatx

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.chatx.databinding.ActivityHomeBinding
import com.example.chatx.databinding.LogoutDialogBinding
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.set

class HomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHomeBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private val allUsers = ArrayList<User>()
    private val displayUsers = ArrayList<User>()
    private lateinit var userAdapter: UserAdapter

    private var usersListener: ListenerRegistration? = null
    private val unseenRegs = mutableMapOf<String, ListenerRegistration>()
    private val lastARegs = mutableMapOf<String, ListenerRegistration>()
    private val lastBRegs = mutableMapOf<String, ListenerRegistration>()
    private var currentQuery: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        window.statusBarColor = Color.TRANSPARENT
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            v.setPadding(0, 0, 0, 0)
            insets
        }

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        userAdapter = UserAdapter(displayUsers) { user ->
            startActivity(Intent(this, ChatActivity::class.java).apply {
                putExtra("uid", user.uid)
                putExtra("name", user.name)
            })
        }

        binding.userRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@HomeActivity)
            adapter = userAdapter
            setHasFixedSize(true)
        }

        binding.swipeRefresh.setOnRefreshListener {
            listenUsers(reload = true)
        }

        setupSearchView()
        listenUsers()

        binding.logoutMenu.setOnClickListener {
            showLogoutDialog()
        }
    }

    private fun showLogoutDialog() {
        val dialogBinding = LogoutDialogBinding.inflate(LayoutInflater.from(this))
        val dialog = AlertDialog.Builder(this)
            .setView(dialogBinding.root)
            .setCancelable(false)
            .create()

        dialogBinding.closebtn.setOnClickListener { dialog.dismiss() }
        dialogBinding.cancelbtn.setOnClickListener { dialog.dismiss() }
        dialogBinding.logoutbtn.setOnClickListener {
            dialog.dismiss()
            performLogout()
        }

        dialog.show()
    }

    private fun setupSearchView() {
        val sv = binding.searchView
        sv.isIconified = false
        sv.queryHint = "Search"
        sv.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                currentQuery = query.orEmpty()
                filterUsers()
                sv.clearFocus()
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                currentQuery = newText.orEmpty()
                filterUsers()
                return true
            }
        })
    }

    private fun filterUsers() {
        displayUsers.clear()
        if (currentQuery.isBlank()) {
            displayUsers.addAll(allUsers)
        } else {
            val q = currentQuery.trim().lowercase(Locale.getDefault())
            displayUsers.addAll(allUsers.filter {
                it.name.lowercase(Locale.getDefault()).contains(q) ||
                        it.email.lowercase(Locale.getDefault()).contains(q)
            })
        }
        userAdapter.notifyDataSetChanged()
        binding.emptyState.visibility =
            if (displayUsers.isEmpty()) android.view.View.VISIBLE else android.view.View.GONE
    }

    private fun listenUsers(reload: Boolean = false) {
        val myUid = auth.currentUser?.uid ?: return
        if (reload) {
            usersListener?.remove()
            unseenRegs.values.forEach { it.remove() }
            lastARegs.values.forEach { it.remove() }
            lastBRegs.values.forEach { it.remove() }
            unseenRegs.clear(); lastARegs.clear(); lastBRegs.clear()
        }

        binding.swipeRefresh.isRefreshing = true

        usersListener = db.collection("users")
            .addSnapshotListener { snapshot, error ->
                binding.swipeRefresh.isRefreshing = false
                if (error != null || snapshot == null) return@addSnapshotListener

                val seenUids = mutableSetOf<String>()
                val updated = ArrayList<User>()

                for (doc in snapshot.documents) {
                    val uid = doc.getString("uid") ?: continue
                    if (uid == myUid) continue
                    seenUids.add(uid)

                    val name = (doc.getString("name") ?: doc.getString("username")
                    ?: doc.getString("email")?.substringBefore("@") ?: "User").ifBlank { "User" }

                    val email = (doc.getString("email") ?: "").trim()
                    val existing = allUsers.find { it.uid == uid }
                    val user = existing ?: User(uid, name, email)
                    user.name = name
                    user.email = email
                    updated.add(user)

                    attachUnseenListener(myUid, user)
                    attachLastMessageListeners(myUid, user)
                }

                (unseenRegs.keys - seenUids).forEach { unseenRegs.remove(it)?.remove() }
                (lastARegs.keys - seenUids).forEach { lastARegs.remove(it)?.remove() }
                (lastBRegs.keys - seenUids).forEach { lastBRegs.remove(it)?.remove() }

                allUsers.clear()
                allUsers.addAll(updated.distinctBy { it.uid })
                filterUsers()
            }
    }

    private fun attachUnseenListener(myUid: String, user: User) {
        unseenRegs[user.uid]?.remove()
        unseenRegs[user.uid] = db.collection("msg")
            .whereEqualTo("senderId", user.uid)
            .whereEqualTo("receiverId", myUid)
            .whereEqualTo("seen", false)
            .addSnapshotListener { qs, _ ->
                val count = qs?.size() ?: 0
                user.pendingMessages = count
                userAdapter.updatePending(user.uid, count)
            }
    }

    private fun attachLastMessageListeners(myUid: String, user: User) {
        lastARegs[user.uid]?.remove()
        lastBRegs[user.uid]?.remove()

        var latestA: DocumentSnapshot? = null
        var latestB: DocumentSnapshot? = null

        fun publish() {
            val latest = pickLatest(latestA, latestB)
            val lastMsg = latest?.getString("message").orEmpty()
            val timeText = formatTimeMillis(safeGetMillis(latest, "timestamp"))
            userAdapter.updateLastMessage(user.uid, lastMsg)
            userAdapter.updateTime(user.uid, timeText)
        }

        lastARegs[user.uid] = db.collection("msg")
            .whereEqualTo("senderId", user.uid)
            .whereEqualTo("receiverId", myUid)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(1)
            .addSnapshotListener { qs, _ ->
                latestA = qs?.documents?.firstOrNull()
                publish()
            }

        lastBRegs[user.uid] = db.collection("msg")
            .whereEqualTo("senderId", myUid)
            .whereEqualTo("receiverId", user.uid)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(1)
            .addSnapshotListener { qs, _ ->
                latestB = qs?.documents?.firstOrNull()
                publish()
            }
    }

    private fun pickLatest(d1: DocumentSnapshot?, d2: DocumentSnapshot?): DocumentSnapshot? {
        val t1 = safeGetMillis(d1, "timestamp")
        val t2 = safeGetMillis(d2, "timestamp")
        return when {
            t1 == 0L && t2 == 0L -> null
            t2 == 0L -> d1
            t1 == 0L -> d2
            else -> if (t1 >= t2) d1 else d2
        }
    }

    private fun safeGetMillis(doc: DocumentSnapshot?, field: String): Long {
        return when (val v = doc?.get(field)) {
            is Timestamp -> v.toDate().time
            is Long -> v
            is Number -> v.toLong()
            else -> 0L
        }
    }

    private fun formatTimeMillis(millis: Long): String {
        if (millis <= 0L) return ""
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        return sdf.format(Date(millis))
    }

    private fun performLogout() {
        FirebaseAuth.getInstance().signOut()
        startActivity(Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        })
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        usersListener?.remove()
        unseenRegs.values.forEach { it.remove() }
        lastARegs.values.forEach { it.remove() }
        lastBRegs.values.forEach { it.remove() }
        unseenRegs.clear(); lastARegs.clear(); lastBRegs.clear()
    }
}


