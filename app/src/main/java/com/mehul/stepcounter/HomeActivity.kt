package com.mehul.stepcounter

import CommonUtility.CommonMethod
import Model.Users
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.preference.PreferenceManager
import android.text.TextUtils
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.google.firebase.database.*
import kotlinx.android.synthetic.main.activity_home.*
import java.text.SimpleDateFormat
import java.util.*


class HomeActivity : AppCompatActivity(), SensorEventListener {

    private lateinit var mFirebaseDatabase: DatabaseReference
    private lateinit var mFirebaseInstance: FirebaseDatabase
    var running = false
    var sensorManager: SensorManager? = null
    var cal = Calendar.getInstance()
    private var userId: String = ""
    val myFormat = "dd-MM-yyyy" // mention the format you need
    val sdf = SimpleDateFormat(myFormat, Locale.US)
    lateinit var sp: SharedPreferences
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        mFirebaseInstance = FirebaseDatabase.getInstance()
        sp = PreferenceManager.getDefaultSharedPreferences(this)
        mFirebaseDatabase = mFirebaseInstance.getReference("Users")
        val toolbar =
            findViewById<View>(R.id.toolbar) as Toolbar
        setSupportActionBar(toolbar)

        title = "Home"
        if (!TextUtils.isEmpty(sp.getString("userId", ""))) {
            userId = sp.getString("userId", "")



        }


    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menu.add(0, 0, 0, "Profile").setIcon(R.drawable.person)
            .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM)

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            0 -> { startActivity(Intent(this,ProfileActivity::class.java))
                true}

            else -> super.onOptionsItemSelected(item)
        }
    }
    override fun onResume() {
        super.onResume()
        running = true
        if (CommonMethod.isMyServiceRunning(StepService::class.java, this))
            stopService(Intent(this, StepService::class.java))
        val stepsSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)

        if (stepsSensor == null) {
            Toast.makeText(this, "No Step Counter Sensor !", Toast.LENGTH_SHORT).show()
        } else {
            sensorManager?.registerListener(this, stepsSensor, SensorManager.SENSOR_DELAY_UI)
        }
    }

    override fun onStop() {
        super.onStop()
        startService(Intent(this, StepService::class.java))

    }

    override fun onPause() {
        super.onPause()
        running = false
        sensorManager?.unregisterListener(this)
        //if (!CommonMethod.isMyServiceRunning(StepService::class.java, this))
            startService(Intent(this, StepService::class.java))
    }

    override fun onAccuracyChanged(p0: Sensor?, p1: Int) {
    }

    @SuppressLint("SetTextI18n")
    override fun onSensorChanged(event: SensorEvent) {
        if (running) {

            val steps : Int

            if (sp.getString("tDT", "").isEmpty()) {
                sp.edit().putString("tDT", sdf.format(cal.time)).putFloat("tStep", event.values[0])
                    .apply()
            }
            if (sp.getString("tDT", "") != sdf.format(cal.time)) {
                sp.edit().putString("tDT", sdf.format(cal.time)).putFloat("tStep", event.values[0])
                    .apply()
            }

            // tvP_DOB.text = sdf.format(cal.time)
            if (sp.getFloat("tStep", 0f) == 0f) {
                tvStep.text = "0"
                tvCal.text = "0"
                steps = 0
            } else {
                tvStep.text = "" + (event.values[0] - sp.getFloat("tStep", 0f))
                tvCal.text = String.format("%.1f", (event.values[0] - sp.getFloat("tStep", 0f)) * 0.4)
                steps = (event.values[0] - sp.getFloat("tStep", 0f)).toInt()
            }
            val u = Users()
            u.totalSteps = steps
            updateUser(u)
        }
    }

    private fun updateUser(users: Users) {

        if (!TextUtils.isEmpty(users.totalSteps.toString())) mFirebaseDatabase.child(userId).child("totalSteps").setValue(
            users.totalSteps
        )


    }
}
