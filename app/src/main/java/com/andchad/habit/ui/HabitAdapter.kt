package com.andchad.habit.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.andchad.habit.R
import com.andchad.habit.data.model.Habit

// Note: This is a traditional RecyclerView adapter, but the app uses Jetpack Compose
// This file is created for reference if you want to use RecyclerView instead

class HabitAdapter(
    private val onItemClick: (Habit) -> Unit,
    private val onCheckboxClick: (Habit, Boolean) -> Unit
) : ListAdapter<Habit, HabitAdapter.HabitViewHolder>(HabitDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HabitViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_habit, parent, false)
        return HabitViewHolder(view)
    }

    override fun onBindViewHolder(holder: HabitViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class HabitViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val nameTextView: TextView = itemView.findViewById(R.id.habitNameTextView)
        private val timeTextView: TextView = itemView.findViewById(R.id.habitTimeTextView)
        private val checkbox: CheckBox = itemView.findViewById(R.id.habitCheckbox)

        init {
            itemView.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onItemClick(getItem(position))
                }
            }

            checkbox.setOnCheckedChangeListener { _, isChecked ->
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onCheckboxClick(getItem(position), isChecked)
                }
            }
        }

        fun bind(habit: Habit) {
            nameTextView.text = habit.name
            timeTextView.text = "Reminder: ${habit.reminderTime}"
            checkbox.isChecked = habit.isCompleted
        }
    }

    class HabitDiffCallback : DiffUtil.ItemCallback<Habit>() {
        override fun areItemsTheSame(oldItem: Habit, newItem: Habit): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Habit, newItem: Habit): Boolean {
            return oldItem == newItem
        }
    }
}