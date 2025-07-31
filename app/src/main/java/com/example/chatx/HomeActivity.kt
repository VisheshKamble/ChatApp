package com.example.chatx

import android.app.Dialog
import android.content.Intent
import androidx.appcompat.widget.SearchView
import android.graphics.Color
import android.os.Bundle
import android.view.*
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.children
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.chatx.databinding.ActivityHomeBinding
import com.example.chatx.databinding.LogoutDialogBinding
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.*
import java.text.SimpleDateFormat
import java.util.*

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

        // Enforce fullscreen mode and remove status bar
        window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_FULLSCREEN
                )
        window.setFlags(
            android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN,
            android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
        window.addFlags(android.view.WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
        window.statusBarColor = android.graphics.Color.TRANSPARENT

        // Ensure content extends to the top
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            view.setPadding(0, 0, 0, 0) // Remove any default padding
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

        binding.userRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.userRecyclerView.adapter = userAdapter

        setupSearchView()
        listenUsers()

        binding.logoutMenu.setOnClickListener { showLogoutDialog() }
    }

    private fun setupSearchView() {
        val sv = binding.searchView

        sv.setBackgroundResource(R.drawable.search_background)

        sv.isIconified = true // Start iconified like WhatsApp/Instagram
        sv.queryHint = "Search"
        sv.setIconifiedByDefault(true)

        // Remove underline
        val plateId = sv.context.resources.getIdentifier("android:id/search_plate", null, null)
        sv.findViewById<View>(plateId)?.setBackgroundColor(Color.TRANSPARENT)

        // Customize search icon and close button
        val magId = sv.context.resources.getIdentifier("android:id/search_mag_icon", null, null)
        sv.findViewById<ImageView>(magId)?.setColorFilter(ContextCompat.getColor(this, R.color.gray_700))

        val closeId = sv.context.resources.getIdentifier("android:id/search_close_btn", null, null)
        sv.findViewById<ImageView>(closeId)?.setColorFilter(ContextCompat.getColor(this, R.color.gray_700))

        val textId = sv.context.resources.getIdentifier("android:id/search_src_text", null, null)
        sv.findViewById<EditText>(textId)?.apply {
            setHintTextColor(ContextCompat.getColor(this@HomeActivity, R.color.gray_500))
            setTextColor(ContextCompat.getColor(this@HomeActivity, R.color.black))
            textSize = 16f
        }

        sv.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                currentQuery = query.orEmpty()
                filterUsers(currentQuery)
                sv.clearFocus() // Hide keyboard after submit
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                currentQuery = newText.orEmpty()
                filterUsers(currentQuery)
                return true
            }
        })

        // Expand search view on click and show keyboard only when focused
        sv.setOnClickListener {
            sv.isIconified = false
            sv.requestFocus()
        }

        // Hide keyboard when search view is iconified
        sv.setOnCloseListener {
            sv.clearFocus()
            false
        }
    }

    private fun filterUsers(query: String) {
        displayUsers.clear()
        if (query.isBlank()) {
            displayUsers.addAll(allUsers)
        } else {
            val q = query.trim().lowercase()
            displayUsers.addAll(allUsers.filter {
                it.name.lowercase().contains(q) || it.email.lowercase().contains(q)
            })
        }
        userAdapter.notifyDataSetChanged()
    }

    private fun listenUsers() {
        val myUid = auth.currentUser?.uid ?: return
        usersListener?.remove()

        usersListener = db.collection("users")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Toast.makeText(this, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                if (snapshot == null || snapshot.isEmpty) return@addSnapshotListener

                val seenUids = mutableSetOf<String>()
                val updated = ArrayList<User>()

                for (doc in snapshot) {
                    val uid = doc.getString("uid") ?: continue
                    if (uid == myUid) continue
                    seenUids.add(uid)

                    val name = (
                            doc.getString("name")
                                ?: doc.getString("username")
                                ?: doc.getString("email")?.substringBefore("@")
                                ?: "User"
                            ).ifBlank { "User" }

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
                filterUsers(currentQuery)
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

    private fun showLogoutDialog() {
        val dialog = Dialog(this)
        val dialogBinding = LogoutDialogBinding.inflate(layoutInflater)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(dialogBinding.root)
        dialog.setCancelable(true)

        dialog.window?.setLayout(
            resources.getDimensionPixelSize(R.dimen.dialog_fixed_width),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.window?.setGravity(Gravity.CENTER)

        // Close
        dialogBinding.closebtn.setOnClickListener {
            dialog.dismiss()
        }

        //cancel close
        dialogBinding.cancelbtn.setOnClickListener{
            dialog.dismiss()
        }

        // Grab the inner TextView inside logoutbtn to change its text
        val logoutTextView = dialogBinding.logoutbtn.findViewById(android.R.id.text1)
            ?: run {
                // Fallback: find first child that's a TextView
                dialogBinding.logoutbtn.children
                    .filterIsInstance<TextView>()
                    .firstOrNull()
            }

        var isLoggingOut = false
        dialogBinding.logoutbtn.setOnClickListener {
            if (isLoggingOut) return@setOnClickListener
            isLoggingOut = true

            // Show loading state
            logoutTextView?.let {
                it.text = "Logging out..."
            }
            dialogBinding.logoutbtn.isEnabled = false

            // Perform sign out
            FirebaseAuth.getInstance().signOut()

            // Navigate to MainActivity (clearing back stack)
            startActivity(Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TASK
            })
            finish()

            dialog.dismiss()
        }

        dialog.show()
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
