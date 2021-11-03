package com.example.gdrive

import android.Manifest
import android.Manifest.permission.READ_EXTERNAL_STORAGE
import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.Toast
import android.app.Activity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.api.client.extensions.android.http.AndroidHttp
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import java.util.*


class GoogleSignIn : Activity(){
    private val TAG = "..Google"

    private val REQUEST_CODE_SIGN_IN = 1
    private var mDriveServiceHelper: DriveServiceHelper? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_google_sign_in)
        findViewById<Button>(R.id.sync).setOnClickListener { takePermissions() }
        requestSignIn()

    }


    private fun requestSignIn(){
        Log.d(TAG, "Requesting sign-in")
        val signInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestScopes(Scope(DriveScopes.DRIVE_METADATA))
                .build()
        val client = GoogleSignIn.getClient(this, signInOptions)
        startActivityForResult(client.signInIntent, REQUEST_CODE_SIGN_IN)
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, resultData: Intent?) {
        when (requestCode) {
            REQUEST_CODE_SIGN_IN -> if (resultCode == RESULT_OK && resultData != null) {
                handleSignInResult(resultData)
            }
            101 -> if(Build.VERSION.SDK_INT==Build.VERSION_CODES.R){
                list()
            }else{
                takePermission()
            }
        }
        super.onActivityResult(requestCode, resultCode, resultData)
    }


    private fun handleSignInResult(result: Intent) {
        GoogleSignIn.getSignedInAccountFromIntent(result)
                .addOnSuccessListener { googleAccount: GoogleSignInAccount ->
                Log.d(TAG, "Signed in as " + googleAccount.email)

            val credential = GoogleAccountCredential.usingOAuth2(
                    this, Collections.singleton(DriveScopes.DRIVE_FILE)
            )
            credential.selectedAccount = googleAccount.account
            val googleDriveService = Drive.Builder(
                    AndroidHttp.newCompatibleTransport(),
                   GsonFactory(),
                    credential
            )
                    .setApplicationName("Drive API Migration")
                    .build()

            mDriveServiceHelper = DriveServiceHelper(googleDriveService)
        }
            .addOnFailureListener { exception: Exception? ->
            Log.e(
                    TAG,
                    "Unable to sign in.",
                    exception
            )
        }
    }


    private fun takePermissions(){

        if (isPermissionGranted()){
            list()
        }
        else{
            takePermission()
        }

    }

    private fun isPermissionGranted():Boolean{
        val read =ContextCompat.checkSelfPermission(this,Manifest.permission.READ_EXTERNAL_STORAGE)
        val write =ContextCompat.checkSelfPermission(this,Manifest.permission.WRITE_EXTERNAL_STORAGE)

        return if(Build.VERSION.SDK_INT==Build.VERSION_CODES.R){
            Environment.isExternalStorageManager()
        } else{
            read==PackageManager.PERMISSION_GRANTED && write== PackageManager.PERMISSION_GRANTED
        }
    }

    private fun takePermission(){
        if(Build.VERSION.SDK_INT==Build.VERSION_CODES.R){
            try{
                val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                intent.addCategory("android.intent.category.DEFAULT")
                intent.data = Uri.parse(String.format("package:%s",applicationContext.packageName))
                startActivityForResult(intent,101)
            }
            catch (e:Exception){
                val intent = Intent()
                intent.action = Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION
                startActivityForResult(intent,101)
            }
        }
        else{
            ActivityCompat.requestPermissions(this, arrayOf(WRITE_EXTERNAL_STORAGE,READ_EXTERNAL_STORAGE),102)
        }
    }

    override fun onRequestPermissionsResult(
            requestCode: Int,
            permissions: Array<out String>,
            grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if(requestCode == 102){
            if (grantResults.isNotEmpty()){
                val writeExternalStorage = grantResults[0]==PackageManager.PERMISSION_GRANTED
                val readExternalStorage = grantResults[1]==PackageManager.PERMISSION_GRANTED
                if (readExternalStorage && writeExternalStorage){
                    list()
                }
                else{
                    takePermission()
                }
            }
        }
    }
    private fun list(){
        mDriveServiceHelper?.folder(this)
                ?.addOnSuccessListener {
            Toast.makeText(applicationContext,"Sync started",Toast.LENGTH_LONG).show()}
            ?.addOnFailureListener { exception ->
                Toast.makeText(applicationContext, "Failed to fetch : $exception",Toast.LENGTH_LONG).show()
            Log.i(TAG, "Failed to Download : $exception")
        }
    }


}



