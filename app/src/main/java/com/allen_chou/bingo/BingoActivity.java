package com.allen_chou.bingo;

import android.content.DialogInterface;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.firebase.ui.common.ChangeEventType;
import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BingoActivity extends AppCompatActivity {

    public static final int NUMBER_COUNT = 25;
    private static final String TAG = BingoActivity.class.getSimpleName();
    private String roomId;
    private boolean creator;
    private TextView infoText;
    private RecyclerView recyclerView;
    private List<NumberButton> numberButtons;
    private FirebaseRecyclerAdapter<Boolean, NumberButtonsHolder> adapter;
    private List<Integer> randomNumbers;
    private Map<Integer, NumberButton> numberButtonMap = new HashMap<>();
    private boolean myTurn;

    ValueEventListener statusListener = new ValueEventListener() {
        @Override
        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
            long status = (long) dataSnapshot.getValue();
            switch ((int) status) {
                case Room.STATUS_CREATED:
                    infoText.setText("等待對手加入");
                    break;
                case Room.STATUS_JOINED:
                    setMyTurn(isCreator());
                    FirebaseDatabase.getInstance().getReference("rooms")
                            .child(roomId)
                            .child("status")
                            .setValue(Room.STATUS_CREATOR_TRUN);
                    break;
                case Room.STATUS_CREATOR_TRUN:
                    setMyTurn(isCreator());
                    break;
                case Room.STATUS_JOINNER_TRUN:
                    setMyTurn(!isCreator());
                    break;
                case Room.STATUS_CREATOR_BINGO:
                    if (!isCreator()) {
                        new AlertDialog.Builder(BingoActivity.this)
                                .setTitle("結果")
                                .setMessage("oh! 對方賓果了!")
                                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
                                        endGame();
                                    }
                                }).show();
                    }
                    break;
                case Room.STATUS_JOINNER_BINGO:
                    if (isCreator()) {
                        new AlertDialog.Builder(BingoActivity.this)
                                .setTitle("結果")
                                .setMessage("oh! 對方賓果了!")
                                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
                                        endGame();
                                    }
                                }).show();
                    }
                    break;
            }
        }

        @Override
        public void onCancelled(@NonNull DatabaseError databaseError) {

        }
    };

    private void endGame() {
        //先移除listener
        FirebaseDatabase.getInstance().getReference("rooms").child(roomId).child("status").removeEventListener(statusListener);

        if (isCreator()) {
            FirebaseDatabase.getInstance().getReference("rooms")
                    .child(roomId)
                    .removeValue();

        }
        finish();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bingo);
        findViews();
        roomId = getIntent().getStringExtra("ROOM_ID");
        creator = getIntent().getBooleanExtra("ROOM_CREATOR", false);
        generateRandomNumbers();


        if (isCreator()) {
            //將25個numbers key 放入boolean值 寫入db
            for (int i = 0; i < NUMBER_COUNT; i++) {
                FirebaseDatabase.getInstance().getReference("rooms")
                        .child(roomId)
                        .child("numbers")
                        .child((i + 1) + "")
                        .setValue(false);
            }
            FirebaseDatabase.getInstance().getReference("rooms").child(roomId).child("status")
                    .setValue(Room.STATUS_CREATED);

        } else {
            FirebaseDatabase.getInstance().getReference("rooms").child(roomId)
                    .child("status").setValue(Room.STATUS_JOINED);
        }

        //recycler
        Query query = FirebaseDatabase.getInstance().getReference("rooms")
                .child(roomId)
                .child("numbers").orderByKey();
        FirebaseRecyclerOptions<Boolean> options = new FirebaseRecyclerOptions.Builder<Boolean>()
                .setQuery(query, Boolean.class).build();
        adapter = new FirebaseRecyclerAdapter<Boolean, NumberButtonsHolder>(options) {
            @Override
            protected void onBindViewHolder(@NonNull NumberButtonsHolder holder, final int position, @NonNull Boolean model) {
                holder.numberButton.setText(numberButtons.get(position).getNumber() + "");
                holder.numberButton.setEnabled(!numberButtons.get(position).isPicked());
                holder.numberButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if (myTurn) {
                            int number = numberButtons.get(position).getNumber();
                            FirebaseDatabase.getInstance().getReference("rooms")
                                    .child(roomId)
                                    .child("numbers")
                                    .child(number + "").setValue(true);
                        }
                    }
                });
            }

            @Override
            public void onChildChanged(@NonNull ChangeEventType type, @NonNull DataSnapshot snapshot, int newIndex, int oldIndex) {
                super.onChildChanged(type, snapshot, newIndex, oldIndex);
                Log.d(TAG, "onChildChanged: " + type.name() + "/" + snapshot.getKey());
                if (type == ChangeEventType.CHANGED) {
                    NumberButton numberButton = numberButtonMap.get(Integer.parseInt(snapshot.getKey()));
                    int pos = numberButton.getPosition();
                    NumberButtonsHolder holder = (NumberButtonsHolder) recyclerView.findViewHolderForAdapterPosition(pos);
                    holder.numberButton.setEnabled(false);
                    numberButton.setPicked(true);
                    if (myTurn) {
                        FirebaseDatabase.getInstance().getReference("rooms")
                                .child(roomId)
                                .child("status")
                                .setValue(isCreator() ? Room.STATUS_JOINNER_TRUN : Room.STATUS_CREATOR_TRUN);

                        //check 賓果
                        int bingo = checkBingo();
                        if (bingo > 0) {
                            FirebaseDatabase.getInstance().getReference("rooms")
                                    .child(roomId)
                                    .child("status")
                                    .setValue(isCreator() ? Room.STATUS_CREATOR_BINGO : Room.STATUS_JOINNER_BINGO);
                            new AlertDialog.Builder(BingoActivity.this)
                                    .setTitle("結果")
                                    .setMessage("恭喜，你賓果了")
                                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialogInterface, int i) {
                                            endGame();
                                        }
                                    }).show();
                        }
                    }
                }
            }

            @NonNull
            @Override
            public NumberButtonsHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                return new NumberButtonsHolder(LayoutInflater.from(BingoActivity.this).inflate(R.layout.single_number, parent, false));
            }
        };
        recyclerView.setAdapter(adapter);
    }

    private int checkBingo() {
        int nums[] = new int[NUMBER_COUNT];
        for (int i = 0; i < NUMBER_COUNT; i++) {
            nums[i] = numberButtons.get(i).isPicked() ? 1 : 0;
        }
        int bingo = 0;
        for (int i = 0; i < 5; i++) {
            int sum = 0;
            for (int j = 0; j < 5; j++) {
                sum += nums[i * 5 + j];
            }
            bingo += (sum == 5) ? 1 : 0;
            sum = 0;
            for (int j = 0; j < 5; j++) {
                sum += nums[j * 5 + i];
            }
            bingo += (sum == 5) ? 1 : 0;
        }
        return bingo;
    }

    @Override
    protected void onStart() {
        super.onStart();
        adapter.startListening();
        FirebaseDatabase.getInstance().getReference("rooms").child(roomId).child("status").addValueEventListener(statusListener);
    }

    @Override
    protected void onStop() {
        super.onStop();
        adapter.stopListening();
    }

    public class NumberButtonsHolder extends RecyclerView.ViewHolder {
        NumberButton numberButton;

        public NumberButtonsHolder(View itemView) {
            super(itemView);
            numberButton = itemView.findViewById(R.id.number);
        }
    }

    private void generateRandomNumbers() {
        randomNumbers = new ArrayList<>();
        for (int i = 0; i < NUMBER_COUNT; i++) {
            randomNumbers.add(i + 1);
        }
        Collections.shuffle(randomNumbers);
        numberButtons = new ArrayList<>();
        for (int i = 0; i < NUMBER_COUNT; i++) {
            NumberButton numberButton = new NumberButton(this);
            numberButton.setText(randomNumbers.get(i) + "");
            numberButton.setNumber(randomNumbers.get(i));
            numberButton.setPosition(i);
            numberButtons.add(numberButton);
            //Map:使亂數號碼對應一個Button
            numberButtonMap.put(numberButton.getNumber(), numberButton);
        }
    }

    private void findViews() {
        infoText = findViewById(R.id.info_text);
        recyclerView = findViewById(R.id.recycler);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new GridLayoutManager(this, 5));
    }


    public boolean isCreator() {
        return creator;
    }

    public void setCreator(boolean creator) {
        this.creator = creator;
    }

    public boolean isMyTurn() {
        return myTurn;
    }

    public void setMyTurn(boolean myTurn) {
        this.myTurn = myTurn;
        infoText.setText(myTurn ? "請選號" : "等待對手選號");
    }
}
