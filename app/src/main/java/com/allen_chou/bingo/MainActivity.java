package com.allen_chou.bingo;

import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.constraint.Group;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.firebase.ui.auth.AuthUI;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.Arrays;

public class MainActivity extends AppCompatActivity implements FirebaseAuth.AuthStateListener, View.OnClickListener {

    public static final int REQUEST_CODE_SIGN_IN = 0;
    private FirebaseAuth auth;
    private TextView nickNameText;
    private ImageView avatar;
    private Group groupAvatar;
    int[] avatarsId = {R.drawable.avatar_0, R.drawable.avatar_1, R.drawable.avatar_2, R.drawable.avatar_3, R.drawable.avatar_4};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });
        findViews();
        auth = FirebaseAuth.getInstance();
    }

    private void findViews() {
        nickNameText = findViewById(R.id.nickNameText);
        avatar = findViewById(R.id.avatar);
        groupAvatar = findViewById(R.id.group_avatar);
        groupAvatar.setVisibility(View.GONE);

        nickNameText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showNickNameDialog(nickNameText.getText().toString());
            }
        });
        avatar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                boolean visible = groupAvatar.getVisibility() != View.GONE;
                groupAvatar.setVisibility(visible ? View.GONE : View.VISIBLE);
            }
        });
        findViewById(R.id.avatar_0).setOnClickListener(this);
        findViewById(R.id.avatar_1).setOnClickListener(this);
        findViewById(R.id.avatar_2).setOnClickListener(this);
        findViewById(R.id.avatar_3).setOnClickListener(this);
        findViewById(R.id.avatar_4).setOnClickListener(this);
    }

    @Override
    protected void onStart() {
        super.onStart();
        auth.addAuthStateListener(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        auth.removeAuthStateListener(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        switch (item.getItemId()) {
            case R.id.action_settings:
                return true;
            case R.id.sign_out:
                auth.signOut();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
        FirebaseUser user = firebaseAuth.getCurrentUser();
        if (user == null) {
            startActivityForResult(AuthUI.getInstance().createSignInIntentBuilder()
                    .setAvailableProviders(Arrays.asList(
                            new AuthUI.IdpConfig.GoogleBuilder().build(),
                            new AuthUI.IdpConfig.EmailBuilder().build()
                    ))
                    .setIsSmartLockEnabled(false)
                    .build(), REQUEST_CODE_SIGN_IN);
        } else {
            String uid = user.getUid();
            final String displayName = user.getDisplayName();
            FirebaseDatabase.getInstance().getReference("users")
                    .child(uid)
                    .child("displayName")
                    .setValue(displayName);

            FirebaseDatabase.getInstance().getReference("users")
                    .child(uid)
                    .addValueEventListener(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            Member member = dataSnapshot.getValue(Member.class);
                            if (member.getNickName() == null) {
                                showNickNameDialog(displayName);
                            } else {
                                nickNameText.setText(member.getNickName());
                            }
                            avatar.setImageResource(avatarsId[member.getAvatarId()]);
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {

                        }
                    });
        }
    }

    private void showNickNameDialog(String displayName) {
        final EditText nickNameEdit = new EditText(this);
        nickNameEdit.setText(displayName);
        new AlertDialog.Builder(this)
                .setTitle("Nickname")
                .setMessage("please input your nickName")
                .setView(nickNameEdit)
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {

                    }
                })
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        FirebaseDatabase.getInstance().getReference("users")
                                .child(auth.getUid())
                                .child("nickName")
                                .setValue(nickNameEdit.getText().toString());
                    }
                }).show();


    }

    @Override
    public void onClick(View view) {
        if (view instanceof ImageView) {
            int selectedId = 0;
            switch (view.getId()) {
                case R.id.avatar_1:
                    selectedId = 1;
                    break;
                case R.id.avatar_2:
                    selectedId = 2;
                    break;
                case R.id.avatar_3:
                    selectedId = 3;
                    break;
                case R.id.avatar_4:
                    selectedId = 4;
                    break;
            }
            FirebaseDatabase.getInstance().getReference("users")
                    .child(auth.getUid())
                    .child("avatarId")
                    .setValue(selectedId);
            groupAvatar.setVisibility(View.GONE);
        }
    }
}
