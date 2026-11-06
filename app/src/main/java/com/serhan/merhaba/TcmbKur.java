package com.serhan.merhaba;

import android.util.Xml;

import org.xmlpull.v1.XmlPullParser;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Calendar;
import java.util.Locale;

/**
 * TCMB'nin günlük kur arşivinden (https://www.tcmb.gov.tr/kurlar/) Euro efektif alış kurunu okur.
 * Hafta sonu ve tatil günlerinde TCMB kur yayımlamaz; bu durumda önceki iş gününün kuru kullanılır.
 */
public final class TcmbKur {

    /** Bir kur sorgusunun sonucu. */
    public static final class Sonuc {
        /** Euro efektif alış kuru (1 EUR için TL). */
        public final double efektifAlis;
        /** Kurun ait olduğu TCMB bülten tarihi (gg.aa.yyyy). */
        public final String bultenTarihi;

        Sonuc(double efektifAlis, String bultenTarihi) {
            this.efektifAlis = efektifAlis;
            this.bultenTarihi = bultenTarihi;
        }
    }

    /** Kur yayımlanmamış bir gün için geriye en fazla kaç gün bakılacağı (uzun bayram tatilleri dahil). */
    private static final int EN_FAZLA_GERI_GUN = 14;

    private TcmbKur() {
    }

    /**
     * Verilen gün için geçerli Euro efektif alış kurunu döndürür. Ağ işlemi yaptığı için
     * ana iş parçacığında çağrılmamalıdır.
     *
     * @return kur bulunamazsa null
     */
    public static Sonuc euroEfektifAlis(Calendar gun) throws Exception {
        Calendar c = (Calendar) gun.clone();
        for (int i = 0; i <= EN_FAZLA_GERI_GUN; i++) {
            Sonuc s = bultenOku(c);
            if (s != null) return s;
            c.add(Calendar.DAY_OF_MONTH, -1);
        }
        return null;
    }

    /** Tek bir günün bültenini okur; o gün bülten yoksa null döner. */
    private static Sonuc bultenOku(Calendar gun) throws Exception {
        int yil = gun.get(Calendar.YEAR);
        int ay = gun.get(Calendar.MONTH) + 1;
        int g = gun.get(Calendar.DAY_OF_MONTH);
        String adres = String.format(Locale.ROOT,
                "https://www.tcmb.gov.tr/kurlar/%04d%02d/%02d%02d%04d.xml", yil, ay, g, ay, yil);

        HttpURLConnection baglanti = (HttpURLConnection) new URL(adres).openConnection();
        baglanti.setConnectTimeout(10000);
        baglanti.setReadTimeout(10000);
        baglanti.setRequestProperty("User-Agent", "Mozilla/5.0 (Android) Merhaba");
        try {
            int kod = baglanti.getResponseCode();
            if (kod == HttpURLConnection.HTTP_NOT_FOUND) return null;
            if (kod != HttpURLConnection.HTTP_OK) {
                throw new Exception("TCMB yanıt kodu: " + kod);
            }
            try (InputStream giris = baglanti.getInputStream()) {
                return xmlCoz(giris);
            }
        } catch (FileNotFoundException e) {
            return null;
        } finally {
            baglanti.disconnect();
        }
    }

    private static Sonuc xmlCoz(InputStream giris) throws Exception {
        XmlPullParser p = Xml.newPullParser();
        p.setInput(giris, null);

        String bultenTarihi = null;
        boolean euroIcinde = false;
        int olay = p.getEventType();
        while (olay != XmlPullParser.END_DOCUMENT) {
            if (olay == XmlPullParser.START_TAG) {
                String ad = p.getName();
                if ("Tarih_Date".equals(ad)) {
                    bultenTarihi = p.getAttributeValue(null, "Tarih");
                } else if ("Currency".equals(ad)) {
                    euroIcinde = "EUR".equals(p.getAttributeValue(null, "CurrencyCode"))
                            || "EUR".equals(p.getAttributeValue(null, "Kod"));
                } else if (euroIcinde && "BanknoteBuying".equals(ad)) {
                    String deger = p.nextText().trim().replace(',', '.');
                    if (deger.isEmpty()) return null;
                    return new Sonuc(Double.parseDouble(deger), bultenTarihi);
                }
            } else if (olay == XmlPullParser.END_TAG && "Currency".equals(p.getName())) {
                euroIcinde = false;
            }
            olay = p.next();
        }
        return null;
    }
}
