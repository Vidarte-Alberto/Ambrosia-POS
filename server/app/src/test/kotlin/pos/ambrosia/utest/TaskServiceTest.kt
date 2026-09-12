package pos.ambrosia.utest

import org.junit.After
import org.junit.Before
import pos.ambrosia.models.FreelanceTaskUpsert
import pos.ambrosia.services.TaskService
import pos.ambrosia.utils.ExposedTestDb
import java.io.File
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TaskServiceTest {
    private lateinit var databaseFile: File
    private val service = TaskService()

    @Before
    fun setUp() {
        databaseFile = ExposedTestDb.connect()
    }

    @After
    fun tearDown() {
        ExposedTestDb.cleanup(databaseFile)
    }

    @Test
    fun `addTask returns id for valid request`() {
        val taskId = service.addTask(FreelanceTaskUpsert(name = "Development", isBillable = true))

        assertNotNull(taskId)
        val task = service.getTaskById(taskId)
        assertNotNull(task)
        assertEquals("Development", task.name)
        assertTrue(task.isBillable)
    }

    @Test
    fun `addTask rejects blank name`() {
        assertNull(service.addTask(FreelanceTaskUpsert(name = "   ")))
    }

    @Test
    fun `getTasks excludes deleted tasks`() {
        ExposedTestDb.seedTask(name = "Active")
        ExposedTestDb.seedTask(name = "Deleted", isDeleted = true)

        val tasks = service.getTasks()

        assertEquals(1, tasks.size)
        assertEquals("Active", tasks[0].name)
    }

    @Test
    fun `getTaskById returns null for invalid missing or deleted task`() {
        val deletedTaskId = ExposedTestDb.seedTask(isDeleted = true)

        assertNull(service.getTaskById("not-a-uuid"))
        assertNull(service.getTaskById(UUID.randomUUID().toString()))
        assertNull(service.getTaskById(deletedTaskId))
    }

    @Test
    fun `updateTask updates active task`() {
        val taskId = ExposedTestDb.seedTask()

        val taskWasUpdated = service.updateTask(taskId, FreelanceTaskUpsert(name = "Design", isBillable = false))

        assertTrue(taskWasUpdated)
        val task = service.getTaskById(taskId)
        assertNotNull(task)
        assertEquals("Design", task.name)
        assertFalse(task.isBillable)
    }

    @Test
    fun `updateTask returns false for invalid missing or deleted task`() {
        val deletedTaskId = ExposedTestDb.seedTask(isDeleted = true)
        val validRequest = FreelanceTaskUpsert(name = "Updated")

        assertFalse(service.updateTask("not-a-uuid", validRequest))
        assertFalse(service.updateTask(UUID.randomUUID().toString(), validRequest))
        assertFalse(service.updateTask(deletedTaskId, validRequest))
        assertFalse(service.updateTask(deletedTaskId, validRequest.copy(name = " ")))
    }

    @Test
    fun `deleteTask soft deletes task`() {
        val taskId = ExposedTestDb.seedTask()

        val taskWasDeleted = service.deleteTask(taskId)

        assertTrue(taskWasDeleted)
        assertNull(service.getTaskById(taskId))
    }

    @Test
    fun `deleteTask returns false for invalid missing or already deleted task`() {
        val deletedTaskId = ExposedTestDb.seedTask(isDeleted = true)

        assertFalse(service.deleteTask("not-a-uuid"))
        assertFalse(service.deleteTask(UUID.randomUUID().toString()))
        assertFalse(service.deleteTask(deletedTaskId))
    }
}
