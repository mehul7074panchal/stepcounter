package com.mehul.stepcounter

import CommonUtility.CommonMethod
import Model.Users
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.preference.PreferenceManager
import android.util.Log
import android.view.View
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.facebook.*
import com.facebook.appevents.AppEventsLogger
import com.facebook.login.LoginManager
import com.facebook.login.LoginResult
import com.facebook.login.widget.LoginButton
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FacebookAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.database.*
import kotlinx.android.synthetic.main.activity_main.*
import java.text.SimpleDateFormat
import java.util.*


class MainActivity : AppCompatActivity() {
    var callbackManager: CallbackManager? = null

    private val RC_SIGN_IN = 234
    private lateinit var loginButton: LoginButton
    lateinit var facebook:ImageButton
    private lateinit var auth: FirebaseAuth
    lateinit var sp: SharedPreferences
    var mCurrentUser: FirebaseUser? = null

    private lateinit var mFirebaseDatabaseRef: DatabaseReference

    lateinit var mGoogleSignInClient: GoogleSignInClient
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        sp = PreferenceManager.getDefaultSharedPreferences(this)
        auth = FirebaseAuth.getInstance()
        if (this.auth.currentUser != null) {
            finish()
            if(sp.getBoolean("proSet",false))
                startActivity(Intent(this@MainActivity, HomeActivity::class.java))
            else
                startActivity(Intent(this@MainActivity, ProfileActivity::class.java))

            return


        }
        FacebookSdk.sdkInitialize(applicationContext)
        AppEventsLogger.activateApp(application)
        callbackManager = CallbackManager.Factory.create()
        loginButton = findViewById(R.id.login_button1)
        //  loginButton.setReadPermissions("email");
        loginButton.setReadPermissions(
            listOf(
                "public_profile", "email", "user_birthday", "user_location"
            )
        )


        facebook = findViewById(R.id.login_button_fb)
        val gso =
            GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build()


        mGoogleSignInClient = GoogleSignIn.getClient(this, gso)



        this.loginButton.registerCallback(callbackManager, object :
            FacebookCallback<LoginResult> {
            override fun onSuccess(loginResult: LoginResult) {
                handleFacebookAccessToken(loginResult.accessToken)
            }

            override fun onCancel() { // App code
            }

            override fun onError(exception: FacebookException) { // App code
              //  CommonMethod.showToast(this@LoginActivity, "Check your internet")
            }
        })




        btnLogin.setOnClickListener {
            if (CommonMethod.notEmptyEDT(edtLmail) && CommonMethod.notEmptyEDT(edtLpw)) {
                if (CommonMethod.isValidEmail(edtLmail)) {

                    pdL.visibility = View.VISIBLE
                    var dbR = FirebaseDatabase.getInstance().reference.child("Users")

                    auth.signInWithEmailAndPassword(edtLmail.text.toString(), edtLpw.text.toString())
                        .addOnCompleteListener(
                            this@MainActivity
                        ) { task ->
                            // If sign in fails, display a message to the user. If sign in succeeds
                            // the auth state listener will be notified and logic to handle the
                            // signed in user can be handled in the listener.
                            pdL.visibility = View.GONE
                            if (!task.isSuccessful) { // there was an error
                                if (edtLpw.text.toString().length < 6) {
                                    edtLpw.error = getString(R.string.minimum_password)
                                } else {
                                    Toast.makeText(
                                        this@MainActivity,
                                        getString(R.string.auth_failed),
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            } else {

                                val user: FirebaseUser? = auth.currentUser
                                if (user != null) {
                                    mCurrentUser= task.result?.user
                                    sp.edit().putString("userId", this.mCurrentUser!!.uid).apply()


                                }


                                if(sp.getBoolean("proSet",false))
                                    startActivity(Intent(this@MainActivity, HomeActivity::class.java))
                                else
                                    startActivity(Intent(this@MainActivity, ProfileActivity::class.java))

                                finish()

                            }
                        }
                }
            }
        }

        txtSignUp.setOnClickListener {
            startActivity(
                Intent(
                    this@MainActivity,
                    SignUpActivity::class.java
                )
            )
        }

        btnforgot.setOnClickListener {startActivity(Intent(this@MainActivity, ForgotPasswordActivity::class.java))
        }

        btn_g_sign_in.setOnClickListener { signIn() }


        this.facebook.setOnClickListener {
            LoginManager.getInstance().logInWithReadPermissions(
                this,
                listOf(
                    "public_profile",
                    "email",
                    "user_birthday",
                    "user_location"
                )
            )
        }

    }

    override fun onStart() {
        super.onStart()

        if (auth.currentUser != null) {
            finish()
            startActivity(Intent(this, MainActivity::class.java))
        }
    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        callbackManager?.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RC_SIGN_IN) {
            val task =
                GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account =
                    task.getResult(ApiException::class.java)
                //authenticating with firebase
                if (account != null) {
                    firebaseAuthWithGoogle(account)
                }
            } catch (e: ApiException) {
                Toast.makeText(this@MainActivity, e.message, Toast.LENGTH_SHORT).show()
            }
        }
    }
    private fun firebaseAuthWithGoogle(acct: GoogleSignInAccount) {

        val dbR = FirebaseDatabase.getInstance().reference.child("Users")

        val credential = GoogleAuthProvider.getCredential(acct.idToken, null)
        //Now using firebase we are signing in the user here
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this
            ) { task ->
                if (task.isSuccessful) {

                    val user: FirebaseUser? = auth.currentUser
                    if (user != null) {
                        val isNew =
                            task.result!!.additionalUserInfo.isNewUser
                        mCurrentUser = task.result?.user
                        if(isNew) {


                            mFirebaseDatabaseRef = dbR.child(this.mCurrentUser!!.uid)
                            val usr = Users()
                            usr.email = user.email
                            usr.name = user.displayName
                            usr.profile = user.photoUrl.toString()


                            mFirebaseDatabaseRef.setValue(usr)
                        }
                        sp.edit().putString("userId", this.mCurrentUser!!.uid).apply()


                      finish()
                        if(sp.getBoolean("proSet",false))
                            startActivity(Intent(this@MainActivity, HomeActivity::class.java))
                        else
                            startActivity(Intent(this@MainActivity, ProfileActivity::class.java))

                        // finish()
                        Toast.makeText(this@MainActivity, "User Signed In "+user.displayName, Toast.LENGTH_SHORT)
                            .show()
                    }
                } else {
                    Toast.makeText(
                        this@MainActivity, "Authentication failed.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                // ...
            }
    }

    private fun handleFacebookAccessToken(token: AccessToken) {

        val dbR = FirebaseDatabase.getInstance().reference.child("Users")

        val credential = FacebookAuthProvider.getCredential(token.token)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Sign in success, update UI with the signed-in user's information

                    val user = auth.currentUser
                    if (user != null) {
                        val isNew =
                            task.result!!.additionalUserInfo.isNewUser
                        mCurrentUser = task.result?.user
                        if(isNew) {


                            mFirebaseDatabaseRef = dbR.child(this.mCurrentUser!!.uid)
                            val usr = Users()
                            usr.email = user.email
                            usr.name = user.displayName
                            usr.profile = user.photoUrl.toString()

                            mFirebaseDatabaseRef.setValue(usr)
                        }

                        sp.edit().putString("userId", this.mCurrentUser!!.uid).apply()


                        Toast.makeText(
                            this@MainActivity,
                            "User Signed In " + user.displayName,
                            Toast.LENGTH_SHORT
                        )
                            .show()

                        finish()
                        if (sp.getBoolean("proSet", false))
                            startActivity(Intent(this@MainActivity, HomeActivity::class.java))
                        else
                            startActivity(Intent(this@MainActivity, ProfileActivity::class.java))

                        // finish()
                    }

                } else {
                    // If sign in fails, display a message to the user.

                    Toast.makeText(baseContext, "Authentication failed.",
                        Toast.LENGTH_SHORT).show()

                }

                // ...
            }
    }
    private fun signIn() {
        val signInIntent = this.mGoogleSignInClient.signInIntent
        startActivityForResult(signInIntent, RC_SIGN_IN)
    }

}

