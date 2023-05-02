package com.usc.shapeshift

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.usc.shapeshift.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var database: DatabaseReference


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        firebaseAuth = FirebaseAuth.getInstance()
        binding.logOutButton.setOnClickListener {
            firebaseAuth.signOut()
            val intent = Intent(this, SignInActivity::class.java)
            startActivity(intent)
            Toast.makeText(this, "Logged out!", Toast.LENGTH_SHORT).show()
        }

        database = FirebaseDatabase.getInstance().getReference("Users");
        if (firebaseAuth.currentUser != null) {
            val name =
                database.child(firebaseAuth.currentUser!!.uid).get().addOnSuccessListener {
                    if (it.exists()) {
                        val name = it.child("name").value
                        binding.Name.text = name.toString()
                    } else {
                        Toast.makeText(this, "User Doesn't Exist", Toast.LENGTH_SHORT).show()
                    }
                }

        }
    }
}