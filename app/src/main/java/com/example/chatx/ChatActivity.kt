package com.example.chatx

import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.chatx.databinding.ActivityChatBinding
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.*

class ChatActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChatBinding
    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var messageAdapter: MessageAdapter
    private val messageList = ArrayList<Message>()

    private var receiverId: String? = null
    private var receiverName: String? = null
    private var currentUserId: String? = null
    private var messageListener: ListenerRegistration? = null

    private lateinit var chatId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatBinding.inflate(LayoutInflater.from(this))
        setContentView(binding.root)

        // Make fullscreen immersive
        window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_FULLSCREEN
                )

        // Back button
        binding.backArrow.setOnClickListener { finish() }

        // Init Firebase
        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        currentUserId = auth.currentUser?.uid
        receiverId = intent.getStringExtra("uid")
        receiverName = intent.getStringExtra("name")
        binding.chatUserName.text = receiverName ?: "Chat"

        if (currentUserId == null || receiverId == null) {
            finish()
            return
        }

        // Create chat ID based on users
        chatId = getChatId(currentUserId!!, receiverId!!)

        // Setup RecyclerView
        messageAdapter = MessageAdapter(messageList, currentUserId!!)
        binding.chatRecyclerView.layoutManager = LinearLayoutManager(this).apply {
            stackFromEnd = true
        }
        binding.chatRecyclerView.adapter = messageAdapter

        setupInsets()
        setupSendMessage()
        setupMessageListener()
    }

    private fun setupInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val ime = insets.getInsets(WindowInsetsCompat.Type.ime())
            val bottom = maxOf(systemBars.bottom, ime.bottom)
            binding.main.updatePadding(top = systemBars.top, bottom = bottom)
            insets
        }
    }

    private fun setupSendMessage() {
        binding.btnSend.setOnClickListener {
            val text = binding.etMessage.text.toString().trim()
            if (TextUtils.isEmpty(text)) return@setOnClickListener

            // Optimistic local insert
            val optimistic = Message(
                senderId = currentUserId!!,
                receiverId = receiverId!!,
                message = text,
                timestamp = System.currentTimeMillis(),
                seen = false
            )
            messageList.add(optimistic)
            messageAdapter.notifyItemInserted(messageList.size - 1)
            binding.chatRecyclerView.scrollToPosition(messageList.size - 1)
            binding.etMessage.setText("")

            // Firestore write
            val data = hashMapOf(
                "chatId" to chatId,
                "senderId" to currentUserId,
                "receiverId" to receiverId,
                "message" to text,
                "seen" to false,
                "timestamp" to Timestamp.now()
            )

            db.collection("msg")
                .add(data)
                .addOnFailureListener { e ->
                    Log.e("ChatActivity", "Send failed: ${e.message}", e)
                    Toast.makeText(this, "Send failed: ${e.localizedMessage}", Toast.LENGTH_SHORT)
                        .show()
                }
        }
    }

    private fun setupMessageListener() {
        messageListener = db.collection("msg")
            .whereEqualTo("chatId", chatId)
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snap, err ->
                if (err != null) {
                    Log.e("ChatListener", "Error: ${err.message}", err)
                    Toast.makeText(this, err.message ?: "Listener error", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }
                if (snap == null) return@addSnapshotListener

                val newList = ArrayList<Message>(snap.size())
                for (doc in snap.documents) {
                    val senderId = doc.getString("senderId") ?: continue
                    val receiverId = doc.getString("receiverId") ?: ""
                    val text = doc.getString("message") ?: ""
                    val seen = doc.getBoolean("seen") ?: false
                    val millis = when (val v = doc.get("timestamp")) {
                        is Timestamp -> v.toDate().time
                        is Long -> v
                        is Number -> v.toLong()
                        else -> 0L
                    }

                    val m = Message(
                        senderId = senderId,
                        receiverId = receiverId,
                        message = text,
                        timestamp = millis,
                        seen = seen
                    )

                    // Mark incoming as seen
                    if (m.receiverId == currentUserId && !seen) {
                        doc.reference.update("seen", true)
                    }

                    newList.add(m)
                }

                messageList.clear()
                messageList.addAll(newList)
                messageAdapter.notifyDataSetChanged()
                binding.chatRecyclerView.scrollToPosition(messageList.size - 1)
            }
    }

    private fun getChatId(uid1: String, uid2: String): String =
        if (uid1 < uid2) "${uid1}_${uid2}" else "${uid2}_${uid1}"

    override fun onDestroy() {
        super.onDestroy()
        messageListener?.remove()
    }
}
