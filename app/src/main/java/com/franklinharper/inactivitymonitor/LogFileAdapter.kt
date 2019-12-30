package com.franklinharper.inactivitymonitor

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.io.File

class LogFileAdapter() : RecyclerView.Adapter<LogFileAdapter.ViewHolder>() {

  // TODO read file lazily based on the current position
  private var data: List<String> = emptyList()

  class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
    var textView: TextView = view.findViewById(android.R.id.text1) as TextView
  }

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
    return ViewHolder(
      LayoutInflater.from(parent.context).inflate(
        android.R.layout.simple_list_item_1,
        parent,
        false
      )
    )
  }

  override fun onBindViewHolder(holder: ViewHolder, position: Int) {
    val text = data[position]
      .replaceFirst("/","\n  ")
      .replaceLast(":", "\n  ")
    holder.textView.text = text
  }

  override fun getItemCount(): Int {
    return data.size
  }

  fun update(file: File) {
    data = file.readLines().reversed()
    notifyDataSetChanged()
  }
}

private fun String.replaceLast(old: String, new: String): CharSequence? {
  val index = lastIndexOf(old)
  return if (index < 0) this else this.replaceRange(index, index + 1, new)
}
