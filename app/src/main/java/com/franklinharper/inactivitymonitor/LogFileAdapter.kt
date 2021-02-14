package com.franklinharper.inactivitymonitor

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.franklinharper.inactivitymonitor.databinding.LogItemBinding

class LogFileAdapter : ListAdapter<String, LogItemViewHolder>(DIFF_CALLBACK) {

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LogItemViewHolder {
    val binding = LogItemBinding.inflate(
      LayoutInflater.from(parent.context),
      parent,
      false
    )
    return LogItemViewHolder(binding)
  }

  override fun onBindViewHolder(holder: LogItemViewHolder, position: Int) {
    holder.bind(getItem(position))
  }

  companion object {

    val DIFF_CALLBACK = object : DiffUtil.ItemCallback<String>() {

      override fun areItemsTheSame(oldItem: String, newItem: String): Boolean {
        return oldItem == newItem
      }

      override fun areContentsTheSame(oldItem: String, newItem: String): Boolean {
        return oldItem == newItem
      }
    }
  }

}

class LogItemViewHolder(private val binding: LogItemBinding) : RecyclerView.ViewHolder(binding.root) {

  fun bind(item: String) {
    binding.text1.text = item
  }
}

