package com.example.gdrive

import android.Manifest
import android.accounts.Account
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.util.Log
import android.widget.Button
import android.widget.Toast
import android.app.Activity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.auth.api.identity.BeginSignInRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.auth.api.identity.SignInClient
import com.google.android.gms.common.api.ApiException
import com.google.api.client.extensions.android.http.AndroidHttp
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import java.util.*


class OneTap : Activity() {

    private val REQ_ONE_TAP = 12345
    private val TAG ="..OneTap"
    private var oneTapClient: SignInClient? = null
    private var mDriveServiceHelper: DriveServiceHelper? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_onetap)
        oneTapClient = Identity.getSignInClient(this)
        signIn()
        findViewById<Button>(R.id.sync).setOnClickListener { takePermissions()}
    }

    private fun signIn() {
        val signInRequest = createSignInRequest(onlyAuthorizedAccounts = true)

        oneTapClient
            ?.beginSignIn(signInRequest)
            ?.addOnSuccessListener(this) { result ->
                try {
                    startIntentSenderForResult(
                        result.pendingIntent.intentSender, REQ_ONE_TAP,
                        null, 0, 0, 0, null
                    )
                } catch (e: IntentSender.SendIntentException) {
                    Log.e(TAG, "Couldn't start One Tap UI: ${e.localizedMessage}")
                }
            }
            ?.addOnFailureListener(this) { e ->
                // No saved credentials found. Launch the One Tap sign-up flow
                signUp()
            }
    }

    private fun signUp() {
        val signUpRequest = createSignInRequest(onlyAuthorizedAccounts = false)

        oneTapClient
            ?.beginSignIn(signUpRequest)
            ?.addOnSuccessListener(this) { result ->
                try {
                    startIntentSenderForResult(
                        result.pendingIntent.intentSender, REQ_ONE_TAP,
                        null, 0, 0, 0, null
                    )
                } catch (e: IntentSender.SendIntentException) {
                    Log.e(TAG, "Couldn't start One Tap UI: ${e.localizedMessage}")
                }
            }
            ?.addOnFailureListener(this) { e ->
                Log.d(TAG, e.localizedMessage)
                // No saved credentials found. Show error
                Toast.makeText(this,e.localizedMessage,Toast.LENGTH_LONG).show()
            }
    }

    private fun createSignInRequest(onlyAuthorizedAccounts: Boolean): BeginSignInRequest =
        BeginSignInRequest.builder()
            .setPasswordRequestOptions(
                BeginSignInRequest.PasswordRequestOptions.builder()
                    .setSupported(true)
                    .build()
            )
            .setGoogleIdTokenRequestOptions(
                BeginSignInRequest.GoogleIdTokenRequestOptions.builder()
                    .setSupported(true)
                    // Your server's client ID, not your Android client ID.
                    .setServerClientId(getString(R.string.default_web_client_id))
                    // Only show accounts previously used to sign in.
                    .setFilterByAuthorizedAccounts(onlyAuthorizedAccounts)
                    .build()
            )
            // Automatically sign in when exactly one credential is retrieved.
            .setAutoSelectEnabled(true)
            .build();

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {

            REQ_ONE_TAP -> {

                try {
                    val credential = oneTapClient?.getSignInCredentialFromIntent(data)
                    val idToken = credential?.googleIdToken
                    val username = credential?.id
                    val password = credential?.password

                    val accountCredential = GoogleAccountCredential.usingOAuth2(this, Collections.singleton(DriveScopes.DRIVE_FILE))
                    accountCredential.selectedAccount = Account(username,"com.example.onetapsignin")

                    val googleDriveService = Drive.Builder(
                        AndroidHttp.newCompatibleTransport(),
                        GsonFactory(),
                        accountCredential
                    )
                        .setApplicationName("Drive API Migration")
                        .build()

                    mDriveServiceHelper = DriveServiceHelper(googleDriveService)

                } catch (e: ApiException) { }
            }
            101 -> if(Build.VERSION.SDK_INT==Build.VERSION_CODES.R){
                list()
            }else{
                takePermission()
            }
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
        val read = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
        val write = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)

        return if(Build.VERSION.SDK_INT== Build.VERSION_CODES.R){
            Environment.isExternalStorageManager()
        } else{
            read== PackageManager.PERMISSION_GRANTED && write== PackageManager.PERMISSION_GRANTED
        }
    }

    private fun takePermission(){
        if(Build.VERSION.SDK_INT== Build.VERSION_CODES.R){
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
            ActivityCompat.requestPermissions(this, arrayOf(
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ),102)
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
                val writeExternalStorage = grantResults[0]== PackageManager.PERMISSION_GRANTED
                val readExternalStorage = grantResults[1]== PackageManager.PERMISSION_GRANTED
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

