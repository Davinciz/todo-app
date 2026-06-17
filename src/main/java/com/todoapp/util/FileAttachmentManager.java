package com.todoapp.util;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

/**
 * Mengelola file lampiran: menyimpan copy file ke folder data/attachments,
 * dan membuka file lampiran menggunakan aplikasi default OS.
 */
public class FileAttachmentManager {

    private static final String ATTACHMENT_DIR = "data/attachments";

    /**
     * Menyalin file yang dipilih user ke folder attachments internal aplikasi.
     * Nama file akan diberi prefix UUID supaya tidak collision antar task.
     *
     * @param sourceFile file asli yang dipilih user lewat FileChooser
     * @return path relatif file yang baru disimpan (untuk disimpan ke database)
     */
    public static String saveAttachment(File sourceFile) throws IOException {
        Path attachmentDirPath = Paths.get(ATTACHMENT_DIR);
        if (!Files.exists(attachmentDirPath)) {
            Files.createDirectories(attachmentDirPath);
        }

        String uniquePrefix = UUID.randomUUID().toString().substring(0, 8);
        String newFileName = uniquePrefix + "_" + sourceFile.getName();
        Path targetPath = attachmentDirPath.resolve(newFileName);

        Files.copy(sourceFile.toPath(), targetPath, StandardCopyOption.REPLACE_EXISTING);

        return targetPath.toString();
    }

    /**
     * Membuka file lampiran menggunakan aplikasi default sesuai tipe file
     * (misalnya PDF dibuka PDF reader, gambar dibuka image viewer, dst).
     */
    public static void openAttachment(String filePath) throws IOException {
        if (filePath == null || filePath.isBlank()) {
            throw new IOException("Path file lampiran tidak valid.");
        }

        File file = new File(filePath);
        if (!file.exists()) {
            throw new IOException("File lampiran tidak ditemukan: " + filePath);
        }

        if (Desktop.isDesktopSupported()) {
            Desktop.getDesktop().open(file);
        } else {
            throw new IOException("Sistem tidak mendukung pembukaan file otomatis.");
        }
    }

    /**
     * Menghapus file lampiran dari folder internal (dipanggil saat task dihapus
     * atau lampiran diganti dengan yang baru).
     */
    public static void deleteAttachment(String filePath) {
        if (filePath == null || filePath.isBlank()) {
            return;
        }
        try {
            Files.deleteIfExists(Paths.get(filePath));
        } catch (IOException e) {
            System.err.println("Gagal menghapus file lampiran: " + e.getMessage());
        }
    }
}
