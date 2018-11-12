package com.allen_chou.bingo;

import android.support.annotation.NonNull;
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
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bingo);
        findViews();
        roomId = getIntent().getStringExtra("ROOM_ID");
        creator = getIntent().getBooleanExtra("ROOM_CREATOR", false);
        generateRandomNumbers();


        if (creator) {
            //寫入db
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

        }

        //recycler
        Query query = FirebaseDatabase.getInstance().getReference("rooms")
                .child(roomId)
                .child("numbers").orderByKey();
        FirebaseRecyclerOptions<Boolean> options = new FirebaseRecyclerOptions.Builder<Boolean>()
                .setQuery(query, Boolean.class).build();
        adapter = new FirebaseRecyclerAdapter<Boolean, NumberButtonsHolder>(options) {
            @Override
            protected void onBindViewHolder(@NonNull NumberButtonsHolder holder, int position, @NonNull Boolean model) {
                holder.numberButton.setText(numberButtons.get(position).getNumber() + "");
                numberButtons.get(position).setSelected(model);
            }

            @Override
            public void onChildChanged(@NonNull ChangeEventType type, @NonNull DataSnapshot snapshot, int newIndex, int oldIndex) {
                super.onChildChanged(type, snapshot, newIndex, oldIndex);
                Log.d(TAG, "onChildChanged: " + type.name() + "/" + snapshot.getKey());
                if (type == ChangeEventType.CHANGED) {
                    int pos = numberButtonMap.get(Integer.parseInt(snapshot.getKey())).getPosition();
                    NumberButtonsHolder holder = (NumberButtonsHolder) recyclerView.findViewHolderForAdapterPosition(pos);
                    holder.numberButton.setEnabled(false);
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

    @Override
    protected void onStart() {
        super.onStart();
        adapter.startListening();
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
}
