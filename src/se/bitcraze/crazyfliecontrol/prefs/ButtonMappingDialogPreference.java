package se.bitcraze.crazyfliecontrol.prefs;

import se.bitcraze.crazyfliecontrol.R;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnKeyListener;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.LinearLayout;
import android.widget.TextView;

public class ButtonMappingDialogPreference extends DialogPreference implements OnKeyListener{

    private TextView mValueTextView;
    private String mKeyCode;

    public ButtonMappingDialogPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onPrepareDialogBuilder(Builder builder) {
        super.onPrepareDialogBuilder(builder);

        builder.setOnKeyListener(this);
//        builder.setNeutralButton(R.string.reset_key, new OnClickListener() {
//
//            @Override
//            public void onClick(DialogInterface dialog, int which) {
//                keyCode = 0;
//                valueTextView.setText("No Key");
//            }
//        });
    }

    @Override
    protected View onCreateDialogView() {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        LinearLayout layout = new LinearLayout(getContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(6,6,6,6);

        TextView promptTextView = new TextView(getContext());
        promptTextView.setText(R.string.preferences_button_mapping_dialog_text);
        promptTextView.setGravity(Gravity.CENTER_HORIZONTAL);

        mValueTextView = new TextView(getContext());
        mValueTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22);
        mValueTextView.setGravity(Gravity.CENTER_HORIZONTAL);
        mValueTextView.setPadding(0, 12, 0, 12);

        layout.addView(promptTextView, params);
        layout.addView(mValueTextView, params);

        return layout;
    }
    
    @Override
    protected void onSetInitialValue(boolean restorePersistedValue, Object defaultValue) {
        super.onSetInitialValue(restorePersistedValue, defaultValue);
        if(restorePersistedValue) {
            mKeyCode = getPersistedString((String) defaultValue);
        } else {
            mKeyCode = (String) defaultValue;
        }
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        if(positiveResult) {
            persistString(mKeyCode);
        }
        super.onDialogClosed(positiveResult);
    }

    @Override
    protected void onBindDialogView(View view) {
        super.onBindDialogView(view);
        if(mKeyCode == null || mKeyCode.equals("")) {
            mValueTextView.setText("No Key");
        } else {
            mValueTextView.setText(mKeyCode);
        }
    }

    @Override
    public boolean onKey(DialogInterface dialog, int pKeyCode, KeyEvent event) {
        if(pKeyCode != KeyEvent.KEYCODE_BACK) {
            this.mKeyCode = KeyEvent.keyCodeToString(pKeyCode);
            mValueTextView.setText(mKeyCode);
        } else {
            dialog.dismiss();
        }
        return true;
    }

}