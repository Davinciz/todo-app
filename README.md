# To-Do List - Task Tracker (Java Desktop App)

Aplikasi desktop untuk melacak tugas (to-do list), dibangun dengan **JavaFX**, **Maven**, dan **SQLite**. Mendukung fitur lampiran file per tugas.

## Fitur

- Tambah, edit, hapus tugas
- Field: judul, deskripsi, deadline, kategori, prioritas, status
- Lampirkan file apapun ke tugas (PDF, gambar, dokumen, dll) dan buka langsung dari aplikasi
- Pencarian dan filter berdasarkan status
- Highlight otomatis untuk tugas yang sudah lewat deadline (overdue) dan tugas yang selesai
- Data tersimpan permanen di file SQLite lokal (`data/todo.db`)

## Prasyarat

1. **JDK 17 atau lebih baru** — cek dengan `java -version`
2. **Maven** — cek dengan `mvn -version`
3. **VS Code** dengan ekstensi berikut (install dari Extensions Marketplace):
   - `Extension Pack for Java` (Microsoft)
   - `Maven for Java` (biasanya sudah include di atas)

> Catatan: JavaFX SDK **tidak perlu** didownload manual karena dependency-nya sudah diatur lewat Maven (`pom.xml`) dan plugin `javafx-maven-plugin` akan menanganinya otomatis.

## Cara Menjalankan

### Opsi 1 — Lewat Terminal (paling direkomendasikan)

Buka terminal di root folder project (`todo-app/`), lalu jalankan:

```bash
mvn clean javafx:run
```

Maven otomatis akan men-download dependency JavaFX dan SQLite saat pertama kali dijalankan (butuh koneksi internet sekali di awal).

### Opsi 2 — Lewat VS Code

1. Buka folder `todo-app` di VS Code (`File > Open Folder`)
2. Tunggu ekstensi Java selesai memuat project (lihat indikator di status bar)
3. Buka terminal terintegrasi (`` Ctrl+` ``) dan jalankan `mvn clean javafx:run`

> Menjalankan langsung lewat tombol "Run" di `MainApp.java` bisa gagal karena JavaFX butuh module-path khusus. Jalur Maven (`javafx:run`) adalah cara paling stabil dan tidak butuh konfigurasi tambahan.

## Build jadi file JAR standalone

```bash
mvn clean package
```

File hasil build ada di `target/todo-app-1.0.0.jar` dan bisa dijalankan dengan:

```bash
java -jar target/todo-app-1.0.0.jar
```

## Troubleshooting

| Masalah                                       | Solusi                                                                     |
| --------------------------------------------- | -------------------------------------------------------------------------- |
| `mvn: command not found`                      | Install Maven dan pastikan sudah ditambahkan ke PATH sistem                |
| Error `JavaFX runtime components are missing` | Pastikan menjalankan lewat `mvn javafx:run`, bukan langsung `java MainApp` |
| Database error saat pertama run               | Pastikan folder `data/` memiliki izin tulis (write permission)             |
| Lampiran tidak bisa dibuka                    | Pastikan ada aplikasi default di OS untuk membuka tipe file tersebut       |

## Pengembangan Selanjutnya (Ide)

- Notifikasi reminder sebelum deadline
- Kategori sebagai dropdown dinamis (bukan free text)
- Drag & drop file langsung ke form lampiran
- Export daftar tugas ke PDF/Excel
- Sub-tugas (checklist) di dalam satu tugas
