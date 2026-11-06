package com.serhan.merhaba;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import java.util.Calendar;

public class MainActivity extends Activity {

    private static final String[] AYLAR = {
            "Ocak", "Şubat", "Mart", "Nisan", "Mayıs", "Haziran",
            "Temmuz", "Ağustos", "Eylül", "Ekim", "Kasım", "Aralık"
    };

    private static final String[] GUNLER = {"Pzt", "Sal", "Çar", "Per", "Cum", "Cmt", "Paz"};

    private static final int VURGU_RENGI = Color.parseColor("#1E88E5");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout ekran = new LinearLayout(this);
        ekran.setOrientation(LinearLayout.VERTICAL);
        ekran.setGravity(Gravity.CENTER_HORIZONTAL);
        int kenar = dp(16);
        ekran.setPadding(kenar, dp(48), kenar, kenar);

        TextView merhaba = new TextView(this);
        merhaba.setText(R.string.merhaba);
        merhaba.setTextSize(40);
        merhaba.setGravity(Gravity.CENTER);
        ekran.addView(merhaba);

        ekran.addView(takvimOlustur());

        setContentView(ekran);
    }

    /** İçinde bulunulan ayın takvimini oluşturur (haftalar Pazartesi'den başlar). */
    private LinearLayout takvimOlustur() {
        Calendar bugun = Calendar.getInstance();
        int yil = bugun.get(Calendar.YEAR);
        int ay = bugun.get(Calendar.MONTH);
        int bugununGunu = bugun.get(Calendar.DAY_OF_MONTH);

        Calendar ayinIlkGunu = Calendar.getInstance();
        ayinIlkGunu.clear();
        ayinIlkGunu.set(yil, ay, 1);
        int gunSayisi = ayinIlkGunu.getActualMaximum(Calendar.DAY_OF_MONTH);
        // Calendar: Pazar=1 ... Cumartesi=7. Pazartesi=0 olacak şekilde çevir.
        int baslangic = (ayinIlkGunu.get(Calendar.DAY_OF_WEEK) + 5) % 7;

        LinearLayout kutu = new LinearLayout(this);
        kutu.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams kutuParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        kutuParams.topMargin = dp(32);
        kutu.setLayoutParams(kutuParams);

        TextView baslik = new TextView(this);
        baslik.setText(AYLAR[ay] + " " + yil);
        baslik.setTextSize(24);
        baslik.setTypeface(Typeface.DEFAULT_BOLD);
        baslik.setGravity(Gravity.CENTER);
        baslik.setPadding(0, 0, 0, dp(16));
        kutu.addView(baslik);

        TableLayout tablo = new TableLayout(this);
        tablo.setStretchAllColumns(true);

        TableRow gunBasliklari = new TableRow(this);
        for (int i = 0; i < 7; i++) {
            TextView t = hucre(GUNLER[i]);
            t.setTypeface(Typeface.DEFAULT_BOLD);
            if (i >= 5) t.setTextColor(Color.parseColor("#E53935"));
            gunBasliklari.addView(t);
        }
        tablo.addView(gunBasliklari);

        TableRow satir = new TableRow(this);
        for (int i = 0; i < baslangic; i++) {
            satir.addView(hucre(""));
        }
        for (int gun = 1; gun <= gunSayisi; gun++) {
            TextView t = hucre(String.valueOf(gun));
            int sutun = (baslangic + gun - 1) % 7;
            if (gun == bugununGunu) {
                GradientDrawable kutucuk = new GradientDrawable();
                kutucuk.setCornerRadius(dp(10));
                kutucuk.setColor(VURGU_RENGI);
                t.setBackground(kutucuk);
                t.setTextColor(Color.WHITE);
                t.setTypeface(Typeface.DEFAULT_BOLD);
            } else if (sutun >= 5) {
                t.setTextColor(Color.parseColor("#E53935"));
            }
            satir.addView(t);
            if (sutun == 6) {
                tablo.addView(satir);
                satir = new TableRow(this);
            }
        }
        if (satir.getChildCount() > 0) {
            while (satir.getChildCount() < 7) satir.addView(hucre(""));
            tablo.addView(satir);
        }

        kutu.addView(tablo);
        return kutu;
    }

    private TextView hucre(String yazi) {
        TextView t = new TextView(this);
        t.setText(yazi);
        t.setTextSize(18);
        t.setGravity(Gravity.CENTER);
        int boyut = dp(40);
        TableRow.LayoutParams p = new TableRow.LayoutParams(boyut, boyut);
        p.setMargins(dp(2), dp(2), dp(2), dp(2));
        t.setLayoutParams(p);
        return t;
    }

    private int dp(int deger) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, deger, getResources().getDisplayMetrics());
    }
}
