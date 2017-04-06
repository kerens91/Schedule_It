package com.example.keren.schedule_test;




import android.app.ProgressDialog;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.text.InputType;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.example.keren.schedule_test.FireBase.FireBaseOperations;
import com.example.keren.schedule_test.SQLDataBase.DBHandler;
import com.example.keren.schedule_test.SQLDataBase.TableData;
import com.example.keren.schedule_test.UsersInformation.UserInformation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;

public class MainActivity extends AppCompatActivity {

    // tool members
    private Button logIn_buttn;
    private Button register_buttn;
    private EditText editTxtEmail;
    private EditText editTxtPass;
    private ProgressDialog progressdialog;
    private UserInformation user;

    // firebase members
    private FireBaseOperations fireBase;
    private String userEmail;
    private String userPass;
    private boolean hasDataOnDevice;


    // database
    private DBHandler dataBase;

    // key for game activity Bundle
    final static String EMAIL_KEY = "emailKey";
    final static String NAME_KEY = "nameKey";
    final static String STATUS_KEY = "statusKey";
    final static String URI_KEY = "uriKey";
    final static String NEW_USER_KEY = "newUserKey";
    final static String BUNDLE_KEY = "userBundle";




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        // init boolean indicates if the user is new on this device
        hasDataOnDevice = true;

        // set activity window
        setWindowProperties();

        // init dataBase
        dataBase = new DBHandler(MainActivity.this);

        // get access to fireBase
        initFireBase();

        // check if user is already connected - do auto login
        checkAutoLogin();

        // init fields
        iniFields();

        // init buttons
        initButtons();


    }


    public void checkAutoLogin()
    {
        if (fireBase.isUserConnected()) {
            Log.d("user connected", "trying to login");

            try {
                ifUserExistInDBGetTheUser(fireBase.getUserEmail());

                startNextActivity();
                finish();
            } catch (Exception e) {
                e.printStackTrace();
            }



        }
    }




    public void setWindowProperties()
    {
        // set window to black
        Window window = MainActivity.this.getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.setStatusBarColor(ContextCompat.getColor(MainActivity.this, R.color.common_google_signin_btn_text_dark_focused));

        // hide action bar
        ActionBar actionBar = getSupportActionBar();
        actionBar.hide();
    }




    public void initFireBase()
    {
        // gives us permission to register users to firebase
        fireBase = FireBaseOperations.getInstance();
        fireBase.getData();


    }




    public void iniFields()
    {
        // init progress dialog
        progressdialog = new ProgressDialog(this);

        // init text fields
        editTxtEmail = (EditText)findViewById(R.id.email_editTxt);
        editTxtPass = (EditText)findViewById(R.id.pass_editTxt);

        // set keyboard type
        editTxtEmail.setInputType(InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        editTxtEmail.setImeOptions(EditorInfo.IME_ACTION_DONE);
        editTxtPass.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        editTxtPass.setImeOptions(EditorInfo.IME_ACTION_DONE);

        // set editor listeners
        editTxtEmail.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
                boolean handled = false;

                // if 'Done' pressed
                if (actionId == EditorInfo.IME_ACTION_DONE){

                    // get user input
                    userEmail = textView.getText().toString();

                    // hide keyBoard
                    InputMethodManager imm = (InputMethodManager)getSystemService(INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(textView.getWindowToken(), 0);

                    handled = true;
                }
                return handled;
            }
        });


        editTxtPass.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
                boolean handled = false;

                // if 'Done' pressed
                if (actionId == EditorInfo.IME_ACTION_DONE){

                    // get user input
                    userPass = textView.getText().toString();

                    // hide keyBoard
                    InputMethodManager imm = (InputMethodManager)getSystemService(INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(textView.getWindowToken(), 0);

                    handled = true;
                }
                return handled;
            }
        });

    }







    public void initButtons() {

        // init log-in button
        logIn_buttn = (Button) findViewById(R.id.login_btn);

        logIn_buttn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                logInUser();
            }
        });


        // init register button
        register_buttn = (Button)findViewById(R.id.register_btn);

        register_buttn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                registerUser();
            }
        });
    }








    public void registerUser()
    {

        // get data from user
        if (!checkUserDataValidation())
            return;

        // if both string (email & password) are valid - start progress dialog
        progressdialog.setMessage(getString(R.string.Registering));
        progressdialog.show();

        FirebaseAuth firebaseAuth = fireBase.getFirebaseAuth();

        // register the user with the given details
        firebaseAuth.createUserWithEmailAndPassword(userEmail, userPass)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {

                        // stop the progress dialog
                        progressdialog.dismiss();

                        // inform the user if registration succeeded
                        if (task.isSuccessful()) {
                            createUser(userEmail);
                            fireBase.saveInformation(user);
                            insertNewUserToDataBase();
                            Toast.makeText(MainActivity.this, R.string.registerSucceed, Toast.LENGTH_SHORT).show();
                        }
                        else
                        {
                            Toast.makeText(MainActivity.this, R.string.registerNotSucceed, Toast.LENGTH_SHORT).show();
                        }
                    }
                });

    }







    public boolean checkUserDataValidation()
    {
        userEmail = editTxtEmail.getText().toString();
        userPass = editTxtPass.getText().toString();

        // check if email string is valid
        if (TextUtils.isEmpty(userEmail))
        {
            Toast.makeText(this, R.string.enterEmail, Toast.LENGTH_SHORT).show();
            return false;
        }


        // check if password string is valid
        if (TextUtils.isEmpty(userPass)) {
            Toast.makeText(this, R.string.enterPass, Toast.LENGTH_SHORT).show();
            return false;
        }


        return true;
    }





    public void logInUser()
    {


        try {
            // get data from user
            if (!checkUserDataValidation())
                return;

            // if both string (email & password) are valid - start progress dialog
            progressdialog.setMessage(getString(R.string.Searching) );
            progressdialog.show();

            Log.d("in login", "mail= " + userEmail + ", pass= " + userPass);

            final FirebaseAuth firebaseAuth = fireBase.getFirebaseAuth();

            firebaseAuth.signInWithEmailAndPassword(userEmail, userPass)
                    .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {

                            // stop the progress dialog
                            progressdialog.dismiss();

                            // start next activity with user's details
                            if (task.isSuccessful()) {

                                //Toast.makeText(MainActivity.this, "succeeded", Toast.LENGTH_SHORT).show();

                                // get the user from fireBase
                                createExistingUser(userEmail);

                                // starts the new activity and sends user information
                                startNextActivity();

                                // finish this activity and start next one
                                finish();


                            }

                            // inform the user if failed to log in
                            else {
                                Toast.makeText(MainActivity.this, R.string.NoUser, Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
        }
        catch (Exception ex)
        {
            Log.d("in login", "ex=" + ex.getMessage());
        }



    }






    // create new user if not exist in data base on this device
    public void createUser(String email)
    {
        Log.d("createUser", "creating...");

        String name = ""+R.string.UserName;
        String status = ""+R.string.UserStatus;
        String uri = ""+R.string.UserUri;

        user = new UserInformation(email, name, status, uri);
    }






    // get the user from data base if exist on this device
    public void ifUserExistInDBGetTheUser(String email) throws Exception
    {
        Log.d("ifUserExistInDBGet", "checking...");

        try {
            Cursor c = dataBase.getUserByEmail(email);

            if (c.getColumnCount() > 0) {
                //Toast.makeText(this, "cursor more than one", Toast.LENGTH_SHORT).show();
                Log.d("cursor", "more than 1");

                c.moveToFirst();
                Log.d("cursor", "moved to first");


                int id = c.getInt(TableData.TableInfo.COL_ID);
                Log.d("cursor", "" + id);

                String eemail = c.getString(TableData.TableInfo.COL_EMAIL);
                Log.d("cursor", eemail);

                String name = c.getString(TableData.TableInfo.COL_NAME);
                Log.d("cursor", name);

                String status = c.getString(TableData.TableInfo.COL_STATUS);
                Log.d("cursor", status);

                String uri = c.getString(TableData.TableInfo.COL_PHOTO);
                Log.d("cursor", uri);



                user = new UserInformation(email, name, status, uri);
                user.setId(id);

            }
        }
        catch (Exception ex) {
            Log.d("cursor", "is null" + ex.getMessage());

            // create new user
            createUser(email);
            insertNewUserToDataBase();
            fireBase.saveInformation(user);

            Log.d("cursor", "first part" + ex.getMessage());

            // set boolean to existing new user
            this.hasDataOnDevice = false;

            //Toast.makeText(this, "has data set to false", Toast.LENGTH_SHORT).show();
        }

    }







    // save new registered user to data base on this device
    public void insertNewUserToDataBase()
    {

        Log.d("insertNewUserToDataBase", "user in database");
        dataBase.addUser(user);
    }







    // user exists in fire base
    public void createExistingUser(String email)
    {
        Log.d("createExistingUser", "insert to dataBase");


        // check if also exist on data base
        try {
            //Toast.makeText(this, "check user in data base", Toast.LENGTH_SHORT).show();

            // if exist - get the user
            ifUserExistInDBGetTheUser(email);

        }


        // if not exist in data base on this device
        catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, R.string.UserNotInDB, Toast.LENGTH_SHORT).show();


        }


    }




    public void startNextActivity() {

        // create new intent
        Intent ProfileIntent = new Intent(MainActivity.this, ProfileActivity.class);

        // add args to bundle
        Bundle bundle = new Bundle();

        // email
        bundle.putString(EMAIL_KEY, user.getEmail());

        // name
        bundle.putString(NAME_KEY, user.getName());

        // status
        bundle.putString(STATUS_KEY, user.getStatus());

        // uri
        bundle.putString(URI_KEY, user.getUri());

        // boolean indicates if the user is new on this device
        bundle.putBoolean(NEW_USER_KEY, this.hasDataOnDevice);

        // add bundle to intent
        ProfileIntent.putExtra(BUNDLE_KEY,bundle);

        // start profile activity
        MainActivity.this.startActivity(ProfileIntent);

    }







}
