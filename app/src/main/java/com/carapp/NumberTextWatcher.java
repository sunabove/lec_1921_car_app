package com.carapp;

import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;

/**
 * Created by sunabove on 2016-03-03.
 */
public class NumberTextWatcher implements TextWatcher {

    private EditText editText ;
    public NumberTextWatcher( EditText editText ) {
        this.editText = editText ;

    }

    boolean isManualChange = false;

    @Override
    public void onTextChanged(CharSequence s, int start, int before,
                              int count) {
        if (isManualChange) {
            isManualChange = false;
            return;
        }

        try {
            String value = s.toString().replace(",", "");
            String reverseValue = new StringBuilder(value).reverse()
                    .toString();
            StringBuilder finalValue = new StringBuilder();
            for (int i = 1; i <= reverseValue.length(); i++) {
                char val = reverseValue.charAt(i - 1);
                finalValue.append(val);
                if (i % 3 == 0 && i != reverseValue.length() && i > 0) {
                    finalValue.append(",");
                }
            }
            isManualChange = true;
            editText.setText(finalValue.reverse());
            editText.setSelection(finalValue.length());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
    }

    @Override
    public void afterTextChanged(Editable s) {
    }
}
