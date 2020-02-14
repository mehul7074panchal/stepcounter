package com.mehul.stepcounter

import CommonUtility.CommonMethod
import Model.Users
import android.Manifest.permission.*
import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.app.Activity
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.ProgressDialog
import android.content.*
import android.content.pm.PackageManager

import android.content.pm.ResolveInfo
import android.graphics.*
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Parcelable
import android.preference.PreferenceManager
import android.provider.MediaStore
import android.text.TextUtils
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.facebook.login.LoginManager
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.nostra13.universalimageloader.core.DisplayImageOptions
import com.nostra13.universalimageloader.core.ImageLoader
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration
import com.nostra13.universalimageloader.core.display.CircleBitmapDisplayer
import com.nostra13.universalimageloader.core.display.FadeInBitmapDisplayer
import com.nostra13.universalimageloader.core.listener.ImageLoadingListener
import com.nostra13.universalimageloader.core.listener.SimpleImageLoadingListener
import kotlinx.android.synthetic.main.activity_profile.*
import java.io.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt


open class ProfileActivity : AppCompatActivity() {


    private var time: String = ""
    private var Npath: String = ""
    private var Path: String = ""
    var picUri: Uri? = null
    private lateinit var mFirebaseDatabase: DatabaseReference
    private lateinit var mFirebaseInstance: FirebaseDatabase
    private var userId: String = ""
    private lateinit var auth: FirebaseAuth
    var fileName: String = ""
    private lateinit var myBitmap: Bitmap
    var loader: ImageLoader? = null
    private var options: DisplayImageOptions? = null
    private val animateFirstListener: ImageLoadingListener = AnimateFirstDisplayListener()

    private lateinit var permissionsToRequest: ArrayList<String>
    private var permissionsRejected = ArrayList<String>()
    private var permissions = ArrayList<String>()
    val ALL_PERMISSIONS_RESULT = 107
    val cal = Calendar.getInstance()
    lateinit var sp: SharedPreferences
    lateinit var storage: FirebaseStorage
    lateinit var storageReference: StorageReference
    lateinit var mGoogleSignInClient: GoogleSignInClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        permissions.add(CAMERA)
        permissions.add(READ_EXTERNAL_STORAGE)
        permissions.add(WRITE_EXTERNAL_STORAGE)
        permissionsToRequest = findUnAskedPermissions(permissions)

        val gso =
            GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build()
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso)
        val toolbar =
            findViewById<View>(R.id.toolbar) as Toolbar
        setSupportActionBar(toolbar)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        title = "Profile"

        sp = PreferenceManager.getDefaultSharedPreferences(this)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {


            if (permissionsToRequest.size > 0) {
                val array: Array<String?> = arrayOfNulls(permissionsToRequest.size)
                requestPermissions(permissionsToRequest.toArray(array), ALL_PERMISSIONS_RESULT)
            }
        }

        mFirebaseInstance = FirebaseDatabase.getInstance()
        auth = FirebaseAuth.getInstance()

        mFirebaseInstance.getReference("app_title").setValue("StepCounter")

        mFirebaseDatabase = mFirebaseInstance.getReference("Users")

        loader = ImageLoader.getInstance()
        initLoader(this)
        storage = FirebaseStorage.getInstance()
        storageReference = storage.reference

        if (!TextUtils.isEmpty(sp.getString("userId", ""))) {
            userId = sp.getString("userId", "")
            pbPro.visibility = View.VISIBLE
            ivProfile.visibility = View.VISIBLE

            mFirebaseDatabase.child(userId).addValueEventListener(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    val user: Users? =
                        dataSnapshot.getValue(
                            Users::class.java
                        )
                    if (user != null) {
                        edtP_Name.setText(if (user.name == null) "" else user.name.toString())
                        val myFormat = "dd-MM-yyyy" // mention the format you need
                        val sdf = SimpleDateFormat(myFormat, Locale.US)
                        if (user.DOB != null) {
                            tvP_DOB.text = sdf.format(user.DOB)
                        }

                        if (user.phone != null) {
                            edtP_Mobile.setText(user.phone.toString())
                        }

                        if (user.gender != null) {
                            if (user.gender.toString() == "Male") {
                                edtSemail.setSelection(0)
                            } else {
                                edtSemail.setSelection(1)
                            }
                        }
                        if (user.profile.toString().trim().isNotEmpty()) {
                            loader!!.displayImage(
                                user.profile.toString(),
                                ivProfile,
                                options,
                                animateFirstListener
                            )
                        }
                    }
                    pbPro.visibility = View.GONE

                }

                override fun onCancelled(error: DatabaseError) { // Failed to read value
                    Log.e(
                        "Profile Err",
                        "Failed to read value.",
                        error.toException()
                    )
                    pbPro.visibility = View.GONE
                }
            })
        } else {
            ivProfile.visibility = View.GONE
        }
        mFirebaseInstance.getReference("app_title")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {

                    val appTitle =
                        dataSnapshot.getValue(String::class.java)!!
                    // update toolbar title
                    supportActionBar!!.title = appTitle
                }

                override fun onCancelled(error: DatabaseError) { // Failed to read value
                    Log.e(
                        "Profile Err",
                        "Failed to read app title value.",
                        error.toException()
                    )
                }
            })

        val dateSetListener =
            DatePickerDialog.OnDateSetListener { view, year, monthOfYear, dayOfMonth ->
                cal.set(Calendar.YEAR, year)
                cal.set(Calendar.MONTH, monthOfYear)
                cal.set(Calendar.DAY_OF_MONTH, dayOfMonth)

                val myFormat = "dd-MM-yyyy" // mention the format you need
                val sdf = SimpleDateFormat(myFormat, Locale.US)
                tvP_DOB.text = sdf.format(cal.time)

            }

        tvP_DOB.setOnClickListener {
            DatePickerDialog(
                this@ProfileActivity, dateSetListener,
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        btnupdate.setOnClickListener {
            if (CommonMethod.notEmptyEDT(edtP_Name) && CommonMethod.notEmptyEDT(edtP_Mobile) && CommonMethod.notEmptySpinner(
                    edtSemail
                )
            ) {
                if (CommonMethod.isTen(edtP_Mobile)) {

                    pbPro.visibility = View.VISIBLE

                    val u = Users()
                    u.name = edtP_Name.text.toString()
                    u.phone = edtP_Mobile.text.toString()
                    val myFormat = "dd-MM-yyyy" // mention the format you need
                    val sdf = SimpleDateFormat(myFormat, Locale.US)
                    u.DOB = sdf.parse(tvP_DOB.text.toString())
                    u.gender = edtSemail.selectedItem.toString()
                    u.profile = ""
                    u.totalSteps = 0

                        updateUser(u)


                }
            }
        }

        toggleButton()





        ivProfile.setOnClickListener { startActivityForResult(getPickImageChooserIntent(), 200); }

        btnLogOut.setOnClickListener {
            auth.signOut()
            mGoogleSignInClient.signOut()
            mGoogleSignInClient.revokeAccess()
            LoginManager.getInstance().logOut();

            sp.edit().clear().apply()
            startActivity(Intent(this@ProfileActivity, MainActivity::class.java))
            finish()
        }

    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                // todo: goto back activity from here
                val intent = Intent(this@ProfileActivity, HomeActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                //startActivity(intent)
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun toggleButton() {
        if (TextUtils.isEmpty(userId)) {
            btnupdate.text = "Save"
        } else {
            btnupdate.text = "Update"
        }
        if (!sp.getBoolean("proSet", false)) {
            sp.edit().putBoolean("proSet", true).apply()
        }

    }


 /*   @SuppressLint("CommitPrefEdits")
    private fun createUser(users: Users) {
        if (TextUtils.isEmpty(userId)) {
            userId = mFirebaseDatabase.push().key.toString()
            sp.edit().putString("userId", userId).apply()
        }

        mFirebaseDatabase.child(userId).setValue(users)
        addUserChangeListener()
    }*/


    /* override fun onActivityResult(
        requestCode: Int,
        resultCode: Int, @Nullable data: Intent?
    ) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null && data.data != null) {
            val uri: Uri = data.data
            try {
                val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, uri)


                ivProfile.setImageBitmap(bitmap)
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }*/
    open fun getPickImageChooserIntent(): Intent? { // Determine Uri of camera image to save.
        val outputFileUri = getCaptureImageOutputUri()
        val allIntents: ArrayList<Any> = ArrayList()
        val packageManager = packageManager
        // collect all camera intents
        val captureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        val listCam: List<ResolveInfo> = packageManager.queryIntentActivities(captureIntent, 0)
        for (res in listCam) {
            val intent = Intent(captureIntent)
            intent.component = ComponentName(res.activityInfo.packageName, res.activityInfo.name)
            intent.setPackage(res.activityInfo.packageName)
            if (outputFileUri != null) {


                intent.putExtra(MediaStore.EXTRA_OUTPUT, outputFileUri)
            }
            allIntents.add(intent)
        }
        // collect all gallery intents
        val galleryIntent = Intent(Intent.ACTION_GET_CONTENT)
        galleryIntent.type = "image/*"
        val listGallery: List<ResolveInfo> = packageManager.queryIntentActivities(galleryIntent, 0)
        for (res in listGallery) {
            val intent = Intent(galleryIntent)
            intent.component = ComponentName(res.activityInfo.packageName, res.activityInfo.name)
            intent.setPackage(res.activityInfo.packageName)
            allIntents.add(intent)
        }
        // the main intent is the last in the list (fucking android) so pickup the useless one
        var mainIntent: Any = allIntents[allIntents.size - 1]
        /* for (intent in allIntents) {
           if (intent.className.equals("com.android.documentsui.DocumentsActivity")) {
               mainIntent = intent
               break
           }
       }
       allIntents.remove(mainIntent)*/
        // Create a chooser from the main intent
        val chooserIntent = Intent.createChooser(mainIntent as Intent?, "Select source")
        // Add all other intents
        chooserIntent.putExtra(
            Intent.EXTRA_INITIAL_INTENTS,
            allIntents.toArray(arrayOfNulls<Parcelable>(allIntents.size))
        )
        return chooserIntent
    }


    /**
     * Get URI to image received from capture by camera.
     */
    private fun getCaptureImageOutputUri(): Uri? {
        var outputFileUri: Uri? = null
        val d = Date()

        time = "" + d.time
        fileName = "$time.jpg"

        val filepath = Environment.getExternalStorageDirectory()

        val dir = File(
            filepath.absolutePath
                    + "/StepCounter/profile/"
        )

        if (!dir.exists()) {
            dir.mkdirs()
        }
        val f = File(dir, fileName)


        if (filepath != null) {

            outputFileUri = Uri.fromFile(f)
        }
        return outputFileUri
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {

        if (resultCode == Activity.RESULT_OK) {

            var isCamera = true
            if (data != null) {
                val action = data.action
                isCamera = action != null && action == MediaStore.ACTION_IMAGE_CAPTURE
            }
            if (!isCamera) {


                picUri = getPickImageResultUri(data)
                try {
                    myBitmap = MediaStore.Images.Media.getBitmap(this.contentResolver, picUri)

                    myBitmap = getResizedBitmap(myBitmap, 500)


                    uploadImage(true)
                    // ivProfile.setImageBitmap(myBitmap)
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            } else {
                /*if (data != null) {
                    bitmap = data.extras["data"] as Bitmap
                    myBitmap = bitmap
                }*/

                val filepath = Environment.getExternalStorageDirectory()

                val f = File(
                    filepath.absolutePath
                            + "/StepCounter/profile/" + fileName
                )


                try {
                    Path = f.absolutePath
                    Npath = compressImage(Path)
                    val bm: Bitmap
                    val btmapOptions = BitmapFactory.Options()
                    bm = BitmapFactory.decodeFile(Npath, btmapOptions)
                    picUri = Uri.parse(Npath)
                    uploadImage(false)
                    //  ivProfile.setImageBitmap(bm)
                } catch (e: java.lang.Exception) {

                }


            }
        }
    }


    open fun getResizedBitmap(image: Bitmap, maxSize: Int): Bitmap {
        var width = image.width
        var height = image.height
        val bitmapRatio = width.toFloat() / height.toFloat()
        if (bitmapRatio > 0) {
            width = maxSize
            height = (width / bitmapRatio).toInt()
        } else {
            height = maxSize
            width = (height * bitmapRatio).toInt()
        }
        return Bitmap.createScaledBitmap(image, width, height, true)
    }


    /**
     * Get the URI of the selected image from [.getPickImageChooserIntent].<br></br>
     * Will return the correct URI for camera and gallery image.
     *
     * @param data the returned data of the activity result
     */
    open fun getPickImageResultUri(data: Intent?): Uri? {
        var isCamera = true
        if (data != null) {
            val action = data.action
            isCamera = action != null && action == MediaStore.ACTION_IMAGE_CAPTURE
        }
        return data!!.data
    }

    @TargetApi(Build.VERSION_CODES.M)
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            ALL_PERMISSIONS_RESULT -> {
                for (perms in permissionsToRequest) {
                    if (hasPermission(perms)) {
                    } else {
                        permissionsRejected.add(perms)
                    }
                }
                if (permissionsRejected.size > 0) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        if (shouldShowRequestPermissionRationale(permissionsRejected[0])) {
                            showMessageOKCancel("These permissions are mandatory for the application. Please allow access.",
                                DialogInterface.OnClickListener { dialog, which ->
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) { //Log.d("API123", "permisionrejected " + permissionsRejected.size());
                                        requestPermissions(
                                            permissionsRejected.toArray(
                                                arrayOfNulls(permissionsRejected.size)
                                            ), ALL_PERMISSIONS_RESULT
                                        )
                                    }
                                })
                            return
                        }
                    }
                }
            }
        }
    }


    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        // save file url in bundle as it will be null on scren orientation
// changes
        outState.putParcelable("pic_uri", picUri)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        // get the file url
        picUri = savedInstanceState.getParcelable("pic_uri")
    }

    private fun addUserChangeListener() {
        mFirebaseDatabase.child(userId).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val user: Users? =
                    dataSnapshot.getValue(
                        Users::class.java
                    )
                // Check for null
                if (user == null) {
                    Log.e("Profile Err", "User data is null!")
                    return
                }

                // Display newly updated name and email
                edtP_Name.setText(user.name.toString())
                val myFormat = "dd-MM-yyyy" // mention the format you need
                val sdf = SimpleDateFormat(myFormat, Locale.US)
                tvP_DOB.text = sdf.format(user.DOB)
                edtP_Mobile.setText(user.phone.toString())
                if (user.gender.toString() == "Male") {
                    edtSemail.setSelection(0)
                } else {
                    edtSemail.setSelection(1)
                }


                toggleButton()
                pbPro.visibility = View.GONE
            }

            override fun onCancelled(error: DatabaseError) { // Failed to read value
                Log.e("Profile Err", "Failed to read user", error.toException())
            }
        })


    }

    private fun updateUser(users: Users) { // updating the user via child nodes
        if (!TextUtils.isEmpty(users.name)) mFirebaseDatabase.child(userId).child("name").setValue(
            users.name
        )
        if (!TextUtils.isEmpty(users.phone)) mFirebaseDatabase.child(userId).child("phone").setValue(
            users.phone
        )

        if (!TextUtils.isEmpty(users.DOB.toString())) mFirebaseDatabase.child(userId).child("DOB").setValue(
            users.DOB
        )
        if (!TextUtils.isEmpty(users.gender)) mFirebaseDatabase.child(userId).child("gender").setValue(
            users.gender
        )
        if (!TextUtils.isEmpty(users.profile)) mFirebaseDatabase.child(userId).child("profile").setValue(
            users.profile
        )

    }


    private fun findUnAskedPermissions(wanted: ArrayList<String>): ArrayList<String> {
        val result = ArrayList<String>()
        for (perm in wanted) {
            if (!hasPermission(perm)) {
                result.add(perm)
            }
        }
        return result
    }

    private fun hasPermission(permission: String): Boolean {
        if (canMakeSmores()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                return checkSelfPermission(permission) === PackageManager.PERMISSION_GRANTED
            }
        }
        return true
    }

    private fun showMessageOKCancel(
        message: String,
        okListener: DialogInterface.OnClickListener
    ) {
        AlertDialog.Builder(this)
            .setMessage(message)
            .setPositiveButton("OK", okListener)
            .setNegativeButton("Cancel", null)
            .create()
            .show()


    }

    private fun canMakeSmores(): Boolean {
        return Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP_MR1
    }

    open fun compressImage(imageUri: String): String {
        val filePath = getRealPathFromURI(imageUri)
        var scaledBitmap: Bitmap? = null
        val options = BitmapFactory.Options()
        //      by setting this field as true, the actual bitmap pixels are not loaded in the memory. Just the bounds are loaded. If
//      you try the use the bitmap here, you will get null.
        options.inJustDecodeBounds = true
        var bmp = BitmapFactory.decodeFile(filePath, options)
        var actualHeight = options.outHeight
        var actualWidth = options.outWidth
        //      max Height and width values of the compressed image is taken as 816x612
        val maxHeight = 816.0f
        val maxWidth = 612.0f
        var imgRatio = actualWidth / actualHeight.toFloat()
        val maxRatio = maxWidth / maxHeight
        //      width and height values are set maintaining the aspect ratio of the image
        if (actualHeight > maxHeight || actualWidth > maxWidth) {
            if (imgRatio < maxRatio) {
                imgRatio = maxHeight / actualHeight
                actualWidth = (imgRatio * actualWidth).toInt()
                actualHeight = maxHeight.toInt()
            } else if (imgRatio > maxRatio) {
                imgRatio = maxWidth / actualWidth
                actualHeight = (imgRatio * actualHeight).toInt()
                actualWidth = maxWidth.toInt()
            } else {
                actualHeight = maxHeight.toInt()
                actualWidth = maxWidth.toInt()
            }
        }
        //      setting inSampleSize value allows to load a scaled down version of the original image
        options.inSampleSize = calculateInSampleSize(options, actualWidth, actualHeight)
        //      inJustDecodeBounds set to false to load the actual bitmap
        options.inJustDecodeBounds = false
        //      this options allow android to claim the bitmap memory if it runs low on memory
        options.inPurgeable = true
        options.inInputShareable = true
        options.inTempStorage = ByteArray(16 * 1024)
        try { //          load the bitmap from its path
            bmp = BitmapFactory.decodeFile(filePath, options)
        } catch (exception: OutOfMemoryError) {
            exception.printStackTrace()
        }
        try {
            scaledBitmap = Bitmap.createBitmap(actualWidth, actualHeight, Bitmap.Config.ARGB_8888)
        } catch (exception: OutOfMemoryError) {
            exception.printStackTrace()
        }
        val ratioX = actualWidth / options.outWidth.toFloat()
        val ratioY = actualHeight / options.outHeight.toFloat()
        val middleX = actualWidth / 2.0f
        val middleY = actualHeight / 2.0f
        val scaleMatrix = Matrix()
        scaleMatrix.setScale(ratioX, ratioY, middleX, middleY)
        assert(scaledBitmap != null)
        val canvas = Canvas(scaledBitmap)
        canvas.matrix = scaleMatrix
        canvas.drawBitmap(
            bmp,
            middleX - bmp.width / 2,
            middleY - bmp.height / 2,
            Paint(Paint.FILTER_BITMAP_FLAG)
        )
        //      check the rotation of the image and display it properly
        val exif: ExifInterface
        try {
            exif = ExifInterface(filePath)
            val orientation = exif.getAttributeInt(
                ExifInterface.TAG_ORIENTATION, 0
            )
            Log.d("EXIF", "Exif: $orientation")
            val matrix = Matrix()
            if (orientation == 6) {
                matrix.postRotate(90f)
                Log.d("EXIF", "Exif: $orientation")
            } else if (orientation == 3) {
                matrix.postRotate(180f)
                Log.d("EXIF", "Exif: $orientation")
            } else if (orientation == 8) {
                matrix.postRotate(270f)
                Log.d("EXIF", "Exif: $orientation")
            }
            scaledBitmap = Bitmap.createBitmap(
                scaledBitmap, 0, 0,
                scaledBitmap!!.width, scaledBitmap.height, matrix,
                true
            )
        } catch (e: IOException) {
            e.printStackTrace()
        }
        val out: FileOutputStream
        val filename = getFilename()
        try {
            out = FileOutputStream(filename)
            //          write the compressed bitmap at the destination specified by filename.
            scaledBitmap!!.compress(Bitmap.CompressFormat.JPEG, 80, out)
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        }
        return filename
    }


    private fun getRealPathFromURI(contentURI: String): String {
        val contentUri = Uri.parse(contentURI)
        @SuppressLint("Recycle") val cursor =
            contentResolver.query(contentUri, null, null, null, null)
        return if (cursor == null) {
            contentUri.path
        } else {
            cursor.moveToFirst()
            val index = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA)
            cursor.getString(index)
        }
    }

    open fun calculateInSampleSize(
        options: BitmapFactory.Options,
        reqWidth: Int,
        reqHeight: Int
    ): Int {
        val height = options.outHeight
        val width = options.outWidth
        var inSampleSize = 1
        if (height > reqHeight || width > reqWidth) {
            val heightRatio =
                (height.toFloat() / reqHeight.toFloat()).roundToInt()
            val widthRatio =
                (width.toFloat() / reqWidth.toFloat()).roundToInt()
            inSampleSize = if (heightRatio < widthRatio) heightRatio else widthRatio
        }
        val totalPixels = width * height.toFloat()
        val totalReqPixelsCap = reqWidth * reqHeight * 2.toFloat()
        while (totalPixels / (inSampleSize * inSampleSize) > totalReqPixelsCap) {
            inSampleSize++
        }
        return inSampleSize
    }

    open fun getFilename(): String {
        val file = File(
            Environment.getExternalStorageDirectory().path,
            "/StepCounter/profile"
        )
        if (!file.exists()) {
            file.mkdirs()
        }
        return file.absolutePath + "/" + time + ".jpg"
    }

    private fun initLoader(context: Context) {
        loader?.init(ImageLoaderConfiguration.createDefault(context))
        options = DisplayImageOptions.Builder()
            .showImageOnLoading(R.drawable.loading)
            .showImageForEmptyUri(R.drawable.com_facebook_profile_picture_blank_portrait)
            .showImageOnFail(R.drawable.com_facebook_profile_picture_blank_portrait)
            .cacheInMemory(true)
            .cacheOnDisk(true)
            .considerExifParams(true)
            .displayer(CircleBitmapDisplayer(Color.WHITE, 5F))
            .build()
    }

    open fun uploadImage(flg: Boolean) {
        if (picUri != null) { // Code for showing progressDialog while uploading
            val progressDialog = ProgressDialog(this)
            progressDialog.setTitle("Uploading...")
            progressDialog.show()


            // Defining the child of storageReference
            val ref = storageReference
                .child(
                    "images/"
                            + UUID.randomUUID().toString()
                )

            if (flg) {
                ref.putFile(picUri!!)
                    .addOnSuccessListener {
                        // Image uploaded successfully
                        // Dismiss dialog
                        progressDialog.dismiss()
                        Toast
                            .makeText(
                                this@ProfileActivity,
                                "Image Uploaded!!",
                                Toast.LENGTH_SHORT
                            )
                            .show()

                        ref.downloadUrl.addOnSuccessListener {

                            run {
                                Log.e("DownloadURL", "onSuccess: uri= $it")

                                loader!!.displayImage(
                                    it.toString(),
                                    ivProfile,
                                    options,
                                    animateFirstListener
                                )
                                val users = Users()
                                users.profile = it.toString()
                                updateUser(users)
                            }
                        }
                    }
                    .addOnFailureListener { e ->
                        // Error, Image not uploaded
                        progressDialog.dismiss()
                        Toast
                            .makeText(
                                this@ProfileActivity,
                                "Failed " + e.message,
                                Toast.LENGTH_SHORT
                            )
                            .show()
                    }
                    .addOnProgressListener {


                        run {
                            val progress: Double = (100.0
                                    * it.bytesTransferred
                                    / it.totalByteCount)
                            progressDialog.setMessage(
                                "Uploaded "
                                        + progress.toInt() + "%"
                            )


                        }
                    }
            } else {

                ref.putStream(FileInputStream(File(Npath)))
                    .addOnSuccessListener {
                        // Image uploaded successfully
                        // Dismiss dialog
                        progressDialog.dismiss()
                        Toast
                            .makeText(
                                this@ProfileActivity,
                                "Image Uploaded!!",
                                Toast.LENGTH_SHORT
                            )
                            .show()
                        ref.downloadUrl.addOnSuccessListener {

                            run {
                                Log.e("DownloadURL", "onSuccess: uri= $it")
                                loader!!.displayImage(
                                    it.toString(),
                                    ivProfile,
                                    options,
                                    animateFirstListener
                                )

                                val users = Users()
                                users.profile = it.toString()
                                updateUser(users)
                            }
                        }
                        // val downloadUrl: Uri = ref.downloadUrl.resul


                    }
                    .addOnFailureListener { e ->
                        // Error, Image not uploaded
                        progressDialog.dismiss()
                        Toast
                            .makeText(
                                this@ProfileActivity,
                                "Failed " + e.message,
                                Toast.LENGTH_SHORT
                            )
                            .show()
                    }
                    .addOnProgressListener {


                        run {
                            val progress: Double = (100.0
                                    * it.bytesTransferred
                                    / it.totalByteCount)
                            progressDialog.setMessage(
                                "Uploaded "
                                        + progress.toInt() + "%"
                            )
                        }
                    }
            }
        }
    }

    private class AnimateFirstDisplayListener :
        SimpleImageLoadingListener() {
        override fun onLoadingComplete(
            imageUri: String,
            view: View,
            loadedImage: Bitmap
        ) {
            if (loadedImage != null) {
                val imageView = view as ImageView
                val firstDisplay =
                    !displayedImages.contains(imageUri)
                if (firstDisplay) {
                    FadeInBitmapDisplayer.animate(imageView, 500)
                    displayedImages.add(imageUri)
                }
            }
        }

        companion object {
            val displayedImages =
                Collections.synchronizedList(LinkedList<String>())
        }
    }

}