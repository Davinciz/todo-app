package com.todoapp.dao;

import com.todoapp.model.Task;
import com.todoapp.util.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object untuk entitas Task.
 * Bertanggung jawab untuk semua operasi CRUD ke tabel `tasks`.
 */
public class TaskDAO {

    public List<Task> getAllTasks() {
        List<Task> tasks = new ArrayList<>();
        String sql = "SELECT * FROM tasks ORDER BY " +
                "CASE WHEN deadline IS NULL THEN 1 ELSE 0 END, deadline ASC";

        try (Statement stmt = DatabaseConnection.getConnection().createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                tasks.add(mapRowToTask(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Gagal mengambil daftar task: " + e.getMessage(), e);
        }
        return tasks;
    }

    public Task getTaskById(int id) {
        String sql = "SELECT * FROM tasks WHERE id = ?";
        try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRowToTask(rs);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Gagal mengambil task: " + e.getMessage(), e);
        }
        return null;
    }

    public int addTask(Task task) {
        String sql = """
            INSERT INTO tasks (title, description, deadline, priority, status,
                                category, attachment_path, attachment_name)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
        """;

        try (PreparedStatement ps = DatabaseConnection.getConnection()
                .prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            bindTaskParams(ps, task);
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    int newId = keys.getInt(1);
                    task.setId(newId);
                    return newId;
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Gagal menambah task: " + e.getMessage(), e);
        }
        return -1;
    }

    public boolean updateTask(Task task) {
        String sql = """
            UPDATE tasks SET title = ?, description = ?, deadline = ?, priority = ?,
                              status = ?, category = ?, attachment_path = ?, attachment_name = ?
            WHERE id = ?
        """;

        try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement(sql)) {
            bindTaskParams(ps, task);
            ps.setInt(9, task.getId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Gagal mengupdate task: " + e.getMessage(), e);
        }
    }

    public boolean deleteTask(int id) {
        String sql = "DELETE FROM tasks WHERE id = ?";
        try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Gagal menghapus task: " + e.getMessage(), e);
        }
    }

    public List<Task> searchTasks(String keyword) {
        List<Task> tasks = new ArrayList<>();
        String sql = "SELECT * FROM tasks WHERE title LIKE ? OR description LIKE ? " +
                "ORDER BY CASE WHEN deadline IS NULL THEN 1 ELSE 0 END, deadline ASC";

        try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement(sql)) {
            String likeKeyword = "%" + keyword + "%";
            ps.setString(1, likeKeyword);
            ps.setString(2, likeKeyword);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    tasks.add(mapRowToTask(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Gagal mencari task: " + e.getMessage(), e);
        }
        return tasks;
    }

    public List<Task> getTasksByStatus(Task.Status status) {
        List<Task> tasks = new ArrayList<>();
        String sql = "SELECT * FROM tasks WHERE status = ? " +
                "ORDER BY CASE WHEN deadline IS NULL THEN 1 ELSE 0 END, deadline ASC";

        try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement(sql)) {
            ps.setString(1, status.name());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    tasks.add(mapRowToTask(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Gagal mengambil task by status: " + e.getMessage(), e);
        }
        return tasks;
    }

    // --- Helper methods ---

    private void bindTaskParams(PreparedStatement ps, Task task) throws SQLException {
        ps.setString(1, task.getTitle());
        ps.setString(2, task.getDescription());
        ps.setString(3, task.getDeadline() != null ? task.getDeadline().toString() : null);
        ps.setString(4, task.getPriority().name());
        ps.setString(5, task.getStatus().name());
        ps.setString(6, task.getCategory());
        ps.setString(7, task.getAttachmentPath());
        ps.setString(8, task.getAttachmentName());
    }

    private Task mapRowToTask(ResultSet rs) throws SQLException {
        Task task = new Task();
        task.setId(rs.getInt("id"));
        task.setTitle(rs.getString("title"));
        task.setDescription(rs.getString("description"));

        String deadlineStr = rs.getString("deadline");
        if (deadlineStr != null && !deadlineStr.isBlank()) {
            task.setDeadline(LocalDate.parse(deadlineStr));
        }

        task.setPriority(Task.Priority.valueOf(rs.getString("priority")));
        task.setStatus(Task.Status.valueOf(rs.getString("status")));
        task.setCategory(rs.getString("category"));
        task.setAttachmentPath(rs.getString("attachment_path"));
        task.setAttachmentName(rs.getString("attachment_name"));

        return task;
    }
}
