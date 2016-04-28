package com.coinbase.android.pin;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;

import com.coinbase.android.BuildConfig;
import com.coinbase.android.BuildType;
import com.coinbase.android.CoinbaseActivity;
import com.coinbase.android.CoinbaseActivity.RequiresAuthentication;
import com.coinbase.android.Constants;
import com.coinbase.android.FontManager;
import com.coinbase.android.LoginActivity;
import com.coinbase.android.R;
import com.coinbase.android.Utils;
import com.coinbase.api.LoginManager;
import com.google.inject.Inject;

@RequiresAuthentication
public class PINPromptActivity extends CoinbaseActivity {

  public static final String ACTION_PROMPT = "com.coinbase.android.pin.ACTION_PROMPT";
  public static final String ACTION_SET = "com.coinbase.android.pin.ACTION_SET";

  private boolean mIsSetMode = false;
  private EditText mPinNumberField = null;
  private GridView mKeyboard = null;

  @Inject
  protected PINManager mPinManager;

  @Override
  public void onCreate(Bundle arg0) {
    super.onCreate(arg0);

    mIsSetMode = ACTION_SET.equals(getIntent().getAction());

    if(mIsSetMode && !mPinManager.shouldGrantAccess(this)) {
      finish();
      return;
    }

    if (Utils.inKioskMode(this)) {
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
    }

    setContentView(R.layout.activity_pinprompt);

    ((TextView) findViewById(R.id.pin_account)).setText(mLoginManager.getActiveUserEmail());

    mPinNumberField = ((EditText) findViewById(R.id.pin_number));
    mPinNumberField.setOnEditorActionListener(new OnEditorActionListener() {

      @Override
      public boolean onEditorAction(TextView arg0, int arg1, KeyEvent arg2) {

        onSubmit();
        return true;
      }
    });


    ((TextView) findViewById(R.id.pin_text)).setTypeface(
            FontManager.getFont(this, "Roboto-Light"));
    ((TextView) findViewById(R.id.pin_account)).setTypeface(
            FontManager.getFont(this, "Roboto-Light"));
    ((TextView) findViewById(R.id.pin_number)).setTypeface(
            FontManager.getFont(this, "Roboto-Light"));

    mKeyboard = (GridView) findViewById(R.id.pin_keyboard);
    String[] data = { "1", "2", "3", "4", "5", "6", "7", "8", "9", "<", "0", "D" };
    ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, R.layout.activity_pinprompt_key,
            data) {

      String[] alphaText = new String[] { "", "ABC", "DEF", "GHI", "JKL", "MNO", "PQRS",
              "TUV", "WXYZ", "", "", "" };

      @Override
      public View getView(int position, View convertView, ViewGroup parent) {

        ViewGroup cell = (ViewGroup) convertView;

        if(cell == null) {
          cell = (ViewGroup) View.inflate(PINPromptActivity.this, R.layout.activity_pinprompt_key, null);
        }

        final String index = getItem(position);
        TextView number = (TextView) cell.findViewById(R.id.number);
        TextView alpha = (TextView) cell.findViewById(R.id.alpha);
        TextView submit = (TextView) cell.findViewById(R.id.submit);

        if("D".equals(index)) {

          cell.findViewById(R.id.key).setVisibility(View.INVISIBLE);
          submit.setVisibility(View.VISIBLE);
          submit.setTypeface(FontManager.getFont(PINPromptActivity.this, "RobotoCondensed-Regular"));
          submit.setGravity(Gravity.CENTER);
        } else if("<".equals(index)) {
          cell.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
              mPinNumberField.setText("");
              return true;
            }
          });
        }

        if(!"D".equals(index)) {
          // Reset submit button visibility
          submit.setVisibility(View.INVISIBLE);
          cell.findViewById(R.id.key).setVisibility(View.VISIBLE);
        }

        number.setText(index);
        number.setTypeface(FontManager.getFont(PINPromptActivity.this, "Roboto-Light"));

        alpha.setText(alphaText[position]);
        alpha.setTypeface(FontManager.getFont(PINPromptActivity.this, "Roboto-Light"));


        cell.setOnClickListener(new View.OnClickListener() {
          @Override
          public void onClick(View view) {
            onKeyPressed(index);
          }
        });

        return cell;
      }
    };
    mKeyboard.setAdapter(adapter);
  }

  private void onKeyPressed(String index) {

    if("D".equals(index)) {
      onSubmit();
    } else if("<".equals(index)) {
      String text = mPinNumberField.getText().toString();
      if(text.length() > 0) {
        mPinNumberField.setText(text.substring(0, text.length() - 1));
      }
    } else {
      mPinNumberField.append(index);
    }
  }

  private void onSubmit() {
      String user_pin = mPinNumberField.getText().toString();
    if(mIsSetMode) {
      if(!"".equals(user_pin)) {
        onPinEntered(user_pin);
      }
    } else {

      SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(PINPromptActivity.this);

      if(mPinManager.verifyPin(this.getApplicationContext(), user_pin)) {
        // Correct PIN has been entered.
        onPinEntered(user_pin);
      } else {
        mPinNumberField.setText(null);
        Toast.makeText(PINPromptActivity.this, R.string.pin_incorrect, Toast.LENGTH_SHORT).show();
      }
    }
  }

  private void onPinEntered(String pin) {

    if(mIsSetMode) {
      mPinManager.setPin(this, pin);
      mPinManager.resetPinClock(this);
    } else {
      mPinManager.resetPinClock(this);
    }

    setResult(RESULT_OK);
    finish();
  }

  @Override
  public void finish() {
    super.finish();

    if(!mIsSetMode && BuildConfig.type != BuildType.MERCHANT) {
      overridePendingTransition(R.anim.pin_stop_enter, R.anim.pin_stop_exit);
    }
  }

  @Override
  public void onBackPressed() {
      super.onBackPressed();

      if(!mIsSetMode) {
          mPinManager.setQuitPINLock(true);
      }
  }
}
