package com.mehul.stepcounter

import CommonUtility.CommonMethod
import android.content.Intent
import android.os.Bundle

import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.google.firebase.auth.FirebaseAuth
import kotlinx.android.synthetic.main.activity_forgot_password.*


class ForgotPasswordActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_forgot_password)
        auth = FirebaseAuth.getInstance()

        val toolbar =
            findViewById<View>(R.id.toolbar) as Toolbar
        setSupportActionBar(toolbar)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        title = "Forgot Password"

        btnResetPass.setOnClickListener {
            if (CommonMethod.notEmptyEDT(edtFEmail)) {
                if (CommonMethod.isValidEmail(edtFEmail)) {

                    pbF.visibility = View.VISIBLE

                    auth.sendPasswordResetEmail(edtFEmail.text.toString())
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                Toast.makeText(
                                    this@ForgotPasswordActivity,
                                    "We have sent you instructions to reset your password!",
                                    Toast.LENGTH_LONG
                                ).show()
                            } else {
                                Toast.makeText(
                                    this@ForgotPasswordActivity,
                                    "Failed to send reset email!",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                            pbF.visibility = View.GONE
                        }
                }
            }
        }
    }
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                // todo: goto back activity from here
                val intent = Intent(this@ForgotPasswordActivity, MainActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

}
