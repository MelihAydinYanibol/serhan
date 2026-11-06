# Merhaba

Açıldığında ekranda "Merhaba" ve içinde bulunulan ayın takvimini gösteren basit bir Android uygulaması. Ay başlığının yanındaki ‹ ve › butonlarıyla önceki aylara geçilebilir (içinde bulunulan aydan ileriye gidilmez). Bugünün tarihi mavi ile, hafta sonları kırmızı ile gösterilir.

Takvimde bir güne dokunulduğunda, Türkiye Cumhuriyet Merkez Bankası'nın (TCMB) o gün için belirlediği
**Euro efektif alış kuru** virgülden sonra en fazla 2 haneyle gösterilir. Kurlar
[TCMB kur arşivinden](https://www.tcmb.gov.tr/kurlar/) alınır; hafta sonu ve tatil günlerinde
TCMB kur yayımlamadığından önceki iş gününün kuru gösterilir.

## APK'yı indirme

Her push'ta GitHub Actions APK'yı derler. **Actions** sekmesinden son çalıştırmayı açın
ve **merhaba-apk** dosyasını indirin.

## Bilgisayarda derleme

```
./gradlew assembleDebug
```

APK: `app/build/outputs/apk/debug/app-debug.apk`
