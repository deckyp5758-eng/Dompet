package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.local.DatabaseModule
import com.example.data.local.DatabaseProvider
import com.example.data.local.DefaultDatabaseModule
import com.example.data.local.MonthlyBudgetDao
import com.example.data.local.MonthlyBudgetEntity
import com.example.data.local.SavingsGoalDao
import com.example.data.local.SavingsGoalEntity
import com.example.data.local.TransactionDao
import com.example.data.local.TransactionEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class RoomDatabaseProviderTest {

    private lateinit var db: AppDatabase
    private lateinit var transactionDao: TransactionDao
    private lateinit var monthlyBudgetDao: MonthlyBudgetDao
    private lateinit var savingsGoalDao: SavingsGoalDao
    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        transactionDao = db.transactionDao()
        monthlyBudgetDao = db.monthlyBudgetDao()
        savingsGoalDao = db.savingsGoalDao()

        DatabaseProvider.setDatabaseForTesting(db)
    }

    @After
    fun tearDown() {
        DatabaseProvider.setDatabaseForTesting(null)
        db.close()
    }

    @Test
    fun testTransactionDaoOperations() = runBlocking {
        val now = System.currentTimeMillis()
        val tx1 = TransactionEntity(
            title = "Gaji Pokok",
            amount = 2_700_000L,
            type = "INCOME",
            category = "Gaji Tetap",
            paymentMethod = "TRANSFER_BANK",
            dateMillis = now - 1000
        )
        val tx2 = TransactionEntity(
            title = "Makan Siang",
            amount = 35_000L,
            type = "EXPENSE",
            category = "Makan & Kebutuhan Pokok",
            paymentMethod = "QRIS",
            dateMillis = now
        )

        val id1 = transactionDao.insertTransaction(tx1)
        val id2 = transactionDao.insertTransaction(tx2)
        assertTrue(id1 > 0)
        assertTrue(id2 > 0)

        // Read all transactions
        val allTx = transactionDao.getAllTransactions().first()
        assertEquals(2, allTx.size)
        assertEquals("Makan Siang", allTx[0].title) // sorted desc by dateMillis

        // Read by type
        val incomes = transactionDao.getTransactionsByType("INCOME").first()
        assertEquals(1, incomes.size)
        assertEquals(2_700_000L, incomes[0].amount)

        // Total amount by type
        val totalExpense = transactionDao.getTotalAmountByType("EXPENSE").first()
        assertEquals(35_000L, totalExpense)

        // Update transaction
        val updatedTx = allTx[0].copy(amount = 40_000L, notes = "Tambah es teh")
        transactionDao.updateTransaction(updatedTx)
        val fetchedTx = transactionDao.getTransactionById(updatedTx.id)
        assertNotNull(fetchedTx)
        assertEquals(40_000L, fetchedTx?.amount)
        assertEquals("Tambah es teh", fetchedTx?.notes)

        // Delete
        transactionDao.deleteTransactionById(id1)
        val remaining = transactionDao.getAllTransactions().first()
        assertEquals(1, remaining.size)
        assertEquals(id2, remaining[0].id)
    }

    @Test
    fun testMonthlyBudgetDaoOperations() = runBlocking {
        val budgetOctober = MonthlyBudgetEntity(
            month = 10,
            year = 2026,
            totalBudget = 2_700_000L,
            allocatedNeeds = 1_620_000L,
            allocatedWants = 405_000L,
            allocatedSavings = 675_000L,
            notes = "Anggaran Oktober 2026"
        )

        val budgetId = monthlyBudgetDao.insertMonthlyBudget(budgetOctober)
        assertTrue(budgetId > 0)

        val fetchedBudget = monthlyBudgetDao.getBudgetForMonth(10, 2026).first()
        assertNotNull(fetchedBudget)
        assertEquals(2_700_000L, fetchedBudget?.totalBudget)
        assertEquals(1_620_000L, fetchedBudget?.allocatedNeeds)

        // Update
        val modified = fetchedBudget!!.copy(totalBudget = 3_000_000L)
        monthlyBudgetDao.updateMonthlyBudget(modified)

        val reFetched = monthlyBudgetDao.getBudgetForMonthSync(10, 2026)
        assertEquals(3_000_000L, reFetched?.totalBudget)

        // Delete
        monthlyBudgetDao.deleteMonthlyBudget(reFetched!!)
        val emptyBudget = monthlyBudgetDao.getBudgetForMonthSync(10, 2026)
        assertNull(emptyBudget)
    }

    @Test
    fun testSavingsGoalDaoOperations() = runBlocking {
        val now = System.currentTimeMillis()
        val goal1 = SavingsGoalEntity(
            title = "Laptop Kerja",
            targetAmount = 8_000_000L,
            currentAmount = 1_500_000L,
            targetDateMillis = now + 100_000,
            priority = "TINGGI",
            isCompleted = false
        )
        val goal2 = SavingsGoalEntity(
            title = "Sepatu Baru",
            targetAmount = 500_000L,
            currentAmount = 500_000L,
            targetDateMillis = now - 100_000,
            priority = "RENDAH",
            isCompleted = true
        )

        val id1 = savingsGoalDao.insertSavingsGoal(goal1)
        val id2 = savingsGoalDao.insertSavingsGoal(goal2)
        assertTrue(id1 > 0)
        assertTrue(id2 > 0)

        // Active vs completed goals
        val activeGoals = savingsGoalDao.getActiveSavingsGoals().first()
        assertEquals(1, activeGoals.size)
        assertEquals("Laptop Kerja", activeGoals[0].title)

        val completedGoals = savingsGoalDao.getCompletedSavingsGoals().first()
        assertEquals(1, completedGoals.size)
        assertEquals("Sepatu Baru", completedGoals[0].title)

        // Add deposit progress
        savingsGoalDao.addSavingsGoalDeposit(id1, 500_000L)
        val updatedGoal = savingsGoalDao.getSavingsGoalByIdSync(id1)
        assertEquals(2_000_000L, updatedGoal?.currentAmount)

        // Toggle completion status
        savingsGoalDao.updateSavingsGoalStatus(id1, true)
        val finishedGoal = savingsGoalDao.getSavingsGoalByIdSync(id1)
        assertTrue(finishedGoal?.isCompleted == true)
    }

    @Test
    fun testDatabaseProviderAndModule() {
        val providerDb = DatabaseProvider.provideDatabase(context)
        assertNotNull(providerDb)

        val txDao = DatabaseProvider.provideTransactionDao(context)
        val bDao = DatabaseProvider.provideMonthlyBudgetDao(context)
        val sDao = DatabaseProvider.provideSavingsGoalDao(context)
        val fDao = DatabaseProvider.provideFinanceDao(context)

        assertNotNull(txDao)
        assertNotNull(bDao)
        assertNotNull(sDao)
        assertNotNull(fDao)

        val module: DatabaseModule = DefaultDatabaseModule()
        assertEquals(providerDb, module.getDatabase(context))
        assertNotNull(module.getTransactionDao(context))
        assertNotNull(module.getMonthlyBudgetDao(context))
        assertNotNull(module.getSavingsGoalDao(context))
        assertNotNull(module.getFinanceDao(context))
    }
}
