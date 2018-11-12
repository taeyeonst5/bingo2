package com.allen_chou.bingo;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.Button;

public class NumberButton extends android.support.v7.widget.AppCompatButton {
    int number;
    boolean selected;
    int position;

    public NumberButton(Context context) {
        super(context);
    }

    public NumberButton(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public int getNumber() {
        return number;
    }

    public void setNumber(int number) {
        this.number = number;
    }

    @Override
    public boolean isSelected() {
        return selected;
    }

    @Override
    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }
}
