/*
 * SPDX-License-Identifier: GPL-3.0-only
 */

package com.tiefensuche.motionmate.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.ResultReceiver
import android.text.format.DateUtils
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.ItemTouchHelper
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.preference.PreferenceManager
import com.tiefensuche.motionmate.R
import com.tiefensuche.motionmate.service.MotionService
import com.tiefensuche.motionmate.ui.cards.MotionActivityTextItem
import com.tiefensuche.motionmate.ui.cards.GlobalStatsItem
import com.tiefensuche.motionmate.util.Database
import com.tiefensuche.motionmate.util.Util
import java.util.*
import kotlin.collections.ArrayList
import com.tiefensuche.motionmate.stats.StepStreakCalculator
import com.tiefensuche.motionmate.stats.RollingStepAverages
import com.tiefensuche.motionmate.stats.WeeklyStepSummary
import com.tiefensuche.motionmate.stats.DailyHistorySummary
import com.tiefensuche.motionmate.ui.cards.TextItem
import com.tiefensuche.motionmate.ui.cards.StreakDashboardItem
import com.tiefensuche.motionmate.ui.cards.RollingStepAveragesItem
import com.tiefensuche.motionmate.stats.DailyGoalProgress
import java.text.NumberFormat
import java.text.SimpleDateFormat

/**
 * The main activity for the UI of the step counter.
 */
internal class MainActivity : AppCompatActivity() {
    private lateinit var mTextViewSteps: TextView
    private lateinit var mTextViewMeters: TextView
    private lateinit var mTextViewGoalProgress: TextView
    private lateinit var mGoalProgress: GoalProgressView
    private lateinit var mHistoryCalendar: HistoryCalendarView
    private lateinit var mHistoryGoalBar: HistoryGoalBarView
    private lateinit var mCalendarContainer: View
    private lateinit var mCalendarToggle: TextView
    private var mHistoryExpanded = false
    private lateinit var mChart: Chart
    private lateinit var mTextViewChart: TextView
    private var mAdapter: TextItemAdapter = TextItemAdapter()
    // Dedicated one-card list positioned between daily steps and History.
    private val mStreakAdapter = TextItemAdapter()
    private lateinit var mGlobalStatsCard: GlobalStatsItem
    private lateinit var mStreakDashboardCard: StreakDashboardItem
    private lateinit var mRollingAveragesCard: RollingStepAveragesItem
    private var mCachedHistory: List<Pair<Long, Int>>? = null
    private var mCachedHistoryDay: Long = Long.MIN_VALUE
    private var mDailyHistory: Map<Long, Long>? = null
    private var mCalendarNeedsRefresh = true
    private var mLastToday = DailyHistorySummary.startOfDay(System.currentTimeMillis())
    private var mCurrentSteps: Int = 0
    private var mSelectedMonth = Util.calendar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Util.applyTheme(PreferenceManager.getDefaultSharedPreferences(this).getString("theme", "system")!!)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContentView(R.layout.activity_main)

        Util.stepWidth = PreferenceManager.getDefaultSharedPreferences(this).getInt("step_width", 70)

        mTextViewSteps = findViewById(R.id.textViewSteps)
        mTextViewMeters = findViewById(R.id.textViewMeters)
        mTextViewGoalProgress = findViewById(R.id.textViewGoalProgress)
        mGoalProgress = findViewById(R.id.goalProgress)
        val mRecyclerView = findViewById<RecyclerView>(R.id.recyclerView)
        mHistoryCalendar = findViewById(R.id.historyCalendar)
        mHistoryGoalBar = findViewById(R.id.historyGoalProgress)
        mChart = findViewById(R.id.chart)
        mTextViewChart = findViewById(R.id.textViewChart)
        mCalendarContainer = findViewById(R.id.calendarContainer)
        mCalendarToggle = findViewById(R.id.textViewCalendarToggle)
        // Restore the selected date as well as the collapsed calendar on rotation.
        mSelectedMonth.timeInMillis = savedInstanceState?.getLong(
            HISTORY_SELECTED_DAY_KEY, Util.calendar.timeInMillis
        ) ?: Util.calendar.timeInMillis
        mHistoryExpanded = savedInstanceState?.getBoolean(HISTORY_EXPANDED_KEY) ?: false
        mCalendarToggle.setOnClickListener {
            mHistoryExpanded = !mHistoryExpanded
            updateHistoryVisibility()
        }
        mHistoryCalendar.onDaySelected = { day -> selectHistoryDay(day) }
        findViewById<TextView>(R.id.historyPreviousDay).setOnClickListener { moveHistoryDay(-1) }
        findViewById<TextView>(R.id.historyNextDay).setOnClickListener { moveHistoryDay(1) }
        updateHistoryVisibility()

        val mLayoutManager = LinearLayoutManager(this)
        mRecyclerView.layoutManager = mLayoutManager
        mRecyclerView.adapter = mAdapter

        // A separate RecyclerView lets the streak appear before History,
        // without moving the activity/statistics list itself.
        findViewById<RecyclerView>(R.id.recyclerViewStreak).apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            isNestedScrollingEnabled = false
            adapter = mStreakAdapter
        }

        // setup swipeable cards and remove them on swiped, used for activities
        ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
            override fun onMove(recyclerView: RecyclerView,
                                viewHolder: RecyclerView.ViewHolder,
                                target: RecyclerView.ViewHolder): Boolean {
                return false
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val item = mAdapter[viewHolder.adapterPosition]
                if (item is MotionActivityTextItem) {
                    val i = Intent(this@MainActivity, MotionService::class.java)
                    i.action = MotionService.ACTION_STOP_ACTIVITY
                    i.putExtra(MotionService.KEY_ID, item.id)
                    startService(i)
                    mAdapter.remove(viewHolder.adapterPosition)
                }
            }

            override fun getMovementFlags(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder): Int {
                val swipeFlags = ItemTouchHelper.START or ItemTouchHelper.END
                return if (mAdapter[viewHolder.adapterPosition].isSwipeable) {
                    ItemTouchHelper.Callback.makeMovementFlags(0, swipeFlags)
                } else 0
            }

        }).attachToRecyclerView(mRecyclerView)

        // Floating action button to start new activity
        findViewById<View>(R.id.fab).let {
            it.setOnClickListener {
                val i = Intent(this@MainActivity, MotionService::class.java)
                i.action = MotionService.ACTION_START_ACTIVITY
                startService(i)
            }

            ViewCompat.setOnApplyWindowInsetsListener(it) { view, windowInsets ->
                val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
                view.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                    bottomMargin = insets.bottom + 50
                }
                WindowInsetsCompat.CONSUMED
            }
        }

        mTextViewSteps.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        // initial update of the diagram
        updateChart()

        // Add some cards with statistics
        setupCards()

        // Start the motion service
        checkPermission()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        AppCompatDelegate.setDefaultNightMode(
            if (AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_YES)
                AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            else AppCompatDelegate.MODE_NIGHT_YES)
        return super.onOptionsItemSelected(item)
    }

    private fun setupCards() {
        // One compact group replaces the three separate statistics cards.
        mGlobalStatsCard = GlobalStatsItem(this)
        mAdapter.add(mGlobalStatsCard)

        mStreakDashboardCard = StreakDashboardItem(this)
        mStreakDashboardCard.displayStyle = PreferenceManager.getDefaultSharedPreferences(this)
            .getString("streak_card_style", "current") ?: "current"
        mStreakAdapter.add(mStreakDashboardCard)
        mRollingAveragesCard = RollingStepAveragesItem(this)
        mAdapter.add(mRollingAveragesCard)

        updateStreakCards()

        updateCards()
    }

    private fun subscribeService() {
        // start the service and pass a result receiver that is used by the service to update the UI
        val i = Intent(this, MotionService::class.java)
        i.action = MotionService.ACTION_SUBSCRIBE
        i.putExtra(RECEIVER_TAG, object : ResultReceiver(null) {
            override fun onReceiveResult(resultCode: Int, resultData: Bundle) {
                if (resultCode == 0) {
                    runOnUiThread { updateView(resultData.getInt(MotionService.KEY_STEPS), resultData.getParcelableArrayList(MotionService.KEY_ACTIVITIES) ?: ArrayList()) }
                }
            }
        })
        startService(i)
    }

    private fun checkPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
            packageManager.hasSystemFeature(PackageManager.FEATURE_SENSOR_STEP_COUNTER) &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACTIVITY_RECOGNITION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACTIVITY_RECOGNITION), 0)
        } else {
            subscribeService()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        subscribeService()
    }

    private fun updateView(steps: Int, activities: MutableList<Bundle>) {
        // update current today's steps in the header
        mCurrentSteps = steps

        updateDailyGoalProgress(steps)
        updateStreakCards()

        mTextViewMeters.text = String.format(getString(R.string.meters_today), Util.stepsToMeters(steps))
        mTextViewSteps.text = resources.getQuantityString(R.plurals.steps_text, steps, steps)

        // Midnight rollover: follow today only if the user was viewing yesterday.
        val newToday = DailyHistorySummary.startOfDay(System.currentTimeMillis())
        if (newToday != mLastToday) {
            if (DailyHistorySummary.startOfDay(mSelectedMonth.timeInMillis) == mLastToday)
                mSelectedMonth.timeInMillis = newToday
            mLastToday = newToday
            mCachedHistory = null
            mDailyHistory = null
            mCalendarNeedsRefresh = true
            updateChart()
        }

        // update the cards
        mGlobalStatsCard.updateToday(steps)
        for (i in 0 until mAdapter.itemCount) {
            val item = mAdapter[i]
            if (item is MotionActivityTextItem) {
                for (activity in activities) {
                    if (activity.getInt(MotionService.KEY_ID) == item.id) {
                        item.updateSteps(activity.getInt(MotionService.KEY_STEPS))
                        activities.remove(activity)
                        break
                    }
                }
            }
        }

        // initialize dynamic cards, e.g. activities, that are not yet added
        for (activity in activities) {
            val id = activity.getInt(MotionService.KEY_ID)
            val item = MotionActivityTextItem(this, id, View.OnClickListener {
                val i = Intent(this@MainActivity, MotionService::class.java)
                i.action = MotionService.ACTION_TOGGLE_ACTIVITY
                i.putExtra(MotionService.KEY_ID, id)
                startService(i)
            })
            item.updateSteps(activity.getInt(MotionService.KEY_STEPS))
            item.setActive(activity.getBoolean(MotionService.KEY_ACTIVE))
            mAdapter.addTop(item)
        }

        // Refresh the selected week only when it includes today (even across a year boundary).
        val now = System.currentTimeMillis()
        val selectedWeek = WeeklyStepSummary.calculate(
            history = emptyList(), selectedDate = mSelectedMonth.timeInMillis,
            todaySteps = steps.coerceAtLeast(0), goal = 1, now = now
        )
        if (selectedWeek.todayIndex >= 0) updateChart()
        mHistoryCalendar.updateTodaySteps(steps.coerceAtLeast(0))
        val currentCalendar = Calendar.getInstance()
        if (mSelectedMonth.get(Calendar.YEAR) == currentCalendar.get(Calendar.YEAR) &&
            mSelectedMonth.get(Calendar.MONTH) == currentCalendar.get(Calendar.MONTH)) {
            // Global statistics were already refreshed above.
        }
    }

    /** Hide only the calendar. The selected-day summary remains visible. */
    private fun updateHistoryVisibility() {
        mCalendarContainer.visibility = if (mHistoryExpanded) View.VISIBLE else View.GONE
        mCalendarToggle.setText(if (mHistoryExpanded)
            R.string.history_hide_calendar else R.string.history_show_calendar)
        mCalendarToggle.contentDescription = mCalendarToggle.text
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean(HISTORY_EXPANDED_KEY, mHistoryExpanded)
        outState.putLong(HISTORY_SELECTED_DAY_KEY, mSelectedMonth.timeInMillis)
        super.onSaveInstanceState(outState)
    }

    private fun updateChart() {
        val selected = Calendar.getInstance().apply {
            timeInMillis = mSelectedMonth.timeInMillis
        }
        val goal = PreferenceManager.getDefaultSharedPreferences(this)
            .getInt("daily_step_goal", 10000).coerceAtLeast(1)
        val history = getCachedHistory()
        val week = WeeklyStepSummary.calculate(
            history = history,
            selectedDate = selected.timeInMillis,
            todaySteps = mCurrentSteps.coerceAtLeast(0),
            goal = goal
        )
        val weekStart = Calendar.getInstance().apply { timeInMillis = week.weekStart }
        val weekEnd = Calendar.getInstance().apply {
            timeInMillis = week.weekEndExclusive
            add(Calendar.DAY_OF_MONTH, -1)
        }
        mTextViewChart.text = String.format(
            Locale.getDefault(), getString(R.string.week_display_format),
            weekStart.get(Calendar.WEEK_OF_YEAR), weekStart.timeInMillis, weekEnd.timeInMillis
        )
        val formatter = NumberFormat.getIntegerInstance()
        findViewById<TextView>(R.id.textViewWeekGoal).text =
            getString(R.string.week_goal_line, formatter.format(goal))
        findViewById<TextView>(R.id.textViewWeekTotal).text = formatter.format(week.total)
        findViewById<TextView>(R.id.textViewWeekAverage).text =
            formatter.format(week.averagePerElapsedDay)
        findViewById<TextView>(R.id.textViewWeekAchieved).text =
            getString(R.string.week_goal_days_value, week.achievedDays, week.elapsedDays)
        findViewById<TextView>(R.id.textViewWeekHint).setText(when {
            week.todayIndex >= 0 -> R.string.week_average_current_hint
            week.elapsedDays == 7 -> R.string.week_average_past_hint
            else -> R.string.week_average_future_hint
        })
        mChart.setWeek(week)

        updateHistoryDetails(goal)
    }

    /** Selected date navigates together with Week. Calendar never changes database rows. */
    private fun selectHistoryDay(date: Long) {
        val today = DailyHistorySummary.startOfDay(System.currentTimeMillis())
        val min = mDailyHistory?.keys?.minOrNull() ?: today
        val day = DailyHistorySummary.startOfDay(date)
        if (day < min || day > today) return
        mSelectedMonth.timeInMillis = day
        mCalendarNeedsRefresh = true
        updateChart()
        updateCards()
    }

    private fun moveHistoryDay(amount: Int) {
        val next = Calendar.getInstance().apply {
            timeInMillis = mSelectedMonth.timeInMillis
            add(Calendar.DAY_OF_MONTH, amount)
        }
        selectHistoryDay(next.timeInMillis)
    }

    private fun updateHistoryDetails(goal: Int) {
        val today = DailyHistorySummary.startOfDay(System.currentTimeMillis())
        val history = mDailyHistory ?: DailyHistorySummary.aggregate(getCachedHistory()).also {
            mDailyHistory = it
            mCalendarNeedsRefresh = true
        }
        val earliest = history.keys.minOrNull() ?: today
        val info = DailyHistorySummary.describe(
            selected = mSelectedMonth.timeInMillis,
            daily = history,
            todaySteps = mCurrentSteps.coerceAtLeast(0),
            goal = goal
        )
        if (mCalendarNeedsRefresh) {
            mHistoryCalendar.configure(info.dayStart, earliest, today, history,
                mCurrentSteps.coerceAtLeast(0), goal)
            mCalendarNeedsRefresh = false
        } else {
            mHistoryCalendar.updateTodaySteps(mCurrentSteps.coerceAtLeast(0))
        }
        val dayTitle = SimpleDateFormat("EEEE d MMMM yyyy", Locale.getDefault())
            .format(Date(info.dayStart)).replaceFirstChar { it.titlecase(Locale.getDefault()) }
        findViewById<TextView>(R.id.textViewHistoryDate).text = dayTitle
        val number = NumberFormat.getIntegerInstance()
        findViewById<TextView>(R.id.textViewHistorySteps).text =
            getString(R.string.history_steps_value, number.format(info.steps))
        findViewById<TextView>(R.id.textViewHistoryPercent).text =
            getString(R.string.history_goal_percent, info.percent)
        mHistoryGoalBar.setFraction((info.steps.toDouble() / goal).coerceIn(0.0, 1.0).toFloat())
        mHistoryGoalBar.contentDescription =
            getString(R.string.history_goal_percent, info.percent)
        findViewById<TextView>(R.id.textViewHistoryDistance).text =
            getString(R.string.history_distance_value, Util.stepsToMeters(info.steps))
        val label = when {
            !info.hasData -> getString(R.string.history_no_data)
            info.goalReached -> getString(R.string.history_goal_met)
            info.isToday -> getString(R.string.history_in_progress)
            else -> getString(R.string.history_goal_missed)
        }
        findViewById<TextView>(R.id.textViewHistoryStatus).text = if (info.isToday)
            getString(R.string.history_today_status, label) else label
        findViewById<TextView>(R.id.historyPreviousDay).apply {
            isEnabled = info.dayStart > earliest
            alpha = if (isEnabled) 1f else 0.3f
        }
        findViewById<TextView>(R.id.historyNextDay).apply {
            isEnabled = info.dayStart < today
            alpha = if (isEnabled) 1f else 0.3f
        }
    }

    private fun updateCards() {
        val cal = Util.calendar
        cal.set(Calendar.DAY_OF_MONTH, 1)

        val startOfMonth = Calendar.getInstance()
        startOfMonth.timeInMillis = mSelectedMonth.timeInMillis
        startOfMonth.set(Calendar.DAY_OF_MONTH, 1)
        val currentMonth = cal == startOfMonth

        val endOfMonth = Calendar.getInstance()
        endOfMonth.timeInMillis = startOfMonth.timeInMillis
        endOfMonth.add(Calendar.MONTH, 1)

        val database = Database.getInstance(this)
        val stepsThisMonth = database.getSumSteps(startOfMonth.timeInMillis, endOfMonth.timeInMillis)
        val averageSteps = database.avgSteps(startOfMonth.timeInMillis, endOfMonth.timeInMillis)
        // The historic total and today's live counter remain separate in the database.
        val overallSteps = database.getSumSteps(database.firstEntry, database.lastEntry)
        mGlobalStatsCard.updateHistory(stepsThisMonth, averageSteps, overallSteps, currentMonth)
        mGlobalStatsCard.updateToday(mCurrentSteps)
    }

    companion object {

        const val RECEIVER_TAG = "RECEIVER_TAG"
        private const val HISTORY_EXPANDED_KEY = "history_expanded"
        private const val HISTORY_SELECTED_DAY_KEY = "history_selected_day"
    }
    private fun updateStreakCards() {
        val preferences = PreferenceManager.getDefaultSharedPreferences(this)
        val goal = preferences.getInt("daily_step_goal", 10000)

        val history = getCachedHistory()
        val result = StepStreakCalculator.calculate(
            history = history,
            todaySteps = mCurrentSteps,
            goal = goal
        )

        mStreakDashboardCard.updateStats(
            result.current, result.best, goal, result.bestStart, result.bestEnd
        )
        mRollingAveragesCard.updateStats(RollingStepAverages.calculate(history))
    }

    /** Update the live progress; the icon fills from 0 to 100%, the text may exceed 100%. */
    private fun updateDailyGoalProgress(steps: Int) {
        val goal = PreferenceManager.getDefaultSharedPreferences(this)
            .getInt("daily_step_goal", 10000)
        val state = DailyGoalProgress.calculate(steps.coerceAtLeast(0), goal.coerceAtLeast(1))
        // Re-read the preference on each update and after returning from Settings.
        val iconStyle = PreferenceManager.getDefaultSharedPreferences(this)
            .getString("progress_icon_style", "05") ?: "05"
        mGoalProgress.setIconStyle(iconStyle)
        mGoalProgress.setProgress(state.fillFraction, state.percent)
        val formatter = NumberFormat.getIntegerInstance()
        mTextViewGoalProgress.text = getString(
            R.string.daily_goal_progress,
            formatter.format(steps), formatter.format(goal), state.percent
        )
        mGoalProgress.contentDescription = getString(
            R.string.goal_progress_description, state.percent
        )
    }

    /** Keep historical rows in memory between counter updates; refresh daily or after Settings. */
    private fun getCachedHistory(): List<Pair<Long, Int>> {
        val today = Util.calendar.timeInMillis
        if (mCachedHistory == null || mCachedHistoryDay != today) {
            val database = Database.getInstance(this)
            val first = database.firstEntry
            val last = database.lastEntry
            mCachedHistory = if (first != 0L) {
                database.getEntries(first, last).map { it.timestamp to it.steps }
            } else {
                emptyList()
            }
            mCachedHistoryDay = today
            mDailyHistory = null
            mCalendarNeedsRefresh = true
        }
        return mCachedHistory ?: emptyList()
    }
    override fun onResume() {
        super.onResume()
        // An import may have changed history while the Settings screen was open.
        mCachedHistory = null
        mDailyHistory = null
        mCalendarNeedsRefresh = true
        // Refresh all card backgrounds without recalculating statistics.
        val dashboardStyle = PreferenceManager.getDefaultSharedPreferences(this)
            .getString("dashboard_card_style", "standard") ?: "standard"
        mAdapter.setDashboardCardStyle(dashboardStyle)
        mStreakAdapter.setDashboardCardStyle(dashboardStyle)
        DashboardGradientStyler.applyDashboard(
            findViewById(android.R.id.content), dashboardStyle == "gradient"
        )
        // An import from Settings may have changed the week.
        if (::mChart.isInitialized) updateChart()

        if (::mGoalProgress.isInitialized) updateDailyGoalProgress(mCurrentSteps)
        if (::mStreakDashboardCard.isInitialized) {
            val selectedStyle = PreferenceManager.getDefaultSharedPreferences(this)
                .getString("streak_card_style", "current") ?: "current"
            if (mStreakDashboardCard.displayStyle != selectedStyle) {
                mStreakDashboardCard.displayStyle = selectedStyle
                mStreakDashboardCard.detailsExpanded = false
                // View type changes: RecyclerView will inflate the selected compact layout.
                mStreakAdapter.notifyDataSetChanged()
            }
            updateStreakCards()
        }
    }
}
