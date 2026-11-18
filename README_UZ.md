# Winter Dream — Android Live Wallpaper

Bu loyiha Android telefon uchun haqiqiy Live Wallpaper ilovasi.

## Hozirgi versiya
- Qishki tungi manzara ilova ichida Canvas orqali chiziladi; tashqi rasm fayliga bog‘liq emas.
- Ekranda doimiy qor yog‘adi.
- Ekranga tegilganda yaqin qor parchalari chetga suriladi va yengil yorug‘lik effekti paydo bo‘ladi.
- Kotlin ishlatilmaydi: loyiha Java 17 bilan tuzilgan.
- APK GitHub Actions orqali yig‘iladi.

## GitHub Actions
Har bir `main` branch pushidan so‘ng yoki Actions ichidagi `Run workflow` orqali APK yig‘iladi.
Muvaffaqiyatli yig‘ilgach, `WinterDream-debug-apk` artifactini yuklab olish mumkin.

APK yo‘li:
`app/build/outputs/apk/debug/app-debug.apk`

## Telefonga o‘rnatish
1. APK faylini telefonga yuklab oling.
2. Android so‘raganda ushbu APK uchun o‘rnatishga ruxsat bering.
3. Ilovani o‘rnating.
4. Telefonning Wallpaper / Fon rasmi bo‘limidan Live Wallpapers ichidan Winter Dream ni tanlang.
5. Home screen yoki Home and lock screens ni tanlang.

## Muhim
Bu boshlang‘ich, ishlaydigan Live Wallpaper prototipi. Hozircha qor odam yasash, qor to‘pi uloqtirish, shamol sozlamalari va chuqur parallax kiritilmagan. Ular keyingi bosqichlarda qo‘shilishi mumkin.
