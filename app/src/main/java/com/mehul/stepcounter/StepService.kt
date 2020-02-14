package com.mehul.stepcounter

import Model.Users
import android.app.IntentService
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.preference.PreferenceManager
import android.text.TextUtils
import android.widget.Toast
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import java.text.SimpleDateFormat
import java.util.*


class StepService : IntentService("StepService"), SensorEventListener {
    private lateinit var mSensorManager: SensorManager
    private lateinit var mStepDetectorSensor: Sensor
    lateinit var sp: SharedPreferences
    private lateinit var mFirebaseDatabase: DatabaseReference
    private lateinit var mFirebaseInstance: FirebaseDatabase
    var cal = Calendar.getInstance()
    private var userId: String = ""
    val myFormat = "dd-MM-yyyy" // mention the format you need
    val sdf = SimpleDateFormat(myFormat, Locale.US)
    override fun onCreate() {
        super.onCreate()

    }

    override fun onHandleIntent(intent: Intent?) {
        sp = PreferenceManager.getDefaultSharedPreferences(this)
        mFirebaseInstance = FirebaseDatabase.getInstance()
        mFirebaseDatabase = mFirebaseInstance.getReference("users")
        mSensorManager = this.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        if(mSensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR) != null)
        {
            mStepDetectorSensor =
                mSensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR);
            mSensorManager.registerListener(this, mStepDetectorSensor, SensorManager.SENSOR_DELAY_NORMAL);

        }
        if (!TextUtils.isEmpty(sp.getString("userId", ""))) {
            userId = sp.getString("userId", "")



        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {

    }

    override fun onSensorChanged(event: SensorEvent) {


        if (sp.getString("tDT", "").isEmpty()) {
            sp.edit().putString("tDT", sdf.format(cal.time)).putFloat("tStep", event.values[0])
                .apply()
        }
        if (sp.getString("tDT", "") != sdf.format(cal.time)) {
            sp.edit().putString("tDT", sdf.format(cal.time)).putFloat("tStep", event.values[0])
                .apply()
        }

        // tvP_DOB.text = sdf.format(cal.time)
        val steps : Int = if (sp.getFloat("tStep", 0f) == 0f) {

            0
        } else {
            (event.values[0] - sp.getFloat("tStep", 0f)).toInt()
        }
        val u = Users()
        u.totalSteps = steps
        updateUser(u)

    }

    private fun updateUser(users: Users) {

        if (!TextUtils.isEmpty(users.totalSteps.toString())) mFirebaseDatabase.child(userId).child("totalSteps").setValue(
            users.totalSteps
        )


    }
}
