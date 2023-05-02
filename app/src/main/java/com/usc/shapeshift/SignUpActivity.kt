package com.usc.shapeshift

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.usc.shapeshift.databinding.ActivitySignUpBinding

class SignUpActivity : AppCompatActivity() {

    private lateinit var binding : ActivitySignUpBinding
    private lateinit var firebaseAuth : FirebaseAuth
    private lateinit var database : DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivitySignUpBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firebaseAuth = FirebaseAuth.getInstance()

        binding.textView.setOnClickListener {
            val intent = Intent(this, SignInActivity::class.java)
            startActivity(intent)
        }
        binding.button.setOnClickListener {
            val name = binding.nameField.text.toString()
            val email = binding.emailField.text.toString()
            val pwd = binding.pwdField.text.toString()
            val confirmPwd = binding.confirmPwdField.text.toString()

            if (email.isNotEmpty() && pwd.isNotEmpty() && confirmPwd.isNotEmpty()) {
                if (pwd == confirmPwd) {
                    firebaseAuth.createUserWithEmailAndPassword(email, pwd).addOnCompleteListener {
                        if (it.isSuccessful) {
                            database = FirebaseDatabase.getInstance().getReference("Users")
                            val user = User(name)
                            firebaseAuth.currentUser?.let { it1 -> database.child(it1.uid).setValue(user) }

                            val intent = Intent(this, SignInActivity::class.java)
                            startActivity(intent)
                        } else {
                            Toast.makeText(this, it.exception.toString(), Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    Toast.makeText(this, "Passwords do not match.", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Please enter a password.", Toast.LENGTH_SHORT).show()
            }
        }
    }
}