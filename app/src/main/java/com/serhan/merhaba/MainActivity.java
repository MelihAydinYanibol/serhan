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

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {

    private static final String[] AYLAR = {
            "Ocak", "Şubat", "Mart", "Nisan", "Mayıs", "Haziran",
            "Temmuz", "Ağustos", "Eylül", "Ekim", "Kasım", "Aralık"
    };

    private static final String[] GUNLER = {"Pzt", "Sal", "Çar", "Per", "Cum", "Cmt", "Paz"};

    private static final int VURGU_RENGI = Color.parseColor("#1E88E5");
    private static final int HAFTA_SONU_RENGI = Color.parseColor("#E53935");

    private final ExecutorService arkaPlan = Executors.newSingleThreadExecutor();
    /** Gün -> kur sonucu önbelleği (aynı gün tekrar tıklanınca yeniden indirilmez). */
    private final Map<Integer, TcmbKur.Sonuc> onbellek = new HashMap<>();

    private int yil;
    private int ay;
    private int bugununGunu;

    private TextView kurBilgisi;
    private TextView seciliHucre;
    /** En son tıklanan gün; geç gelen eski yanıtların ekranı ezmemesi için. */
    private int seciliGun;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Calendar bugun = Calendar.getInstance();
        yil = bugun.get(Calendar.YEAR);
        ay = bugun.get(Calendar.MONTH);
        bugununGunu = bugun.get(Calendar.DAY_OF_MONTH);

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

        kurBilgisi = new TextView(this);
        kurBilgisi.setTextSize(18);
        kurBilgisi.setGravity(Gravity.CENTER);
        kurBilgisi.setPadding(0, dp(24), 0, 0);
        kurBilgisi.setText("Euro efektif alış kurunu görmek için bir güne dokunun.");
        ekran.addView(kurBilgisi);

        setContentView(ekran);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        arkaPlan.shutdownNow();
    }

    /** İçinde bulunulan ayın takvimini oluşturur (haftalar Pazartesi'den başlar). */
    private LinearLayout takvimOlustur() {
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
            if (i >= 5) t.setTextColor(HAFTA_SONU_RENGI);
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
                t.setTextColor(Color.WHITE);
                t.setTypeface(Typeface.DEFAULT_BOLD);
            } else if (sutun >= 5) {
                t.setTextColor(HAFTA_SONU_RENGI);
            }
            hucreArkaPlaniniAyarla(t, gun, false);

            final int tiklananGun = gun;
            t.setClickable(true);
            t.setOnClickListener(v -> gunSecildi((TextView) v, tiklananGun));

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

    /** Bugün mavi dolgulu, seçili gün mavi çerçeveli gösterilir. */
    private void hucreArkaPlaniniAyarla(TextView t, int gun, boolean secili) {
        if (gun != bugununGunu && !secili) {
            t.setBackground(null);
            return;
        }
        GradientDrawable arka = new GradientDrawable();
        arka.setCornerRadius(dp(10));
        if (gun == bugununGunu) arka.setColor(VURGU_RENGI);
        if (secili) {
            arka.setStroke(dp(2), gun == bugununGunu ? Color.parseColor("#0D47A1") : VURGU_RENGI);
        }
        t.setBackground(arka);
    }

    private void gunSecildi(TextView hucre, int gun) {
        if (seciliHucre != null) {
            hucreArkaPlaniniAyarla(seciliHucre, seciliGun, false);
        }
        seciliHucre = hucre;
        seciliGun = gun;
        hucreArkaPlaniniAyarla(hucre, gun, true);

        String tarihYazisi = gun + " " + AYLAR[ay] + " " + yil;

        if (gun > bugununGunu) {
            kurBilgisi.setText(tarihYazisi + "\nBu tarih için TCMB kuru henüz belli değil.");
            return;
        }

        TcmbKur.Sonuc kayitli = onbellek.get(gun);
        if (kayitli != null) {
            kurGoster(tarihYazisi, kayitli);
            return;
        }

        kurBilgisi.setText(tarihYazisi + "\nKur yükleniyor…");
        Calendar istenen = Calendar.getInstance();
        istenen.clear();
        istenen.set(yil, ay, gun);

        arkaPlan.execute(() -> {
            TcmbKur.Sonuc sonuc = null;
            String hata = null;
            try {
                sonuc = TcmbKur.euroEfektifAlis(istenen);
                if (sonuc == null) hata = "Bu tarih için TCMB kuru bulunamadı.";
            } catch (Exception e) {
                hata = "Kur alınamadı. İnternet bağlantınızı kontrol edin.";
            }
            final TcmbKur.Sonuc s = sonuc;
            final String h = hata;
            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed()) return;
                if (s != null) onbellek.put(gun, s);
                if (seciliGun != gun) return; // Kullanıcı bu arada başka bir güne dokundu.
                if (s != null) {
                    kurGoster(tarihYazisi, s);
                } else {
                    kurBilgisi.setText(tarihYazisi + "\n" + h);
                }
            });
        });
    }

    private void kurGoster(String tarihYazisi, TcmbKur.Sonuc s) {
        StringBuilder yazi = new StringBuilder();
        yazi.append(tarihYazisi)
                .append("\nTCMB Euro efektif alış: ")
                .append(kurBicimle(s.efektifAlis))
                .append(" ₺");

        String istenenTarih = String.format(Locale.ROOT, "%02d.%02d.%04d", seciliGun, ay + 1, yil);
        if (s.bultenTarihi != null && !s.bultenTarihi.equals(istenenTarih)) {
            yazi.append("\n(Bu gün kur yayımlanmadı; ")
                    .append(s.bultenTarihi)
                    .append(" tarihli kur gösteriliyor.)");
        }
        kurBilgisi.setText(yazi);
    }

    /** Virgülden sonra en fazla 2 hane, Türkçe biçim (ör. 36,12). */
    private static String kurBicimle(double kur) {
        DecimalFormat bicim = new DecimalFormat("#,##0.##",
                DecimalFormatSymbols.getInstance(new Locale("tr", "TR")));
        bicim.setRoundingMode(RoundingMode.HALF_UP);
        return bicim.format(kur);
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
