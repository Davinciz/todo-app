# AGENTS.MD - UniTask Todo App

High-signal guidance for AI agents working in this JavaFX + Maven + SQLite desktop application.

---

## CRITICAL: Development Commands

**Run the application:**
```bash
mvn clean javafx:run
```
⚠️ **DO NOT** run via `java MainApp` or IDE Run button - will fail with "JavaFX runtime components missing"
⚠️ **ALWAYS** use `mvn javafx:run` - it handles JavaFX module-path automatically

**Build standalone JAR:**
```bash
mvn clean package
java -jar target/todo-app-1.0.0.jar
```

**Compile only:**
```bash
mvn clean compile
```

**Clean build artifacts:**
```bash
mvn clean
```

---

## Tech Stack

- **Java 21** (required, not Java 17)
- **JavaFX 21.0.2** (controls + FXML)
- **SQLite 3.45.1.0** (embedded database)
- **Maven** (build tool)
- **No test framework configured** - tests in pom.xml but no actual tests exist

---

## Architecture

### Entry Point
- `src/main/java/com/todoapp/MainApp.java` - JavaFX application entry point
- Loads FXML from `src/main/resources/fxml/MainView.fxml`
- CSS entry: `src/main/resources/css/style.css` (imports partials)

### Package Structure
```
com.todoapp/
├── MainApp.java                    # Entry point
├── controller/                      # UI Controllers (8 files)
│   ├── MainController.java         # Orchestrator - wires all components
│   ├── TaskTableManager.java       # Table view management
│   ├── TaskFormDialog.java         # Create/edit dialog
│   ├── TaskDetailPanel.java        # Sidebar detail panel
│   ├── DashboardStatsPanel.java    # Dashboard stat cards
│   ├── DeadlineCalendarView.java   # Calendar widget
│   ├── NotificationToastManager.java # Toast notifications
│   └── ThemeManager.java           # Theme management (dark mode removed)
├── dao/
│   └── TaskDAO.java                # Database operations (CRUD)
├── model/
│   └── Task.java                   # Task entity with Priority/Status enums
└── util/                           # Utilities (5 files)
    ├── DatabaseConnection.java     # SQLite connection singleton
    ├── FileAttachmentManager.java  # File operations
    ├── ButtonAnimationUtil.java    # Spring animations
    ├── GlassEffectBuilder.java     # Liquid Glass effects helper
    └── SpringAnimationUtil.java    # Custom spring interpolator
```

### Database
- **Location:** `data/todo.db` (SQLite)
- **Schema:** Single `tasks` table created on first run by `DatabaseConnection.java`
- **Attachments:** Stored in `data/attachments/` directory
- ⚠️ **DO NOT** modify database schema - it's used by existing instances

---

## CSS Architecture (Liquid Glass UI)

**Entry point:** `src/main/resources/css/style.css`

**Import order (CRITICAL - do not reorder):**
1. `glass-tokens.css` - Design tokens (colors, blur, radius, shadows)
2. `glass-effects.css` - Reusable glass components (.glass-card, .glass-button, etc)
3. `partials/_tokens.css` - Legacy tokens (backward compatibility)
4. Other partials (base, animations, inputs, buttons, notifications, etc)

**Modular CSS files:**
- `partials/_base.css` - Root & typography
- `partials/_buttons.css` - Button styles
- `partials/_dashboard.css` - **Stat cards with colored backgrounds**
- `partials/_table.css` - Table styling
- `partials/_calendar.css` - Calendar cells
- `partials/_inputs.css` - Navbar & input fields
- `partials/_dialog.css` - Modal dialogs
- `partials/_notifications.css` - Toast notifications
- `partials/_filter-popup.css` - **Dark glass filter dialog**

**Design System:**
- Liquid Glass aesthetic: translucent surfaces, frosted glass, depth layering
- Spring-based animations (stiffness=200, damping=20)
- Color palette: Teal brand (#00A878), Green (#22C55E), Orange (#F59E0B), Red (#EF4444), Indigo (#6366F1)
- No dark mode (removed per user request)

---

## Key Features & Quirks

### Stat Cards
- Located in `DashboardStatsPanel.java`
- **Colored gradient backgrounds** added via CSS `.stat-card-total`, `.stat-card-done`, etc
- Each card has colored top border (3px) + tinted background
- Hover animations use spring interpolator

### Filter Dialog
- **Dark glass styling** with teal accents
- Background: `rgba(13, 17, 23, 0.92)`
- Border: `rgba(0, 168, 120, 0.40)`
- Title color: `#00E0AA`

### File Attachments
- Button "📎 Buka Lampiran" in detail panel (visible only when task has attachment)
- Uses `FileAttachmentManager.openAttachment()` to open with OS default app
- Attachments stored in `data/attachments/` with hashed filenames

### Toast Notifications
- Positioned bottom-right
- Entrance: slide from bottom (+80px) with fade
- Exit: slide to bottom with fade
- Auto-dismiss: 3-4 seconds
- Pause on hover

### Spring Animations
- Custom `SpringInterpolator` in `SpringAnimationUtil.java`
- Used for: hover scale, press scale, modal entrance/exit, toast animations
- Default spring: stiffness=200, damping=20, mass=1.0

---

## Common Mistakes to Avoid

❌ **Running without Maven:**
- Don't use `java MainApp` or IDE Run button
- Always use `mvn javafx:run`

❌ **Modifying database schema:**
- Schema is auto-created on first run
- Changes will break existing user data

❌ **Reordering CSS imports:**
- `glass-tokens.css` and `glass-effects.css` MUST be imported before partials
- Legacy `_tokens.css` needed for backward compatibility

❌ **Removing spring animations:**
- All animations use `SpringAnimationUtil` for consistent feel
- Don't replace with linear/ease interpolators

❌ **Adding dark mode:**
- Dark mode was intentionally removed per user request
- Don't add `.dark-theme` styles

---

## Testing

⚠️ **No test framework configured**
- JUnit 5 is in pom.xml but no actual tests exist
- Manual testing via `mvn javafx:run` is the workflow

---

## Fonts

- **Inter** and **Plus Jakarta Sans** in `src/main/resources/fonts/`
- Loaded via CSS `@font-face` in glass-tokens.css
- Fallback: Segoe UI (Windows), Helvetica Neue (macOS)

---

## Performance Notes

- Glass effects use `setCache(true)` + `CacheHint.SPEED` for performance
- GaussianBlur max radius: 16 (higher causes lag)
- DropShadow effects cached on nodes
- Snapshot blur (backdrop effect) throttled to 100ms updates

---

## Future Agent Notes

When making changes:
1. **CSS changes:** Always test with `mvn clean javafx:run` to see visual results
2. **Java changes:** Run `mvn clean compile` to verify no errors
3. **UI components:** All styling via CSS - avoid inline styles in Java/FXML
4. **Animations:** Use `SpringAnimationUtil` helper methods, don't create raw Transitions
5. **Colors:** Use glass-tokens.css variables for consistency

---

## Troubleshooting

**App won't start:**
- Check Java version: `java -version` (must be 21+)
- Check Maven: `mvn -version`
- Clean build: `mvn clean compile`

**CSS not loading:**
- Verify import order in `style.css`
- Check file paths are relative to resources root
- Run `mvn clean` to clear cached resources

**Attachment button not showing:**
- Button visibility controlled by `TaskDetailPanel.update()`
- Only shows when `task.hasAttachment() == true`

**Filter dialog not dark:**
- Check `_filter-popup.css` is imported in `style.css`
- Verify CSS specificity (filter-popup-content class)

---

*Last updated: 2026-06-24 - Liquid Glass UI Redesign Complete*
