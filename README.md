# Memo Tugas 3.0 — Android

Aplikasi Android native sederhana untuk mencatat tugas kuliah.

## Fitur
- Tambah, edit, hapus tugas
- Mata kuliah, judul, deskripsi, deadline
- Upload lampiran instruksi dan hasil melalui pemilih file Android (opsional)
- Status belum dikerjakan / proses / selesai
- Ringkasan dan filter tugas
- Pengingat deadline H-3, H-1, dan hari H
- Penyimpanan lokal Room Database

## Build APK dari HP melalui GitHub
1. Unduh dan ekstrak ZIP proyek ini.
2. Buat repository GitHub baru, misalnya `memo-tugas-android`.
3. Upload seluruh isi folder proyek ke root repository (termasuk folder `.github`).
4. Buka tab **Actions** pada repository.
5. Pilih workflow **Build Memo Tugas APK** lalu tekan **Run workflow**. Workflow juga berjalan otomatis saat push ke branch `main`.
6. Setelah selesai, buka hasil workflow dan unduh artifact `Memo-Tugas-3.0-APK`.
7. Ekstrak artifact ZIP, lalu instal `app-debug.apk` di Android. Izinkan instalasi dari sumber tersebut jika diminta.

## Catatan
- Build menghasilkan debug APK untuk pengujian/pemasangan pribadi.
- Database tersimpan lokal pada perangkat. Uninstall aplikasi dapat menghapus data lokal.
- File yang dipilih disimpan sebagai URI dengan izin akses persisten Android; jangan hapus/pindahkan file sumber jika ingin tetap dapat membukanya.
- Pengingat menggunakan alarm non-exact agar lebih ramah baterai; waktu notifikasi dapat sedikit bergeser karena kebijakan Android.
- Untuk rilis publik/Play Store, perlu signing release, ikon final, pengujian perangkat, serta kebijakan privasi.
