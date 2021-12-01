package com.android.coffee2go.view.activities;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityOptionsCompat;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.android.coffee2go.R;
import com.android.coffee2go.helper.AccountFirebase;
import com.android.coffee2go.helper.ConfigFirebase;
import com.android.coffee2go.models.Account;
import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

public class EditProfileActivity extends AppCompatActivity {

    private CircleImageView imageEditProfile;
    private TextView textChangePicture;
    private EditText editUsername;
    private TextView editEmail;
    private Button buttonSaveChanges;
    private Account loggedAccount;
    private StorageReference storageRef;
    private String idAccount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        // initial config
        loggedAccount = AccountFirebase.getDataLoggedAccount();
        storageRef = ConfigFirebase.getFirebaseStorage().getReference();
        idAccount = AccountFirebase.getAccountUid();

        // config toolbar
        Toolbar toolbar = findViewById(R.id.toolbarMain);
        toolbar.setTitle("Edit Profile");
        setSupportActionBar(toolbar);

        assert getSupportActionBar() != null;
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_baseline_close_24);


        // init components
        initComponents();

        // get account data from loggedAccount
        FirebaseUser accountProfile = AccountFirebase.getCurrentAccount();
        editUsername.setText(accountProfile.getDisplayName());
        editEmail.setText(accountProfile.getEmail());

        Uri url = accountProfile.getPhotoUrl();
        if ( url != null ) {
            Glide.with(EditProfileActivity.this)
                    .load( url )
                    .into( imageEditProfile );
        } else
            imageEditProfile.setImageResource(R.drawable.avatar);

        // save changes on username
        buttonSaveChanges.setOnClickListener(view -> {
            String updatedUsername = editUsername.getText().toString();

            // update username on profile
            AccountFirebase.updateUsername(updatedUsername);

            // update username on realtime-database
            loggedAccount.setUsername(updatedUsername);
            loggedAccount.update();

            Toast.makeText(EditProfileActivity.this,
                    "Account updated successfully!",
                    Toast.LENGTH_SHORT).show();
        });

        // gallery activity
        ActivityResultLauncher<Intent> launchGalleryActivity = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(), result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {

                        Bitmap image;

                        Intent data = result.getData();
                        try {
                            // Select only from gallery
                            assert data != null;
                            Uri imageSelectedLocation = data.getData();
                            image = MediaStore.Images.Media.getBitmap(getContentResolver(), imageSelectedLocation);


                            // if user has choosen the image, load the image
                            if (image != null) {
                                // config image in the screen
                                imageEditProfile.setImageBitmap(image);

                                // recovery image data to firebase
                                ByteArrayOutputStream boas = new ByteArrayOutputStream();
                                image.compress(Bitmap.CompressFormat.JPEG, 70, boas);
                                byte[] imageDataInBytes = boas.toByteArray();

                                // save image in Storage
                                StorageReference imageRef = storageRef
                                        .child("images")
                                        .child("profile")
                                        .child(idAccount + ".jpeg");

                                // persist an array of bytes of the image  into the Storage using putBytes
                                UploadTask uploadTask = imageRef.putBytes(imageDataInBytes);
                                uploadTask
                                        .addOnFailureListener(e -> Toast.makeText(EditProfileActivity.this,
                                                "Error when trying to upload image!",
                                                Toast.LENGTH_SHORT).show())
                                        .addOnSuccessListener(taskSnapshot -> {
                                            // retrieve image uri
                                            Task<Uri> taskUrl = taskSnapshot.getMetadata().getReference().getDownloadUrl();
                                            taskUrl.addOnSuccessListener(this::updatePictureAccount);
                                });
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });


        // Change picture
        textChangePicture.setOnClickListener(view -> {
            Intent galleryIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                launchGalleryActivity.launch(galleryIntent);
        });
    }


    private void updatePictureAccount(Uri url) {
        // update picture in profile
        AccountFirebase.updatePictureAccount( url );

        // update picture in firebase
        loggedAccount.setUrlPicture( url.toString() );
        loggedAccount.update();

        Toast.makeText(EditProfileActivity.this,
                "Your profile picture has been updated!",
                Toast.LENGTH_SHORT).show();
    }

    private void initComponents() {
        imageEditProfile = findViewById(R.id.editProfile_imageEditProfile);
        textChangePicture = findViewById(R.id.editProfile_textChangePicture);
        editUsername = findViewById(R.id.editProfile_editUsername);
        editEmail = findViewById(R.id.editProfile_TextEmail);
        buttonSaveChanges = findViewById(R.id.editProfile_buttonSaveChanges);

        editEmail.setFocusable(false);
    }

    // when pressing back button, finish() this activity (and returns to previous one)
    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return false;
    }
}