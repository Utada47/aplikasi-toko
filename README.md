# Kasir Aksesoris

Aplikasi kasir Android **ringan**, **offline**, **tanpa login**, untuk toko aksesoris HP kecil.

## Fitur
- **Dashboard**: tombol besar "Mulai Buka Hari Ini", 2 tabel (Barang & Transaksi/Pulsa), tombol tambah (FAB), tombol Tutup Hari dengan konfirmasi modal, total pemasukan real-time.
- **Aturan hari kerja**: jika sesi dibuka hari A dan baru ditutup hari C, semua transaksi selama itu tetap tercatat sebagai pemasukan **hari A** (lihat `Repository.mulaiHariIni()` & `Session.tanggalMulai`).
- **Statistik**: daftar pemasukan per hari, dikelompokkan berdasarkan tanggal mulai sesi.
- **Tema terang/gelap**: toggle langsung dari dashboard (pojok kanan atas), tersimpan otomatis (`ThemeHelper`).
- **Import/Export CSV**: untuk migrasi data ke versi aplikasi baru atau ekspor untuk keperluan penelitian. Lihat menu titik-tiga di dashboard.
- **100% offline**: memakai Room (SQLite) lokal, tidak ada dependensi jaringan/login.

## Arsitektur & alasan "ringan"
- Kotlin + Android Views (XML) + ViewBinding — **bukan Jetpack Compose**, supaya ukuran APK dan overhead render lebih kecil untuk device low-end.
- Room untuk penyimpanan lokal (SQLite), sudah teroptimasi dan kecil.
- Tidak ada library gambar/jaringan (Glide, Retrofit, dsb) karena tidak dibutuhkan.
- `minifyEnabled true` + `shrinkResources true` sudah diaktifkan di build release untuk memperkecil ukuran APK.

## Struktur folder penting
```
app/src/main/java/com/tokoaksesoris/kasir/
├── data/            # Entity, DAO, Database, Repository (Room)
├── ui/dashboard/    # Dashboard: Fragment, ViewModel, Adapter, Dialog tambah item
├── ui/statistik/    # Statistik: Fragment, ViewModel, Adapter
├── utils/           # ThemeHelper (tema), CsvHelper (import/export migrasi)
└── MainActivity.kt  # Host Repository + BottomNavigationView + launcher import/export
```

## Format CSV (untuk migrasi & penelitian)
Satu file CSV, kolom:
```
row_type,session_id,tanggal_mulai,waktu_mulai,waktu_tutup,is_open,tipe,nama,harga,waktu_item
```
- Baris `SESSION` = satu sesi hari buka (kolom sesi terisi, kolom item kosong)
- Baris `ITEM` = satu baris barang/transaksi (session_id merujuk ke sesi di atas, kolom item terisi)
- Semua nilai tanggal/jam dalam epoch millis (angka), supaya ringan & tidak ambigu parsing.
- Saat **import**, `session_id` lama dipetakan ulang otomatis ke ID baru di database (tidak akan bentrok/menimpa data yang sudah ada — bersifat menambahkan/append).
- Saat versi aplikasi berikutnya menambah kolom baru, cukup tambahkan nama kolom baru di header; kolom yang tidak dikenali importer versi lama akan diabaikan (backward compatible).

## Cara membuka di Android Studio
1. Buka Android Studio → **Open** → pilih folder `KasirAksesoris` ini.
2. Tunggu Gradle sync selesai (butuh koneksi internet pertama kali untuk download dependency).
3. Jalankan di emulator/HP (Run ▶).

## Catatan
- Ikon launcher (`ic_launcher.xml`) masih placeholder vector sederhana — silakan ganti lewat **Image Asset Studio** (klik kanan `res` → New → Image Asset) sesuai logo toko Anda.
- Belum ada fitur hapus/edit item (sesuai spesifikasi awal belum diminta) — bisa ditambahkan menyusul di DAO/ViewModel yang sudah ada.
- Untuk menambah fitur baru ke depan, tambahkan kolom pada `Session`/`TransaksiItem`, naikkan `version` di `@Database`, lalu sediakan `Migration` Room (atau tetap pakai jalur CSV export-import sebagai migrasi manual seperti yang diminta).
