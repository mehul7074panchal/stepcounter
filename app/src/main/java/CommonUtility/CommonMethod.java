package CommonUtility;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityManager;

import android.content.Context;

import android.graphics.Color;

import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import android.util.Log;

import android.view.View;

import android.view.inputmethod.InputMethodManager;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;


import org.json.JSONObject;

import java.lang.reflect.Field;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;


/**
 * Created by Mehul Panchal on 9/14/2015.
 */
public class CommonMethod {

    @SuppressLint("StaticFieldLeak")
    private static Context ctx;



    public static String zero(int num) {
        return (num < 10) ? ("0" + num) : ("" + num);

    }

    public static boolean notEmptyEDT(EditText edt) {
        String text = edt.getText().toString();

        if (text.trim().length() > 0) {
            // edt.setBackgroundResource(R.color.green);
            return true;

        } else {

            edt.setError("Field Cannot be empty.");
            edt.requestFocus();
            return false;
        }


    }

    public static boolean isValidEmail(EditText edt) {

        String email = edt.getText().toString();
        String emailPattern = "[a-zA-Z0-9._-]+@[a-z]+\\.+[a-z]+";
        if (email.matches(emailPattern)) {
            // edt.setBackgroundResource(R.color.green);
            return true;

        } else {

            edt.setError("Enter valid email address!!!");
            edt.requestFocus();
            return false;
        }
    }

    public static boolean notEmptySpinner(Spinner spinner) {

        String text = spinner.getSelectedItem().toString();

        if (text.trim().length() > 0) {
            // edt.setBackgroundResource(R.color.green);
            return true;

        } else {
            TextView errorText = (TextView) spinner.getSelectedView();
            errorText.setError("FIll THE FIELD!!!");
            errorText.setTextColor(Color.RED);
//          errorText.setText("FIll THE FIELD!!!");
            spinner.requestFocus();
            return false;
        }


    }

    public static boolean notEmptyAuto(AutoCompleteTextView completeTextView) {
        String text = completeTextView.getText().toString();

        if (text.trim().length() > 0) {
            // edt.setBackgroundResource(R.color.green);
            return true;

        } else {

            completeTextView.setError("FIll THE FIELD!!!");
            completeTextView.requestFocus();
            return false;
        }


    }

    public static boolean isTen(EditText edt) {
        if (edt.getText().toString().trim().length() == 10) {

            return true;
        } else {
            edt.setError("MUST BE 10 DIGIT");
            return false;
        }
    }

    public boolean isLessZero(EditText edt, Context ctx) {
        if (Integer.parseInt(edt.getText().toString()) <= 0) {
            edt.setError("MUST BE GRATER THAN 0");
            //  Toast.makeText(ctx, "MUST BE GRATER THAN",Toast.LENGTH_LONG).show();

            return true;
        } else {

            return false;
        }

    }

    public boolean isGrater(EditText edtFull, EditText edt) {
        if (Integer.parseInt(edtFull.getText().toString()) < Integer.parseInt(edt.getText().toString())) {
            edt.setError("MUST BE GRATER THAN 0");
            return false;
        } else {
            return true;
        }
    }

    public static boolean isOnline(Context ctx) {
        ConnectivityManager cm = (ConnectivityManager) ctx.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();

        if (netInfo != null && netInfo.isConnectedOrConnecting()) {
            return true;
        } else {
            return false;
        }

    }


    public static void JsonToBean(JSONObject jsonobj, Object obj) {
        for (Field field : obj.getClass().getDeclaredFields()) {
            try {
                if (field.getType() == int.class) {
                    field.setInt(obj, jsonobj.getInt(field.getName()));
                } else if (field.getType() == boolean.class) {
                    field.setBoolean(obj, jsonobj.getBoolean(field.getName()));
                } else if (field.getType() == double.class) {
                    field.setDouble(obj, jsonobj.getDouble(field.getName()));
                } else if (field.getType() == long.class) {
                    String data = jsonobj.getString(field.getName());
                    SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd#HH:mm");
                    Date dt = format.parse(data);
                    field.setLong(obj, dt.getTime());
                } else {
                    field.set(obj, jsonobj.getString(field.getName()));
                }
            } catch (Exception ee) {
                Log.e("Exception in reflection", ee.toString());
            }
        }
    }



    public static boolean isMyServiceRunning(Class<?> serviceClass, Context c) {
        ActivityManager manager = (ActivityManager) c.getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    @SuppressWarnings("ConstantConditions")
    public static void hideSoftKeyboard(Activity a) {
        View view = a.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) a.getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }

    }

    public static String convertAbsoluteTimeToRelative(Long time) {
        try {


            Date newdate = new Date();

            long oldmillis = time;
            long newmillis = newdate.getTime();

            long diff = newmillis - oldmillis;

            long sec = diff / 1000;
            if (sec < 60) {
                return "just now";
            }

            long min = sec / 60;
            if (min < 60) {
                return min + " mins ago";
            }

            long hours = min / 60;
            if (hours < 24) {
                return hours + " hours ago";
            }
            long days = hours / 24;
            if (days == 1) {
                return "yesterday";
            } else {
                return days + " days ago";
            }

        } catch (Exception ee) {

        }
        return "";
    }






}
