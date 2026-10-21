package com.aozora.aozora;

import android.content.Intent;
import android.os.SystemClock;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.security.SecureRandom;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * パスワード解除済みであることをアプリ内の画面へ渡すための「1 回だけ使える合言葉」。
 *
 * 以前は Intent に authenticated=true を付けるだけだったので、MainActivity は外部アプリからも
 * 起動できる(exported)ため、他のアプリが同じ値を付けて起動するとパスワード画面を飛ばせた。
 *
 * ここで発行する合言葉はこのアプリのプロセスのメモリ上にしか無く、
 * <ul>
 *   <li>推測できない乱数(128 ビット)</li>
 *   <li>1 回使うと無効({@link #consume})</li>
 *   <li>発行から {@link #VALID_MS} を過ぎると無効</li>
 * </ul>
 * なので、外部アプリが Intent に何を付けても解除済みとして扱われることは無い。
 *
 * 画面の作り直し(回転・テーマ変更など)で合言葉をもう一度使えない問題は、
 * {@link #PROCESS_NONCE} を onSaveInstanceState に残すことで解決する(同じプロセス内の作り直しだけ
 * 解除済みを引き継ぎ、プロセスが終了した後の復元では引き継がない)。
 */
final class AuthTicket {

    static final String EXTRA = "com.aozora.aozora.extra.AUTH_TICKET";
    /** 以前の形式。外部から付けられても無視するため、受け取った Intent から取り除く。 */
    static final String LEGACY_EXTRA = "authenticated";

    private static final long VALID_MS = 60_000;
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Map<String, Long> ISSUED = new ConcurrentHashMap<>();

    /** このプロセスだけの値。保存した画面状態が同じプロセスのものかを見分ける。 */
    static final String PROCESS_NONCE = newToken();

    private AuthTicket() {}

    /** パスワードの解除に成功した時に、次の画面へ渡す合言葉を発行する。 */
    @NonNull
    static String issue() {
        String token = newToken();
        ISSUED.put(token, SystemClock.elapsedRealtime());
        return token;
    }

    /** 解除済みの印を Intent に付ける(アプリ内の画面遷移用)。 */
    static void attach(@NonNull Intent intent) {
        intent.removeExtra(LEGACY_EXTRA);
        intent.putExtra(EXTRA, issue());
    }

    /** Intent の合言葉が有効なら true(使ったら無効にする)。Intent からは印を取り除く。 */
    static boolean consume(@Nullable Intent intent) {
        if (intent == null) return false;
        String token = null;
        try {
            token = intent.getStringExtra(EXTRA);
        } catch (Exception ignored) {
            // 外部から不正な型の値を付けられても落ちないようにする
        }
        intent.removeExtra(EXTRA);
        intent.removeExtra(LEGACY_EXTRA);
        if (token == null) return false;
        Long issuedAt = ISSUED.remove(token);
        return issuedAt != null && SystemClock.elapsedRealtime() - issuedAt <= VALID_MS;
    }

    private static String newToken() {
        byte[] b = new byte[16];
        RANDOM.nextBytes(b);
        StringBuilder sb = new StringBuilder(32);
        for (byte x : b) sb.append(String.format("%02x", x));
        return sb.toString();
    }
}
