package com.zak.soundlibrary;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.os.Build;
import android.text.TextUtils;
import android.util.JsonReader;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.RequiresApi;

import com.sac.speech.GoogleVoiceTypingDisabledException;
import com.sac.speech.Speech;
import com.sac.speech.SpeechDelegate;
import com.sac.speech.SpeechRecognitionNotAvailable;
import com.tbruyelle.rxpermissions.RxPermissions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

/**
 * The type Delegator.
 */
public class Delegator {
    private static String language;
    private final Activity context;
    private Callback callback;
    private TestService service;
    private Speech speech;
    private HashMap<String, String> codeBase;


    /**
     * Instantiates a new Delegator.
     *
     * @param applicationContext the application context
     * @param lang               the lang : arabic , english
     * @param callback           the callback
     */
    public Delegator(Activity applicationContext, String lang, Callback callback) {
        this.context = applicationContext;
        language = lang;
        this.callback = callback;
        codeBase = new HashMap<String, String>();
         loadJson();
         getData();
        loadCodeBse();
    }


    /**
     * Start listen.
     */
    public void startListen() {
        service = new TestService(context, language, callback);
        service.startListen();
    }

    private String loadJson() {
        String json = null;
        try {
            InputStream is = context.getAssets().open("base.json");
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            json = new String(buffer, "UTF-8");
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
        return json;
    }

    private String getData() {
        try {
            JSONObject obj = new JSONObject(loadJson());
            JSONArray codes = obj.getJSONArray("codes");
            for (int i = 0; i < codes.length(); i++) {
                System.out.println("codes.get(0) = " + codes.get(0));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    private void loadCodeBse() {
        codeBase.put("عرض", "001");
        codeBase.put("عروض", "001");
        codeBase.put("العرض", "001");
        codeBase.put("العروض", "001");
        codeBase.put("اعرض", "001");
        codeBase.put("معروض", "001");

    }

    public void addToBase(String key, String value) {
        codeBase.put(key, value);
    }


    private class TestService implements SpeechDelegate, Speech.stopDueToDelay {

        private String language;
        private final Activity context;
        private AlertDialog alertDialog;
        private Callback callback;


        private TestService(Activity applicationContext, String lang, Callback callback) {
            this.context = applicationContext;
            language = lang;
            this.callback = callback;
        }

        /*
         * start listen service
         * show dialog
         * start listening
         * takes permission
         * */
        private void startListen() {
            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(context);
            alertDialogBuilder.setMessage("Listening").setCancelable(false);
            alertDialog = alertDialogBuilder.create();
            alertDialog.show();
            speech = Speech.init(context);

            speech.setListener(this);
            Locale locale = new Locale(language);
            speech.setLocale(locale);
            if (speech.isListening()) {
                speech.stopListening();
                muteBeepSoundOfRecorder();
            } else {
                System.setProperty("rx.unsafe-disable", "True");
                RxPermissions.getInstance(context).request(Manifest.permission.RECORD_AUDIO).subscribe(granted -> {
                    if (granted) {
                        try {
                            speech.stopTextToSpeech();
                            speech.startListening(null, this);

                        } catch (SpeechRecognitionNotAvailable exc) {
                            //showSpeechNotSupportedDialog();
                            exc.printStackTrace();

                        } catch (GoogleVoiceTypingDisabledException exc) {
                            //showEnableGoogleVoiceTyping();
                            exc.printStackTrace();
                        }
                    } else {
                        Toast.makeText(context, "permission required", Toast.LENGTH_LONG).show();
                    }
                });
                muteBeepSoundOfRecorder();
            }
        }

        /*
         * stop service when no sound received
         * */
        @Override
        public void onSpecifiedCommandPronounced(String event) {
            if (event.equals("1")) {
                stopServices();
            }

        }

        @Override
        public void onStartOfSpeech() {

        }


        @Override
        public void onSpeechRmsChanged(float value) {

        }

        /*
         * on speech partial results
         * print to log segmented message
         * */
        @Override
        public void onSpeechPartialResults(List<String> results) {
            for (String partial : results) {
                Log.d("Result", partial + "");
            }
        }

        /*
         * on speech
         * return result by calling callback interface when done
         * */
        @Override
        public void onSpeechResult(String result) {
            String[] results = result.split(" ");

            if (!TextUtils.isEmpty(result)) {
                {
                    stopServices();
                    for (int i = 0; i < results.length; i++) {
                        System.out.println("results[i] = " + results[i]);
                        if (codeBase.containsKey(results[i])) {
                            callback.onDone((String) codeBase.get(results[i]));
                            break;
                        } else {
                            callback.onDone("000");
                        }
                    }
                }
            }
        }

        /**
         * Stop services
         * dismiss popup dialog
         * stop listening
         * shutdown speech service
         */
        public void stopServices() {
            alertDialog.dismiss();
            speech.stopTextToSpeech();
            speech.stopListening();
            speech.shutdown();
        }

        /*
         * mute redundant sounds
         * */
        private void muteBeepSoundOfRecorder() {

            AudioManager manager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);

            if (manager != null) {
                manager.adjustStreamVolume(AudioManager.STREAM_NOTIFICATION, AudioManager.ADJUST_MUTE, 0);
                manager.adjustStreamVolume(AudioManager.STREAM_ALARM, AudioManager.ADJUST_MUTE, 0);
                manager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_MUTE, 0);
                manager.adjustStreamVolume(AudioManager.STREAM_RING, AudioManager.ADJUST_MUTE, 0);
                manager.adjustStreamVolume(AudioManager.STREAM_SYSTEM, AudioManager.ADJUST_MUTE, 0);
            }
        }


    }

}