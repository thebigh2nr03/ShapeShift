package com.usc.shapeshift

import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.view.children
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.children
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.kizitonwose.calendar.core.*
import com.kizitonwose.calendar.view.MonthDayBinder
import com.kizitonwose.calendar.view.MonthHeaderFooterBinder
import com.kizitonwose.calendar.view.ViewContainer
import com.kizitonwose.calendar.core.CalendarDay
import com.kizitonwose.calendar.core.CalendarMonth
import com.kizitonwose.calendar.core.DayPosition
import com.kizitonwose.calendar.core.daysOfWeek
import com.usc.shapeshift.databinding.ActivityCalendarBinding
import com.usc.shapeshift.databinding.CalendarDayLayoutBinding
import com.usc.shapeshift.databinding.CalendarEventItemViewBinding
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.*


class CalendarActivity : AppCompatActivity() {

    private val eventsAdapter = EntryAdapter {
        AlertDialog.Builder(this@CalendarActivity)
            .setMessage("Delete this entry?")
            .setPositiveButton("Delete") { _, _ ->
                deleteEvent(it)
            }
            .setNegativeButton("Close", null)
            .show()
    }

    private val inputDialog by lazy {
        val editEvent = EditText(this@CalendarActivity)
        /*val editProgressNote = EditText(this@CalendarActivity)
        val editWeight = EditText(this@CalendarActivity)
        val editWorkout = EditText(this@CalendarActivity)
        val editMeal = EditText(this@CalendarActivity)*/
        val layout = FrameLayout(this@CalendarActivity).apply {
            // Setting the padding on the EditText only pads the input area
            // not the entire EditText so we wrap it in a FrameLayout.
            val padding = dpToPx(20, this@CalendarActivity)
            setPadding(padding, padding, padding, padding)
            addView(editEvent, FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ))
            /*addView(editProgressNote, FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ))
            addView(editWeight, FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ))
            addView(editWorkout, FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ))
            addView(editMeal, FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ))*/
        }
        AlertDialog.Builder(this@CalendarActivity)
            .setTitle("Enter Entry Title")
            .setView(layout)
            .setPositiveButton("Save") { _, _ ->
                saveEvent(editEvent.text.toString()/*, editProgressNote.text.toString(), editWeight.text.toString(), editWorkout.text.toString(), editMeal.text.toString()*/)
                // Prepare EditText for reuse.
                editEvent.setText("")
            }
            .setNegativeButton("Close", null)
            .create()
            .apply {
                setOnShowListener {
                    // Show the keyboard
                    editEvent.requestFocus()
                    context.inputMethodManager
                        .toggleSoftInput(InputMethodManager.SHOW_FORCED, 0)
                }
                setOnDismissListener {
                    // Hide the keyboard
                    context.inputMethodManager
                        .toggleSoftInput(InputMethodManager.HIDE_IMPLICIT_ONLY, 0)
                }
            }

    }

    private lateinit var binding : ActivityCalendarBinding
    private lateinit var firebaseAuth : FirebaseAuth
    private lateinit var database : DatabaseReference

    private var selectedDate : LocalDate? = null
    private val today = LocalDate.now()

    private val titleMonthFormatter = DateTimeFormatter.ofPattern("MMMM")
    private val titleYearFormatter = DateTimeFormatter.ofPattern("yyyy")
    private val titleFormatter = DateTimeFormatter.ofPattern("MMMM yyyy")
    private val selectionFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy")
    private val events = mutableMapOf<Long, List<CalendarEntry>>()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        firebaseAuth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().getReference("Users")

        binding = ActivityCalendarBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.calendarRv.apply {
            layoutManager = LinearLayoutManager(this@CalendarActivity, RecyclerView.VERTICAL, false)
            adapter = eventsAdapter
            addItemDecoration(DividerItemDecoration(this@CalendarActivity, RecyclerView.VERTICAL))
        }


        class DayViewContainer(view: View) : ViewContainer(view) {
            val textView = CalendarDayLayoutBinding.bind(view).calendarDayText
            lateinit var day: CalendarDay

            init {
                view.setOnClickListener {
                    // Check the day position as we do not want to select in or out dates.
                    selectDate(day.date)
                }
            }
        }

        class MonthViewContainer(view: View) : ViewContainer(view) {
            // Alternatively, you can add an ID to the container layout and use findViewById()
            val titlesContianer = view as ViewGroup
        }

        binding.calendarView.monthScrollListener = {
            binding.Month.text = titleMonthFormatter.format(it.yearMonth)
            binding.Year.text = titleYearFormatter.format(it.yearMonth)
            // Select the first day of the visible month.
            selectDate(it.yearMonth.atDay(1))
        }

        binding.calendarView.dayBinder = object : MonthDayBinder<DayViewContainer> {
            // Called only when a new container is needed.
            override fun create(view: View) = DayViewContainer(view)

            // Called every time we need to reuse a container.
            override fun bind(container: DayViewContainer, data: CalendarDay) {
                container.textView.text = data.date.dayOfMonth.toString()
            }
        }

        val currentMonth = YearMonth.now()
        val startMonth = currentMonth.minusMonths(100)  // Adjust as needed
        val endMonth = currentMonth.plusMonths(100)  // Adjust as needed
        val daysOfWeek = daysOfWeek()
        binding.calendarView.setup(startMonth, endMonth, daysOfWeek.first())
        binding.calendarView.scrollToMonth(currentMonth)
        binding.Month.text = currentMonth.toString();

        binding.calendarView.monthHeaderBinder = object : MonthHeaderFooterBinder<MonthViewContainer> {
            override fun create(view: View) = MonthViewContainer(view)
            override fun bind(container: MonthViewContainer, data: CalendarMonth) {

                if (container.titlesContianer.tag == null) {
                    container.titlesContianer.tag = data.yearMonth
                    container.titlesContianer.children.map { it as TextView }
                        .forEachIndexed { index, textView ->
                            val dayOfWeek = daysOfWeek[index]
                            val title = dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault())
                            textView.text = title
                        }
                }
            }
        }

        binding.calendarView.dayBinder = object : MonthDayBinder<DayViewContainer> {
            override fun create(view: View) = DayViewContainer(view)
            override fun bind(container: DayViewContainer, data: CalendarDay) {
                // Set the calendar day for this container.
                container.day = data
                val day = data
                val textView = container.textView
                // Set the date text
                textView.text = data.date.dayOfMonth.toString()
                if (data.position == DayPosition.MonthDate) {
                    if (day.date == selectedDate) {
                        // If this is the selected date, show a round background and change the text color.
                        textView.setTextColor(getColor(R.color.white))
                        textView.setBackgroundResource(R.drawable.calendar_circle)
                        textView.typeface = ResourcesCompat.getFont(applicationContext, R.font.outfit_bold)
                    } else {
                        // If this is NOT the selected date, remove the background and reset the text color.
                        textView.setTextColor(getColor(R.color.black))
                        textView.background = null
                        textView.typeface = ResourcesCompat.getFont(applicationContext, R.font.outfit_regular)
                    }
                } else {
                    textView.setTextColor(getColor(R.color.blackFaded))
                    textView.typeface = ResourcesCompat.getFont(applicationContext, R.font.outfit_regular)
                    textView.background = null
                }
            }
        }
        binding.AddEntryButton.setOnClickListener { inputDialog.show() }
    }

    private fun selectDate(date: LocalDate) {
        if (selectedDate != date) {
            val oldDate = selectedDate
            selectedDate = date
            oldDate?.let { binding.calendarView.notifyDateChanged(it) }
            binding.calendarView.notifyDateChanged(date)
            updateAdapterForDate(date)
        }
    }

    private fun saveEvent(event: String, /*progressNote: String, weight: String, workout: String, meal: String*/) {
        if (event.isBlank()) {
            Toast.makeText(this@CalendarActivity, "Event cannot be blank", Toast.LENGTH_LONG)
                .show()
        } else {
            selectedDate?.let {
                events[it.toEpochDay()] =
                    events[it.toEpochDay()].orEmpty().plus(CalendarEntry(UUID.randomUUID().toString(), event,/*, progressNote, weight, workout, meal,*/ it.toEpochDay()))
                //firebaseAuth.currentUser?.let{ it1 -> database.child(it1.uid).child("events").setValue(events)}
                updateAdapterForDate(it)
            }
        }
    }

    private fun deleteEvent(event: CalendarEntry) {
        val date = event.date
        events[date] = events[date].orEmpty().minus(event)
        updateAdapterForDate(LocalDate.ofEpochDay(date))
    }

    private fun updateAdapterForDate(date: LocalDate) {
        eventsAdapter.apply {
            events.clear()
            events.addAll(this@CalendarActivity.events[date.toEpochDay()].orEmpty())
            notifyDataSetChanged()
        }
        binding.dateText.text = selectionFormatter.format(date)
    }

    fun saveEvents(){
        firebaseAuth.currentUser?.let{ it1 -> database.child(it1.uid).child("events").setValue(events)}
    }
}

fun dpToPx(dp: Int, context: Context): Int =
    TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,
        dp.toFloat(),
        context.resources.displayMetrics,
    ).toInt()

internal val Context.layoutInflater: LayoutInflater
    get() = LayoutInflater.from(this)

internal val Context.inputMethodManager
    get() = this.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager



data class CalendarEntry(
    val id: String,
    val event: String,/*
    val progressNote: String,
    val weight: String,
    val workout: String,
    val meal: String,*/
    val date: Long)

class EntryAdapter(val onClick: (CalendarEntry) -> Unit) :
    RecyclerView.Adapter<EntryAdapter.EventsViewHolder>() {

    val events = mutableListOf<CalendarEntry>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventsViewHolder {
        return EventsViewHolder(
            CalendarEventItemViewBinding.inflate(parent.context.layoutInflater, parent, false),
        )
    }

    override fun onBindViewHolder(viewHolder: EventsViewHolder, position: Int) {
        viewHolder.bind(events[position])
    }

    override fun getItemCount(): Int = events.size

    inner class EventsViewHolder(private val binding: CalendarEventItemViewBinding) :
        RecyclerView.ViewHolder(binding.root) {

        init {
            itemView.setOnClickListener {
                onClick(events[bindingAdapterPosition])
            }
        }

        fun bind(event: CalendarEntry) {
            binding.eventField.text = event.event
        }
    }
}
