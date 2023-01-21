package com.franklinharper.inactivitymonitor.movementlist

import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.franklinharper.inactivitymonitor.MovementType
import com.franklinharper.inactivitymonitor.R
import com.franklinharper.inactivitymonitor.TimeFormatters
import com.franklinharper.inactivitymonitor.UserMovement
import com.franklinharper.inactivitymonitor.databinding.MovementItemBinding
import timber.log.Timber

class MovementAdapter : ListAdapter<UserMovement, MovementItemViewHolder>(DIFF_CALLBACK) {

  private val green = Color.rgb(0x55, 0x8B, 0x2F)
  private val red = Color.rgb(0xC6, 0x28, 0x28)

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MovementItemViewHolder {
    val binding = MovementItemBinding.inflate(
      LayoutInflater.from(parent.context),
      parent,
      false
    )
    val walkingDrawable = parent.context.getDrawable(R.drawable.walking)!!.also {
      it.colorFilter = PorterDuffColorFilter(green, PorterDuff.Mode.SRC_ATOP)
    }
    val sittingDrawable = parent.context.getDrawable(R.drawable.sitting)!!.also {
      it.colorFilter = PorterDuffColorFilter(red, PorterDuff.Mode.SRC_ATOP)
    }
    val icons = mapOf(
      MovementType.WALKING_START to walkingDrawable,
      MovementType.STILL_START to sittingDrawable,
      // This is the DEFAULT icon
      MovementType.UNKNOWN_START to parent.context.getDrawable(R.drawable.ic_snooze_black_24dp),
    )
    return MovementItemViewHolder(binding, icons)
  }

  override fun onBindViewHolder(holder: MovementItemViewHolder, position: Int) {
    holder.bind(getItem(position))
  }

  companion object {

    val DIFF_CALLBACK = object : DiffUtil.ItemCallback<UserMovement>() {

      override fun areItemsTheSame(oldItem: UserMovement, newItem: UserMovement): Boolean {
        val same = oldItem == newItem
        Timber.d("areItemsTheSame same:$same newItem:$newItem")
        return same
      }

      override fun areContentsTheSame(oldItem: UserMovement, newItem: UserMovement): Boolean {
        val same = oldItem == newItem
        Timber.d("areContentsTheSameK same:$same newItem:$newItem")
        return oldItem == newItem
      }
    }
  }

}

class MovementItemViewHolder(
  private val binding: MovementItemBinding,
  private val icons: Map<MovementType, Drawable?>
) : RecyclerView.ViewHolder(binding.root) {

  fun bind(movement: UserMovement) {
    val startTime = TimeFormatters.time.format(movement.start.toZonedDateTime())
    val minutes = "%.1f min".format(movement.durationSecs / 60.0)
    val icon = icons.getOrDefault(
      key = movement.type,
      defaultValue = icons[MovementType.UNKNOWN_START]
    )
    binding.startTime.text = startTime
    binding.movementIcon.setImageDrawable(icon)
    binding.duration.text = minutes
  }
}

