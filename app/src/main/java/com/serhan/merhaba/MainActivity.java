package com.serhan.merhaba;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.Button;
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
    private static final String KUR_IPUCU = "Euro efektif alış kurunu görmek için bir güne dokunun.";

    private final ExecutorService arkaPlan = Executors.newSingleThreadExecutor();
    /** Tarih anahtarı (yyyyMMdd) -> kur sonucu önbelleği; aynı gün tekrar indirilmez. */
    private final Map<Integer, TcmbKur.Sonuc> onbellek = new HashMap<>();

    /** Bugünün tarih anahtarı (yyyyMMdd). */
    private int bugunAnahtar;
    private int bugununYili;
    private int bugununAyi;

    /** Takvimde gösterilen ay. */
    private int yil;
    private int ay;

    private TextView ayBasligi;
    private Button geriButonu;
    private Button ileriButonu;
    private TableLayout tablo;
    private TextView kurBilgisi;

    private TextView seciliHucre;
    /** En son tıklanan günün anahtarı; geç gelen eski yanıtların ekranı ezmemesi için. */
    private int seciliAnahtar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Calendar bugun = Calendar.getInstance();
        bugununYili = bugun.get(Calendar.YEAR);
        bugununAyi = bugun.get(Calendar.MONTH);
        bugunAnahtar = anahtar(bugununYili, bugununAyi, bugun.get(Calendar.DAY_OF_MONTH));
        yil = bugununYili;
        ay = bugununAyi;

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

        ekran.addView(baslikSatiriOlustur());

        tablo = new TableLayout(this);
        tablo.setStretchAllColumns(true);
        ekran.addView(tablo, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        kurBilgisi = new TextView(this);
        kurBilgisi.setTextSize(18);
        kurBilgisi.setGravity(Gravity.CENTER);
        kurBilgisi.setPadding(0, dp(24), 0, 0);
        ekran.addView(kurBilgisi);

        takvimiCiz();
        setContentView(ekran);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        arkaPlan.shutdownNow();
    }

    /** "‹  Kasım 2026  ›" satırı. */
    private LinearLayout baslikSatiriOlustur() {
        LinearLayout satir = new LinearLayout(this);
        satir.setOrientation(LinearLayout.HORIZONTAL);
        satir.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams satirParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        satirParams.topMargin = dp(32);
        satirParams.bottomMargin = dp(8);
        satir.setLayoutParams(satirParams);

        geriButonu = new Button(this);
        geriButonu.setText("‹");
        geriButonu.setTextSize(24);
        geriButonu.setPadding(0, 0, 0, 0);
        geriButonu.setContentDescription("Önceki ay");
        geriButonu.setOnClickListener(v -> ayDegistir(-1));
        satir.addView(geriButonu, new LinearLayout.LayoutParams(dp(56), dp(56)));

        ayBasligi = new TextView(this);
        ayBasligi.setTextSize(24);
        ayBasligi.setTypeface(Typeface.DEFAULT_BOLD);
        ayBasligi.setGravity(Gravity.CENTER);
        satir.addView(ayBasligi, new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        ileriButonu = new Button(this);
        ileriButonu.setText("›");
        ileriButonu.setTextSize(24);
        ileriButonu.setPadding(0, 0, 0, 0);
        ileriButonu.setContentDescription("Sonraki ay");
        ileriButonu.setOnClickListener(v -> ayDegistir(1));
        satir.addView(ileriButonu, new LinearLayout.LayoutParams(dp(56), dp(56)));

        return satir;
    }

    private void ayDegistir(int fark) {
        ay += fark;
        if (ay < 0) {
            ay = 11;
            yil--;
        } else if (ay > 11) {
            ay = 0;
            yil++;
        }
        takvimiCiz();
    }

    /** Gösterilen ayın takvimini (yeniden) çizer; haftalar Pazartesi'den başlar. */
    private void takvimiCiz() {
        seciliHucre = null;
        seciliAnahtar = 0;
        kurBilgisi.setText(KUR_IPUCU);

        ayBasligi.setText(AYLAR[ay] + " " + yil);
        // Gelecek aylarda TCMB kuru olmadığından içinde bulunulan aydan ileri gidilmez.
        boolean buAy = yil == bugununYili && ay == bugununAyi;
        ileriButonu.setEnabled(!buAy);
        ileriButonu.setAlpha(buAy ? 0.3f : 1f);

        tablo.removeAllViews();

        Calendar ayinIlkGunu = Calendar.getInstance();
        ayinIlkGunu.clear();
        ayinIlkGunu.set(yil, ay, 1);
        int gunSayisi = ayinIlkGunu.getActualMaximum(Calendar.DAY_OF_MONTH);
        // Calendar: Pazar=1 ... Cumartesi=7. Pazartesi=0 olacak şekilde çevir.
        int baslangic = (ayinIlkGunu.get(Calendar.DAY_OF_WEEK) + 5) % 7;

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
            int gunAnahtar = anahtar(yil, ay, gun);
            if (gunAnahtar == bugunAnahtar) {
                t.setTextColor(Color.WHITE);
                t.setTypeface(Typeface.DEFAULT_BOLD);
            } else if (sutun >= 5) {
                t.setTextColor(HAFTA_SONU_RENGI);
            }
            hucreArkaPlaniniAyarla(t, gunAnahtar, false);

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
    }

    /** Bugün mavi dolgulu, seçili gün mavi çerçeveli gösterilir. */
    private void hucreArkaPlaniniAyarla(TextView t, int gunAnahtar, boolean secili) {
        boolean bugunMu = gunAnahtar == bugunAnahtar;
        if (!bugunMu && !secili) {
            t.setBackground(null);
            return;
        }
        GradientDrawable arka = new GradientDrawable();
        arka.setCornerRadius(dp(10));
        if (bugunMu) arka.setColor(VURGU_RENGI);
        if (secili) {
            arka.setStroke(dp(2), bugunMu ? Color.parseColor("#0D47A1") : VURGU_RENGI);
        }
        t.setBackground(arka);
    }

    private void gunSecildi(TextView hucre, int gun) {
        int gunAnahtar = anahtar(yil, ay, gun);
        if (seciliHucre != null) {
            hucreArkaPlaniniAyarla(seciliHucre, seciliAnahtar, false);
        }
        seciliHucre = hucre;
        seciliAnahtar = gunAnahtar;
        hucreArkaPlaniniAyarla(hucre, gunAnahtar, true);

        String tarihYazisi = gun + " " + AYLAR[ay] + " " + yil;
        String istenenTarih = String.format(Locale.ROOT, "%02d.%02d.%04d", gun, ay + 1, yil);

        if (gunAnahtar > bugunAnahtar) {
            kurBilgisi.setText(tarihYazisi + "\nBu tarih için TCMB kuru henüz belli değil.");
            return;
        }

        TcmbKur.Sonuc kayitli = onbellek.get(gunAnahtar);
        if (kayitli != null) {
            kurGoster(tarihYazisi, istenenTarih, kayitli);
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
                if (s != null) onbellek.put(gunAnahtar, s);
                // Kullanıcı bu arada başka bir güne ya da aya geçtiyse ekranı değiştirme.
                if (seciliAnahtar != gunAnahtar) return;
                if (s != null) {
                    kurGoster(tarihYazisi, istenenTarih, s);
                } else {
                    kurBilgisi.setText(tarihYazisi + "\n" + h);
                }
            });
        });
    }

    private void kurGoster(String tarihYazisi, String istenenTarih, TcmbKur.Sonuc s) {
        StringBuilder yazi = new StringBuilder();
        yazi.append(tarihYazisi)
                .append("\nTCMB Euro efektif alış: ")
                .append(kurBicimle(s.efektifAlis))
                .append(" ₺");

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

    private static int anahtar(int yil, int ay, int gun) {
        return yil * 10000 + (ay + 1) * 100 + gun;
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
