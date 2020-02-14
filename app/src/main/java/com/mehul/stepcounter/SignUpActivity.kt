package com.mehul.stepcounter

import CommonUtility.CommonMethod
import Model.Users
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.preference.PreferenceManager
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.FirebaseDatabase.getInstance
import kotlinx.android.synthetic.main.activity_sign_up.*



class SignUpActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    lateinit var sp: SharedPreferences

    var mCurrentUser: FirebaseUser? = null
    private lateinit var mFirebaseDatabaseRef: DatabaseReference
    private lateinit var mFirebaseInstanceDB: FirebaseDatabase
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_up)

        val toolbar =
            findViewById<View>(R.id.toolbar) as Toolbar
        setSupportActionBar(toolbar)
        sp = PreferenceManager.getDefaultSharedPreferences(this)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        title = "Sign Up"

        auth = FirebaseAuth.getInstance()
       var dbR = getInstance().reference.child("Users")

        btnSignUp.setOnClickListener {

            if (CommonMethod.notEmptyEDT(edtSemail) && CommonMethod.notEmptyEDT(edtPassword)) {
                if (CommonMethod.isValidEmail(edtSemail)) {

                    progressBar.visibility = View.VISIBLE


                    auth.createUserWithEmailAndPassword(
                        edtSemail.text.toString(),
                        edtPassword.text.toString()
                    ).addOnCompleteListener(
                        this@SignUpActivity
                    ) { task ->
                        progressBar.visibility = View.GONE

                        if (task.isSuccessful) {
                            mCurrentUser= task.result?.user

                            mFirebaseDatabaseRef= dbR.child(this.mCurrentUser!!.uid)
                            val user = Users()
                            user.email = edtSemail.text.toString()
                            mFirebaseDatabaseRef.setValue(user)

                            sp.edit().putString("userId", this.mCurrentUser!!.uid).apply()
                            Toast.makeText(
                                this@SignUpActivity,
                                "SignUp Done Successfully",
                                Toast.LENGTH_LONG
                            ).show()

                            startActivity(
                                Intent(
                                    this@SignUpActivity,
                                    MainActivity::class.java
                                )
                            )
                            finish()
                        } else if (!task.isSuccessful) {


                            try {
                                throw task.exception!!
                            } // if user enters wrong email.
                            catch (existEmail: FirebaseAuthUserCollisionException) {
                                Toast.makeText(
                                    this@SignUpActivity,
                                    "Already registered",
                                    Toast.LENGTH_SHORT
                                ).show()
                            } catch (e: Exception) {
                                Toast.makeText(
                                    this@SignUpActivity,
                                    "Authentication failed." + task.exception,
                                    Toast.LENGTH_SHORT
                                ).show()
                            }

                        }
                    }

                }
            }

        }


    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                // todo: goto back activity from here
                val intent = Intent(this@SignUpActivity, MainActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                //startActivity(intent)
                //finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
