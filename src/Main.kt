import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException


data class Task(
    val id: Int,
    var title: String,
    var description: String,
    var priority: String,
    var dueDate: String,
    var isCompleted: Boolean,
    var category: String,
    val createdAt: String
)

private val tasks = mutableListOf<Task>()
private var nextId = 1

private val priorities = listOf("Низкий 🔵", "Средний 🟡", "Высокий 🟠", "Срочный 🔴")
private val defaultCategories = listOf("Работа", "Личное", "Учеба", "Здоровье", "Финансы")
private val categories = defaultCategories.toMutableList()

private val dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")

private fun getCurrentDate(): String = LocalDate.now().format(dateFormatter)

private fun isValidDate(dateStr: String): Boolean {
    return try {
        LocalDate.parse(dateStr, dateFormatter)
        true
    } catch (e: DateTimeParseException) {
        false
    }
}

private fun isDateInPast(dateStr: String): Boolean {
    return try {
        LocalDate.parse(dateStr, dateFormatter).isBefore(LocalDate.now())
    } catch (e: DateTimeParseException) {
        false
    }
}

private fun validateTitle(title: String): String? {
    return if (title.isBlank()) "Название задачи не может быть пустым" else null
}

private fun validatePriority(priority: String): String? {
    return if (priority !in priorities) "Приоритет должен быть из списка: ${priorities.joinToString()}" else null
}

private fun validateCategory(category: String): String? {
    return if (category.isBlank()) "Категория не может быть пустой" else null
}

private fun validateDueDate(dueDate: String): String? {
    return when {
        !isValidDate(dueDate) -> "Дата должна быть в формате dd.MM.yyyy"
        isDateInPast(dueDate) -> "Дата выполнения не может быть в прошлом"
        else -> null
    }
}

private fun createTask(
    title: String,
    description: String,
    priority: String,
    dueDate: String,
    category: String
): Result<String> {
    val titleError = validateTitle(title)
    val priorityError = validatePriority(priority)
    val categoryError = validateCategory(category)
    val dateError = validateDueDate(dueDate)

    val errors = listOfNotNull(titleError, priorityError, categoryError, dateError)

    return if (errors.isEmpty()) {
        val task = Task(
            id = nextId++,
            title = title.trim(),
            description = description.trim(),
            priority = priority,
            dueDate = dueDate,
            isCompleted = false,
            category = category.trim(),
            createdAt = getCurrentDate()
        )
        tasks.add(task)
        Result.success("Задача успешно создана!")
    } else {
        Result.failure(Exception(errors.joinToString("\n")))
    }
}

private fun getTaskById(id: Int): Task? = tasks.find { it.id == id }

private fun updateTask(
    id: Int,
    title: String? = null,
    description: String? = null,
    priority: String? = null,
    dueDate: String? = null,
    category: String? = null
): Result<String> {
    val task = getTaskById(id)
    return if (task == null) {
        Result.failure(Exception("Задача с ID $id не найдена"))
    } else if (task.isCompleted) {
        Result.failure(Exception("Невозможно редактировать выполненную задачу"))
    } else {
        title?.let {
            validateTitle(it)?.let { error -> return Result.failure(Exception(error)) }
            task.title = it.trim()
        }
        description?.let { task.description = it.trim() }
        priority?.let {
            validatePriority(it)?.let { error -> return Result.failure(Exception(error)) }
            task.priority = it
        }
        dueDate?.let {
            validateDueDate(it)?.let { error -> return Result.failure(Exception(error)) }
            task.dueDate = it
        }
        category?.let {
            validateCategory(it)?.let { error -> return Result.failure(Exception(error)) }
            task.category = it.trim()
        }
        Result.success("Задача успешно обновлена!")
    }
}

private fun deleteTask(id: Int): Result<String> {
    val task = getTaskById(id)
    return if (task == null) {
        Result.failure(Exception("Задача с ID $id не найдена"))
    } else {
        tasks.remove(task)
        Result.success("Задача успешно удалена!")
    }
}

private fun markTaskCompleted(id: Int): Result<String> {
    val task = getTaskById(id)
    return if (task == null) {
        Result.failure(Exception("Задача с ID $id не найдена"))
    } else if (task.isCompleted) {
        Result.failure(Exception("Задача уже выполнена"))
    } else {
        task.isCompleted = true
        Result.success("Задача отмечена как выполненная!")
    }
}

private fun getAllTasks(): List<Task> = tasks.sortedBy { it.id }

private fun getTasksByStatus(completed: Boolean): List<Task> =
    tasks.filter { it.isCompleted == completed }.sortedBy { it.id }

private fun searchTasks(query: String): List<Task> {
    val lowerQuery = query.lowercase()
    return tasks.filter {
        it.title.lowercase().contains(lowerQuery) ||
                it.description.lowercase().contains(lowerQuery)
    }.sortedBy { it.id }
}

private fun filterTasksByCategory(category: String): List<Task> =
    tasks.filter { it.category.equals(category, ignoreCase = true) }.sortedBy { it.id }

private fun filterTasksByPriority(priority: String): List<Task> =
    tasks.filter { it.priority == priority }.sortedBy { it.id }

private fun getOverdueTasks(): List<Task> =
    tasks.filter { !it.isCompleted && isDateInPast(it.dueDate) }.sortedBy { it.id }

private fun getTaskStatistics(): Map<String, Int> {
    val total = tasks.size
    val completed = tasks.count { it.isCompleted }
    val active = total - completed
    val percentage = if (total > 0) (completed * 100) / total else 0

    return mapOf(
        "total" to total,
        "completed" to completed,
        "active" to active,
        "percentage" to percentage
    )
}

private fun getPriorityDistribution(): Map<String, Int> =
    priorities.associateWith { priority -> tasks.count { it.priority == priority } }

private fun getCategoryDistribution(): Map<String, Int> =
    categories.associateWith { category -> tasks.count { it.category == category } }

private fun getOverdueCount(): Int = getOverdueTasks().size

private fun formatTask(task: Task): String {
    val status = if (task.isCompleted) "✅" else "⏳"
    val overdue = if (!task.isCompleted && isDateInPast(task.dueDate)) " 🔥 ПРОСРОЧЕНА" else ""

    return """
        |┌─── ЗАДАЧА #${task.id} ─────────────────────
        |│ Статус: $status$overdue
        |│ Название: ${task.title}
        |│ Описание: ${task.description.ifBlank { "нет описания" }}
        |│ Приоритет: ${task.priority}
        |│ Категория: ${task.category}
        |│ Дата выполнения: ${task.dueDate}
        |│ Дата создания: ${task.createdAt}
        |└────────────────────────────────────
    """.trimMargin()
}

private fun formatTaskList(taskList: List<Task>): String {
    return if (taskList.isEmpty()) {
        "📭 Задачи не найдены"
    } else {
        taskList.joinToString("\n\n") { formatTask(it) }
    }
}

private fun formatStatistics(): String {
    val stats = getTaskStatistics()
    val priorityDist = getPriorityDistribution()
    val categoryDist = getCategoryDistribution()

    return """
        |┌─── СТАТИСТИКА ─────────────────────
        |│ Всего задач: ${stats["total"]}
        |│ Выполнено: ${stats["completed"]} (${stats["percentage"]}%)
        |│ Активных: ${stats["active"]}
        |│ Просроченных: ${getOverdueCount()}
        |│
        |│ Распределение по приоритетам:
        |${priorityDist.entries.joinToString("\n") { "│   ${it.key}: ${it.value}" }}
        |│
        |│ Распределение по категориям:
        |${categoryDist.entries.joinToString("\n") { "│   ${it.key}: ${it.value}" }}
        |└────────────────────────────────────
    """.trimMargin()
}

private fun readNonEmptyString(prompt: String): String {
    while (true) {
        print(prompt)
        val input = readLine()?.trim()
        if (!input.isNullOrEmpty()) return input
        println("❌ Это поле не может быть пустым. Попробуйте снова.")
    }
}

private fun readOptionalString(prompt: String): String {
    print(prompt)
    return readLine()?.trim() ?: ""
}

private fun selectFromList(options: List<String>, prompt: String): String {
    println(prompt)
    options.forEachIndexed { index, option -> println("${index + 1}. $option") }

    while (true) {
        print("Выберите вариант (1-${options.size}): ")
        val input = readLine()?.toIntOrNull()
        if (input != null && input in 1..options.size) {
            return options[input - 1]
        }
        println("❌ Неверный выбор. Попробуйте снова.")
    }
}

private fun readDate(prompt: String): String {
    while (true) {
        print(prompt)
        val input = readLine()?.trim()
        if (input != null) {
            val error = validateDueDate(input)
            if (error == null) return input
            println("❌ $error")
        }
        println("ℹ️  Используйте формат: дд.мм.гггг (например, 25.12.2024)")
    }
}

private fun readInt(prompt: String): Int {
    while (true) {
        print(prompt)
        val input = readLine()?.toIntOrNull()
        if (input != null) return input
        println("❌ Введите корректное число.")
    }
}

private fun confirmAction(prompt: String): Boolean {
    while (true) {
        print("$prompt (да/нет): ")
        when (readLine()?.trim()?.lowercase()) {
            "да", "д", "yes", "y" -> return true
            "нет", "н", "no", "n" -> return false
            else -> println("❌ Пожалуйста, введите 'да' или 'нет'")
        }
    }
}

private fun showMainMenu() {
    println(
        """
        |┌─── TASKMASTER ────────────────────
        |│ 1. 📋 Просмотреть все задачи
        |│ 2. ➕ Добавить задачу
        |│ 3. ✏️  Редактировать задачу
        |│ 4. ✅ Отметить задачу выполненной
        |│ 5. 🗑️  Удалить задачу
        |│ 6. 🔍 Поиск и фильтрация
        |│ 7. 📊 Статистика
        |│ 8. 🚪 Выход
        |└────────────────────────────────────
    """.trimMargin()
    )
}

private fun showSearchMenu() {
    println(
        """
        |┌─── ПОИСК И ФИЛЬТРАЦИЯ ────────────
        |│ 1. 🔍 Поиск по тексту
        |│ 2. 📁 Активные задачи
        |│ 3. ✅ Выполненные задачи
        |│ 4. 🗂️  Фильтр по категории
        |│ 5. 🎯 Фильтр по приоритету
        |│ 6. 🔥 Просроченные задачи
        |│ 7. ↩️  Назад
        |└────────────────────────────────────
    """.trimMargin()
    )
}

private fun handleAddTask() {
    println("\n┌─── ДОБАВЛЕНИЕ ЗАДАЧИ ─────────────")

    val title = readNonEmptyString("│ Введите название задачи: ")
    val description = readOptionalString("│ Введите описание (опционально): ")
    val priority = selectFromList(priorities, "│ Выберите приоритет:")
    val category = selectFromList(categories, "│ Выберите категорию:")
    val dueDate = readDate("│ Введите дату выполнения (дд.мм.гггг): ")

    val result = createTask(title, description, priority, dueDate, category)

    println("│")
    if (result.isSuccess) {
        println("│ ✅ ${result.getOrNull()}")
    } else {
        println("│ ❌ Ошибка: ${result.exceptionOrNull()?.message}")
    }
    println("└────────────────────────────────────")
}

private fun handleViewAllTasks() {
    println("\n┌─── ВСЕ ЗАДАЧИ ────────────────────")
    println(formatTaskList(getAllTasks()))
    println("└────────────────────────────────────")
}

private fun handleEditTask() {
    println("\n┌─── РЕДАКТИРОВАНИЕ ЗАДАЧИ ─────────")
    val taskId = readInt("│ Введите ID задачи для редактирования: ")

    val task = getTaskById(taskId)
    if (task == null) {
        println("│ ❌ Задача с ID $taskId не найдена")
        println("└────────────────────────────────────")
        return
    }

    if (task.isCompleted) {
        println("│ ❌ Невозможно редактировать выполненную задачу")
        println("└────────────────────────────────────")
        return
    }

    println("│ Текущие данные:")
    println(formatTask(task))

    val newTitle = readOptionalString("│ Новое название (оставьте пустым для сохранения текущего): ")
    val newDescription = readOptionalString("│ Новое описание (оставьте пустым для сохранения текущего): ")
    val newPriority = if (confirmAction("│ Хотите изменить приоритет?")) {
        selectFromList(priorities, "│ Выберите новый приоритет:")
    } else null
    val newCategory = if (confirmAction("│ Хотите изменить категорию?")) {
        selectFromList(categories, "│ Выберите новую категорию:")
    } else null
    val newDueDate = if (confirmAction("│ Хотите изменить дату выполнения?")) {
        readDate("│ Введите новую дату выполнения (дд.мм.гггг): ")
    } else null

    val result = updateTask(
        id = taskId,
        title = if (newTitle.isNotEmpty()) newTitle else null,
        description = if (newDescription.isNotEmpty()) newDescription else null,
        priority = newPriority,
        category = newCategory,
        dueDate = newDueDate
    )

    println("│")
    if (result.isSuccess) {
        println("│ ✅ ${result.getOrNull()}")
    } else {
        println("│ ❌ Ошибка: ${result.exceptionOrNull()?.message}")
    }
    println("└────────────────────────────────────")
}

private fun handleMarkCompleted() {
    println("\n┌─── ОТМЕТКА ВЫПОЛНЕНИЯ ───────────")
    val taskId = readInt("│ Введите ID задачи для отметки: ")

    val result = markTaskCompleted(taskId)

    println("│")
    if (result.isSuccess) {
        println("│ ✅ ${result.getOrNull()}")
    } else {
        println("│ ❌ Ошибка: ${result.exceptionOrNull()?.message}")
    }
    println("└────────────────────────────────────")
}

private fun handleDeleteTask() {
    println("\n┌─── УДАЛЕНИЕ ЗАДАЧИ ───────────────")
    val taskId = readInt("│ Введите ID задачи для удаления: ")

    val task = getTaskById(taskId)
    if (task != null) {
        println("│ Найдена задача:")
        println(formatTask(task))

        if (confirmAction("│ Вы уверены, что хотите удалить эту задачу?")) {
            val result = deleteTask(taskId)
            println("│")
            if (result.isSuccess) {
                println("│ ✅ ${result.getOrNull()}")
            } else {
                println("│ ❌ Ошибка: ${result.exceptionOrNull()?.message}")
            }
        } else {
            println("│ ❌ Удаление отменено")
        }
    } else {
        println("│ ❌ Задача с ID $taskId не найдена")
    }
    println("└────────────────────────────────────")
}

private fun handleSearchAndFilter() {
    while (true) {
        println("\n┌─── ПОИСК И ФИЛЬТРАЦИЯ ────────────")
        showSearchMenu()
        print("Выберите действие: ")

        when (readLine()?.toIntOrNull()) {
            1 -> {
                val query = readNonEmptyString("Введите текст для поиска: ")
                println("\n${formatTaskList(searchTasks(query))}")
            }
            2 -> println("\n${formatTaskList(getTasksByStatus(false))}")
            3 -> println("\n${formatTaskList(getTasksByStatus(true))}")
            4 -> {
                val category = selectFromList(categories, "Выберите категорию:")
                println("\n${formatTaskList(filterTasksByCategory(category))}")
            }
            5 -> {
                val priority = selectFromList(priorities, "Выберите приоритет:")
                println("\n${formatTaskList(filterTasksByPriority(priority))}")
            }
            6 -> println("\n${formatTaskList(getOverdueTasks())}")
            7 -> return
            else -> println("❌ Неверный выбор. Попробуйте снова.")
        }
    }
}

private fun handleStatistics() {
    println("\n${formatStatistics()}")
}

fun main() {
    println("🚀 Добро пожаловать в TaskMaster!")
    println("📝 Система управления задачами\n")

    while (true) {
        showMainMenu()
        print("Выберите действие: ")

        when (readLine()?.toIntOrNull()) {
            1 -> handleViewAllTasks()
            2 -> handleAddTask()
            3 -> handleEditTask()
            4 -> handleMarkCompleted()
            5 -> handleDeleteTask()
            6 -> handleSearchAndFilter()
            7 -> handleStatistics()
            8 -> {
                println("\n👋 До свидания! Спасибо за использование TaskMaster!")
                return
            }
            else -> println("❌ Неверный выбор. Попробуйте снова.\n")
        }

        println()
    }
}