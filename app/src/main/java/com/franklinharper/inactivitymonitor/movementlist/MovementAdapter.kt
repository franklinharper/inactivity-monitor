package com.franklinharper.inactivitymonitor.movementlist

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.franklinharper.inactivitymonitor.databinding.MovementItemBinding
import timber.log.Timber

class MovementAdapter : ListAdapter<String, MovementItemViewHolder>(DIFF_CALLBACK) {

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MovementItemViewHolder {
    Timber.d("Create VH. viewType: $viewType")
    val binding = MovementItemBinding.inflate(
      LayoutInflater.from(parent.context),
      parent,
      false
    )
    return MovementItemViewHolder(binding)
  }

  override fun onBindViewHolder(holder: MovementItemViewHolder, position: Int) {
    Timber.d("onBindViewHolder position: $position")
    holder.bind(getItem(position))
  }

  companion object {

    val DIFF_CALLBACK = object : DiffUtil.ItemCallback<String>() {

      override fun areItemsTheSame(oldItem: String, newItem: String): Boolean {
        val same = oldItem == newItem
        Timber.d("areItemsTheSame same:$same newItem:$newItem")
        return same
      }

      override fun areContentsTheSame(oldItem: String, newItem: String): Boolean {
        val same = oldItem == newItem
        Timber.d("areContentsTheSameK same:$same newItem:$newItem")
        return oldItem == newItem
      }
    }
  }

}

class MovementItemViewHolder(private val binding: MovementItemBinding) : RecyclerView.ViewHolder(binding.root) {

  fun bind(item: String) {
    binding.movementItem.text = item
  }
}

