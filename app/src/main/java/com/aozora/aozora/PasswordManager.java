package com.aozora.aozora;

import android.content.Context;
import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;

import java.nio.charset.StandardCharsets;
import java.security.KeyStore;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

public class PasswordManager {

    private static final String PREF_NAME = "password_prefs";
    private static final String KEY_PASSWORD = "encrypted_password";

    private static final String ANDROID_KEYSTORE = "AndroidKeyStore";
    private static final String KEY_ALIAS = "AppPasswordKey";

    private static final String TRANSFORMATION =
            "AES/GCM/NoPadding";

    private final Context context;

    public PasswordManager(Context context) {
        this.context = context.getApplicationContext();
    }

    /**
     * パスワードが設定されているか
     */
    public boolean isPasswordSet() {
        SharedPreferences prefs =
                context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);

        return prefs.contains(KEY_PASSWORD);
    }

    /**
     * パスワードを暗号化して保存
     */
    public boolean setPassword(String password) {
        try {
            SecretKey secretKey = getOrCreateKey();

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);

            byte[] encrypted =
                    cipher.doFinal(password.getBytes(StandardCharsets.UTF_8));

            byte[] iv = cipher.getIV();

            String encryptedText =
                    Base64.encodeToString(encrypted, Base64.NO_WRAP);

            String ivText =
                    Base64.encodeToString(iv, Base64.NO_WRAP);

            SharedPreferences prefs =
                    context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);

            prefs.edit()
                    .putString(KEY_PASSWORD, encryptedText)
                    .putString("password_iv", ivText)
                    .apply();

            return true;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 入力されたパスワードが正しいか
     */
    public boolean verifyPassword(String password) {
        try {
            SharedPreferences prefs =
                    context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);

            String encryptedText =
                    prefs.getString(KEY_PASSWORD, null);

            String ivText =
                    prefs.getString("password_iv", null);

            if (encryptedText == null || ivText == null) {
                return false;
            }

            SecretKey secretKey = getOrCreateKey();

            byte[] encrypted =
                    Base64.decode(encryptedText, Base64.NO_WRAP);

            byte[] iv =
                    Base64.decode(ivText, Base64.NO_WRAP);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);

            GCMParameterSpec spec =
                    new GCMParameterSpec(128, iv);

            cipher.init(
                    Cipher.DECRYPT_MODE,
                    secretKey,
                    spec
            );

            byte[] decrypted =
                    cipher.doFinal(encrypted);

            String savedPassword =
                    new String(decrypted, StandardCharsets.UTF_8);

            return savedPassword.equals(password);

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * パスワードを削除
     */
    public void removePassword() {
        SharedPreferences prefs =
                context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);

        prefs.edit()
                .remove(KEY_PASSWORD)
                .remove("password_iv")
                .apply();

        try {
            KeyStore keyStore = KeyStore.getInstance(ANDROID_KEYSTORE);
            keyStore.load(null);

            if (keyStore.containsAlias(KEY_ALIAS)) {
                keyStore.deleteEntry(KEY_ALIAS);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * AES鍵を取得。
     * 存在しなければ新しく作成する。
     */
    private SecretKey getOrCreateKey() throws Exception {

        KeyStore keyStore =
                KeyStore.getInstance(ANDROID_KEYSTORE);

        keyStore.load(null);

        if (keyStore.containsAlias(KEY_ALIAS)) {

            KeyStore.Entry entry =
                    keyStore.getEntry(KEY_ALIAS, null);

            return ((KeyStore.SecretKeyEntry) entry).getSecretKey();
        }

        KeyGenerator keyGenerator =
                KeyGenerator.getInstance(
                        KeyProperties.KEY_ALGORITHM_AES,
                        ANDROID_KEYSTORE
                );

        KeyGenParameterSpec spec =
                new KeyGenParameterSpec.Builder(
                        KEY_ALIAS,
                        KeyProperties.PURPOSE_ENCRYPT |
                                KeyProperties.PURPOSE_DECRYPT
                )
                        .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                        .setEncryptionPaddings(
                                KeyProperties.ENCRYPTION_PADDING_NONE
                        )
                        .setKeySize(256)
                        .build();

        keyGenerator.init(spec);

        return keyGenerator.generateKey();
    }

    /**
     * PasswordManagerの設定をすべて削除
     */
    public void clearAll() {

        // SharedPreferencesを完全削除
        SharedPreferences prefs =
                context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);

        prefs.edit()
                .clear()
                .apply();

        // Android Keystoreの暗号鍵を削除
        try {
            KeyStore keyStore =
                    KeyStore.getInstance(ANDROID_KEYSTORE);

            keyStore.load(null);

            if (keyStore.containsAlias(KEY_ALIAS)) {
                keyStore.deleteEntry(KEY_ALIAS);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}